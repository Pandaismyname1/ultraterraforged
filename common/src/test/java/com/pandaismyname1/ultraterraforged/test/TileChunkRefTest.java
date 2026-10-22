package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileChunkRef;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileFactory;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileGenerator;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

/**
 * A chunk being generated reads its tile through a TileChunkRef. Issue #4: thousands of chunks waiting to be
 * generated each held on to their whole tile, gigabytes of cells long after the cache had let the tiles go. The ref
 * mustn't keep the tile alive, and must read the very same cells once it has taken the tile from the cache again.
 */
public class TileChunkRefTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void letsTheTileGoAndReadsTheSameCellsAfter() throws InterruptedException {
		GeneratorContext context = GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(), PresetRenderer.SEED, 3, 1, false);
		Tiles tiles = new Tiles(context.generator);
		int chunkX = (3 << 3) + 5;
		int chunkZ = (-2 << 3) + 2;

		TileChunkRef ref = new TileChunkRef(tiles, chunkX, chunkZ);
		Tile.Chunk first = ref.get();
		Cell[] before = copy(first);
		assertSame(first, ref.get(), "the tile was taken again while still in the cache");
		assertEquals(1, tiles.loads);

		WeakReference<Tile> tile = new WeakReference<>(tiles.tiles.get(PosUtil.pack(3, -2)));
		first = null;
		tiles.tiles.clear();
		for (int i = 0; i < 50 && tile.get() != null; i++) {
			System.gc();
			Thread.sleep(20);
		}
		assertNull(tile.get(), "the ref kept the tile alive after the cache let it go");

		Tile.Chunk again = ref.get();
		assertEquals(2, tiles.loads);
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				Cell expected = before[dz << 4 | dx];
				Cell cell = again.getCell(dx, dz);
				String at = " at " + dx + " " + dz;
				assertEquals(expected.height, cell.height, "height" + at);
				assertEquals(expected.gradient, cell.gradient, "gradient" + at);
				assertEquals(expected.sediment2, cell.sediment2, "sediment" + at);
				assertEquals(expected.localErosion2, cell.localErosion2, "erosion" + at);
				assertEquals(expected.continentNoise, cell.continentNoise, "continentNoise" + at);
				assertEquals(expected.riverDistance, cell.riverDistance, "riverDistance" + at);
				assertEquals(expected.waterLevel, cell.waterLevel, "waterLevel" + at);
				assertEquals(expected.temperature, cell.temperature, "temperature" + at);
				assertEquals(expected.moisture, cell.moisture, "moisture" + at);
				assertSame(expected.terrain, cell.terrain, "terrain" + at);
				assertSame(expected.biomeType, cell.biomeType, "biomeType" + at);
			}
		}
	}

	private static Cell[] copy(Tile.Chunk chunk) {
		Cell[] cells = new Cell[256];
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				Cell cell = new Cell();
				cell.copyFrom(chunk.getCell(dx, dz));
				cells[dz << 4 | dx] = cell;
			}
		}
		return cells;
	}

	// a cache the test can empty
	private static class Tiles implements TileFactory {
		private final TileGenerator generator;
		private final Map<Long, Tile> tiles = new HashMap<>();
		private int loads;

		Tiles(TileGenerator generator) {
			this.generator = generator;
		}

		@Override
		public Tile provide(int tileX, int tileZ) {
			return this.tiles.computeIfAbsent(PosUtil.pack(tileX, tileZ), (k) -> {
				this.loads++;
				return this.generator.generate(tileX, tileZ).join();
			});
		}

		@Override
		public void queue(int tileX, int tileZ) {
		}

		@Override
		public void dropAtChunk(int chunkX, int chunkZ) {
		}

		@Override
		public int chunkToTile(int chunkCoord) {
			return chunkCoord >> 3;
		}
	}
}
