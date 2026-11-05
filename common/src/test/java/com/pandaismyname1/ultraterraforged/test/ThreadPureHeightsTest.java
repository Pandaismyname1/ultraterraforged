package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellField;
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.CellSampler;

/**
 * The terrain a world's generator gives for a column (getBaseHeight, getFirstFreeHeight, getBaseColumn: what places
 * structure starts) is the terrain its chunk is generated with, on whatever thread asks and whenever it asks.
 *
 * Each of those builds a NoiseChunk one column wide; MixinNoiseChunk gives it {@link CellSampler#chunkCells}, and
 * UltraTerraForged's density functions read {@link CellSampler#cell} at the column and at the corners of its 4x4
 * cell (where interpolated functions sample). A chunk being generated reads its own cells from its tile, and the
 * corners on its far edges (in the next chunk, maybe the next tile) the same way. Here those reads are made on the test
 * thread (the server thread) and on several other threads at once, each in its own order, in a world whose tiles
 * aren't made yet and in one whose tiles are, and every one must be the cell of the tile, as the generated chunk has
 * it. Mods that place structure starts ahead of the players on their own threads (Panda's Structure Optimizer) found
 * starts 1-2 blocks off when they weren't.
 */
public class ThreadPureHeightsTest {
	private static final int SEED = 20261105;
	// as MixinRandomState makes a world's context, with the default performance config
	private static final int TILE_SIZE = 3, BATCH_COUNT = 6;
	private static final int MIN_Y = -64, HEIGHT = 384;
	private static final int THREADS = 4;
	private static final CellField[] FIELDS = CellField.values();

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void columnsHaveTheirChunksTerrainOnEveryThread() throws Exception {
		List<Probe> probes = probes();

		// what the chunks are generated with: the cells of their tiles
		GeneratorContext reference = context();
		float[][] expected = new float[probes.size()][];
		for (int i = 0; i < probes.size(); i++) {
			expected[i] = tileCells(reference, probes.get(i));
		}

		List<String> failures = new ArrayList<>();
		// no tile made yet, as for structure starts worked out ahead of the players
		check("tiles not made yet", context(), probes, expected, failures);
		// every tile made first, as when the chunks around were generated already
		GeneratorContext generated = context();
		for (Probe probe : probes) {
			for (int[] pos : probe.reads()) {
				generated.cache.provideChunk(pos[0] >> 4, pos[1] >> 4);
			}
		}
		check("tiles made first", generated, probes, expected, failures);

		Map<String, Integer> counts = new TreeMap<>();
		for (String failure : failures) {
			counts.merge(failure.substring(0, failure.indexOf(':')), 1, Integer::sum);
		}
		assertTrue(failures.isEmpty(), failures.size() + " of " + probes.size() + " probes on " + (THREADS + 1) + " threads in 2 worlds aren't the generated terrain, " + counts + ":\n" + String.join("\n", failures.subList(0, Math.min(40, failures.size()))));
	}

	// every probe, on the test thread and on THREADS others at the same time, each in its own order
	private static void check(String phase, GeneratorContext context, List<Probe> probes, float[][] expected, List<String> failures) throws Exception {
		CyclicBarrier start = new CyclicBarrier(THREADS + 1);
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		try {
			List<Future<float[][]>> others = new ArrayList<>();
			for (int t = 0; t < THREADS; t++) {
				long orderSeed = t + 1;
				others.add(pool.submit(() -> {
					start.await();
					return read(context, probes, orderSeed);
				}));
			}
			start.await();
			List<float[][]> results = new ArrayList<>();
			results.add(read(context, probes, 0L));
			for (Future<float[][]> other : others) {
				results.add(other.get());
			}
			for (int t = 0; t < results.size(); t++) {
				String thread = t == 0 ? "server thread" : "thread " + t;
				float[][] result = results.get(t);
				for (int i = 0; i < probes.size(); i++) {
					if (!Arrays.equals(expected[i], result[i])) {
						failures.add(phase + ", " + thread + ": " + probes.get(i) + " " + describe(context, expected[i], result[i]));
					}
				}
			}
		} finally {
			pool.shutdownNow();
		}
	}

	private static float[][] read(GeneratorContext context, List<Probe> probes, long orderSeed) {
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < probes.size(); i++) {
			order.add(i);
		}
		if (orderSeed != 0L) {
			Collections.shuffle(order, new Random(orderSeed));
		}
		float[][] result = new float[probes.size()][];
		for (int i : order) {
			Probe probe = probes.get(i);
			CellSampler.ChunkCells cells = CellSampler.chunkCells(context, probe.volume());
			List<int[]> reads = probe.reads();
			float[] values = new float[(reads.size() + 1) * FIELDS.length];
			for (int r = 0; r < reads.size(); r++) {
				int[] pos = reads.get(r);
				// read at once: the cell may be one the thread reuses for its next lookup
				Cell cell = CellSampler.cell(context, cells, pos[0], pos[1]);
				for (int f = 0; f < FIELDS.length; f++) {
					values[r * FIELDS.length + f] = FIELDS[f].read(cell);
				}
			}
			// and what a structure rule (CellTest, which keeps villages off rivers) reads at the column
			int[] pos = reads.get(0);
			Cell cell = new Cell();
			context.lookup.applyAsGenerated(cell, pos[0], pos[1]);
			for (int f = 0; f < FIELDS.length; f++) {
				values[reads.size() * FIELDS.length + f] = FIELDS[f].read(cell);
			}
			result[i] = values;
		}
		return result;
	}

	private static float[] tileCells(GeneratorContext context, Probe probe) {
		List<int[]> reads = new ArrayList<>(probe.reads());
		reads.add(reads.get(0));
		float[] values = new float[reads.size() * FIELDS.length];
		for (int r = 0; r < reads.size(); r++) {
			int[] pos = reads.get(r);
			Cell cell = context.cache.provideChunk(pos[0] >> 4, pos[1] >> 4).getCell(pos[0], pos[1]);
			for (int f = 0; f < FIELDS.length; f++) {
				values[r * FIELDS.length + f] = FIELDS[f].read(cell);
			}
		}
		return values;
	}

	private static String describe(GeneratorContext context, float[] expected, float[] actual) {
		int height = Arrays.asList(FIELDS).indexOf(CellField.HEIGHT);
		StringBuilder builder = new StringBuilder();
		for (int r = 0; r < expected.length / FIELDS.length; r++) {
			int want = context.levels.scale(expected[r * FIELDS.length + height]), got = context.levels.scale(actual[r * FIELDS.length + height]);
			if (want != got) {
				builder.append(" read ").append(r).append(": height ").append(want).append(" -> ").append(got);
			}
		}
		return builder.length() == 0 ? "(less than a block, or fields other than the height)" : builder.toString();
	}

	private static GeneratorContext context() {
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		return GeneratorContext.makeCached(preset, SEED, TILE_SIZE, BATCH_COUNT, false);
	}

	// columns inside chunks, on chunk edges and on tile edges (a tile is 8 chunks), on and off the 4-block grid
	private static List<Probe> probes() {
		List<Probe> probes = new ArrayList<>();
		int tile = 16 << TILE_SIZE;
		int[] offsets = { -1, 0, 3, 13, 15, 16, 66, 127 };
		int[][] tiles = { { 0, 0 }, { 3, -2 }, { -5, 7 } };
		for (int[] t : tiles) {
			for (int dx : offsets) {
				for (int dz : new int[] { 2, 15, 64 }) {
					probes.add(Probe.column(t[0] * tile + dx, t[1] * tile + dz));
				}
			}
			// chunks whose far edges are in the next tile
			probes.add(Probe.chunk(t[0] * (tile >> 4) + 7, t[1] * (tile >> 4) + 7));
			probes.add(Probe.chunk(t[0] * (tile >> 4) + 7, t[1] * (tile >> 4) + 3));
			probes.add(Probe.chunk(t[0] * (tile >> 4) + 2, t[1] * (tile >> 4) + 7));
		}
		Random random = new Random(SEED);
		for (int i = 0; i < 24; i++) {
			probes.add(Probe.column(random.nextInt(4000) - 2000, random.nextInt(4000) - 2000));
		}
		return probes;
	}

	private record Probe(String name, DensityVolume volume, List<int[]> reads) {

		// what getBaseHeight's NoiseChunk reads at a column: the column, and the corners of its cell
		static Probe column(int x, int z) {
			List<int[]> reads = new ArrayList<>();
			reads.add(new int[] { x, z });
			int x0 = Mth.floorDiv(x, 4) * 4, z0 = Mth.floorDiv(z, 4) * 4;
			for (int cz = 0; cz <= 4; cz += 4) {
				for (int cx = 0; cx <= 4; cx += 4) {
					reads.add(new int[] { x0 + cx, z0 + cz });
				}
			}
			return new Probe("column " + x + " " + z, new DensityVolume(1, HEIGHT, 1, x, MIN_Y, z), reads);
		}

		// what a chunk being generated reads outside itself: the corners on its far edges
		static Probe chunk(int chunkX, int chunkZ) {
			int minX = chunkX << 4, minZ = chunkZ << 4;
			List<int[]> reads = new ArrayList<>();
			for (int i = 0; i <= 16; i += 4) {
				reads.add(new int[] { minX + 16, minZ + i });
				reads.add(new int[] { minX + i, minZ + 16 });
			}
			reads.add(new int[] { minX, minZ });
			reads.add(new int[] { minX + 7, minZ + 9 });
			return new Probe("chunk " + chunkX + " " + chunkZ, new DensityVolume(16, HEIGHT, 16, minX, MIN_Y, minZ), reads);
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	@Test
	void probesReadSomething() {
		// a guard on the test itself: a preset whose cells read nothing would pass the test above without a word
		GeneratorContext context = context();
		Probe probe = Probe.column(13, 2);
		float[] cells = tileCells(context, probe);
		assertEquals((probe.reads().size() + 1) * FIELDS.length, cells.length);
		assertTrue(cells[Arrays.asList(FIELDS).indexOf(CellField.HEIGHT)] > 0.0F, "the column should have ground");
	}
}
