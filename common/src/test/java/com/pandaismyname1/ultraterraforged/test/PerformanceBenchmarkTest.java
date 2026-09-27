package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.data.preset.PresetLibrary;
import com.pandaismyname1.ultraterraforged.data.preset.settings.CaveFeatureSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.CoastSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.LandformSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.OceanSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.TerrainLocator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Benchmarks of the terrain pipeline as world generation runs it: whole tiles, with the erosion and smoothing filters,
 * in clusters like the land around a player exploring. Time is the CPU time of the whole process, so other programs
 * running at the same time barely skew it. Only runs when asked: -Drtf.bench=true
 */
public class PerformanceBenchmarkTest {
	private static final int SEED = PresetRenderer.SEED;
	// the game's default tile size: 8 chunks across
	private static final int TILE_SIZE = 3;
	private static final int TILE_CHUNKS = 1 << TILE_SIZE;
	// clusters of 3 by 3 tiles spread over the world
	private static final int[][] CLUSTERS = { { 0, 0 }, { 40, -25 }, { -60, 35 }, { 90, 80 }, { -110, -95 }, { 150, -140 } };
	private static final int ROUNDS = 5;

	@BeforeAll
	static void bootstrap() {
		assumeTrue(Boolean.getBoolean("rtf.bench"));
		TestBootstrap.init();
	}

	private static Preset builtin(String name) {
		return BuiltinPresetRenderTest.presets().get(name).get();
	}

	// the default preset with everything added since the original UltraTerraForged turned off
	private static Preset withoutNew(Preset preset) {
		Preset p = replace(preset, LandformSettings.makeNone(), CoastSettings.makeNone(), CaveFeatureSettings.makeNone(), OceanSettings.makeNone());
		p.rivers().winding = false;
		p.rivers().raisedWater = false;
		return p;
	}

	private static Preset replace(Preset p, LandformSettings landforms, CoastSettings coasts, CaveFeatureSettings caves, OceanSettings oceans) {
		return new Preset(p.world(), p.surface(), p.caves(), p.climate(), p.terrain(), p.rivers(), p.filters(), p.structures(), p.miscellaneous(), landforms, coasts, caves, oceans);
	}

	private static Preset change(String name, UnaryOperator<Preset> change) {
		return change.apply(builtin(name));
	}

	@Test
	void tileGeneration() {
		Map<String, Preset> configs = new LinkedHashMap<>();
		configs.put("default", builtin("default"));
		configs.put("default, no landforms", change("default", (p) -> replace(p, LandformSettings.makeNone(), p.coasts(), p.caveFeatures(), p.oceans())));
		configs.put("default, no coasts", change("default", (p) -> replace(p, p.landforms(), CoastSettings.makeNone(), p.caveFeatures(), p.oceans())));
		configs.put("default, no oceans", change("default", (p) -> replace(p, p.landforms(), p.coasts(), p.caveFeatures(), OceanSettings.makeNone())));
		configs.put("default, straight rivers", change("default", (p) -> {
			p.rivers().winding = false;
			return p;
		}));
		configs.put("default, nothing new", change("default", PerformanceBenchmarkTest::withoutNew));
		configs.put("legacy_default", builtin("legacy_default"));
		for (String name : new String[] { "archipelago", "highlands", "badlands", "frozen_north", "tropics", "volcanic_isles", "waterlands" }) {
			configs.put(name, builtin(name));
		}

		int chunks = CLUSTERS.length * 9 * TILE_CHUNKS * TILE_CHUNKS;
		Map<String, List<double[]>> results = new LinkedHashMap<>();
		configs.keySet().forEach((name) -> results.put(name, new ArrayList<>()));
		// warm up the JIT on every configuration first
		for (Map.Entry<String, Preset> entry : configs.entrySet()) {
			generate(entry.getValue(), 2);
		}
		// interleaved, so slow drift in the machine's speed spreads evenly over the configurations
		for (int round = 0; round < ROUNDS; round++) {
			for (Map.Entry<String, Preset> entry : configs.entrySet()) {
				results.get(entry.getKey()).add(generate(entry.getValue(), CLUSTERS.length));
			}
		}

		double base = median(results.get("default, nothing new"), 0);
		System.out.printf("%nTile generation, %d chunks per run (%d tiles of %dx%d chunks), median of %d runs%n", chunks, CLUSTERS.length * 9, TILE_CHUNKS, TILE_CHUNKS, ROUNDS);
		System.out.printf("%-26s %12s %12s %12s %10s%n", "preset", "CPU us/chunk", "wall us/chunk", "chunks/s", "vs base");
		for (Map.Entry<String, List<double[]>> entry : results.entrySet()) {
			double cpu = median(entry.getValue(), 0);
			double wall = median(entry.getValue(), 1);
			System.out.printf("BENCH %-26s %12.0f %12.0f %12.0f %+9.1f%%%n", entry.getKey(), cpu * 1000.0 / chunks, wall * 1000.0 / chunks, chunks / (wall / 1000.0), (cpu - base) * 100.0 / base);
		}
	}

	// what each new feature costs: the default preset with just that one turned off
	@Test
	void featureCost() throws ReflectiveOperationException {
		Map<String, Preset> configs = new LinkedHashMap<>();
		configs.put("default", builtin("default"));
		for (String section : new String[] { "landforms", "coasts", "oceans" }) {
			Object settings = Preset.class.getMethod(section).invoke(builtin("default"));
			for (Field field : settings.getClass().getFields()) {
				if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
					continue;
				}
				String name = field.getName();
				configs.put(section + "." + name, change("default", (p) -> {
					try {
						Object feature = field.get(Preset.class.getMethod(section).invoke(p));
						feature.getClass().getField("enabled").setBoolean(feature, false);
					} catch (ReflectiveOperationException e) {
						throw new IllegalStateException(e);
					}
					return p;
				}));
			}
		}
		configs.put("rivers.winding", change("default", (p) -> {
			p.rivers().winding = false;
			return p;
		}));
		configs.put("rivers.raisedWater", change("default", (p) -> {
			p.rivers().raisedWater = false;
			return p;
		}));

		Map<String, List<double[]>> results = new LinkedHashMap<>();
		configs.keySet().forEach((name) -> results.put(name, new ArrayList<>()));
		for (Preset preset : configs.values()) {
			generate(preset, 2);
		}
		for (int round = 0; round < 3; round++) {
			for (Map.Entry<String, Preset> entry : configs.entrySet()) {
				results.get(entry.getKey()).add(generate(entry.getValue(), CLUSTERS.length));
			}
		}
		int chunks = CLUSTERS.length * 9 * TILE_CHUNKS * TILE_CHUNKS;
		double base = median(results.get("default"), 0);
		System.out.printf("%nEach feature turned off on its own, against the default preset (%.0f us of CPU per chunk)%n", base * 1000.0 / chunks);
		for (Map.Entry<String, List<double[]>> entry : results.entrySet()) {
			double cpu = median(entry.getValue(), 0);
			System.out.printf("FEATURE %-32s %8.0f us/chunk  saves %5.1f%%%n", entry.getKey(), cpu * 1000.0 / chunks, (base - cpu) * 100.0 / base);
		}
	}

	// the default preset only, over and over, to profile with -Drtf.jfr=<file>
	@Test
	void profileDefault() {
		assumeTrue(System.getProperty("rtf.profile") != null);
		Preset preset = builtin("default");
		for (int i = 0; i < 8; i++) {
			generate(preset, CLUSTERS.length);
		}
	}

	// generates the clusters with a fresh context, as a new world would; returns CPU and wall milliseconds
	private static double[] generate(Preset preset, int clusters) {
		GeneratorContext context = GeneratorContext.makeCached(preset, SEED, TILE_SIZE, 6, false);
		long cpu = cpuTime();
		long wall = System.nanoTime();
		for (int c = 0; c < clusters; c++) {
			int[] cluster = CLUSTERS[c];
			for (int dz = -1; dz <= 1; dz++) {
				for (int dx = -1; dx <= 1; dx++) {
					context.generator.generate(cluster[0] + dx, cluster[1] + dz).join();
				}
			}
		}
		return new double[] { (cpuTime() - cpu) / 1e6, (System.nanoTime() - wall) / 1e6 };
	}

	@Test
	void locate() {
		String[] targets = {
			"peninsula", "coastal_island", "sand_bar", "skerry", "volcanic_island", "river_island",
			"submarine_canyon", "ocean_trench", "seamount", "guyot", "ocean_ridge", "blue_hole", "coral_reef", "sand_waves",
			"salt_flats", "alluvial_fan", "glacial_valley", "cirque", "drumlins", "barrier_island", "karst", "delta",
			"mesa", "canyon", "volcano", "badlands"
		};
		Preset preset = builtin("default");
		System.out.printf("%n/rtf locate on the default preset, from 0 0, each on a fresh world%n");
		System.out.printf("%-20s %9s %9s%n", "terrain", "ms", "distance");
		for (String name : targets) {
			Terrain terrain = TerrainType.get(name);
			if (terrain == null) {
				System.out.printf("LOCATE %-20s unknown%n", name);
				continue;
			}
			GeneratorContext context = GeneratorContext.makeCached(preset, SEED, TILE_SIZE, 6, false);
			long start = System.nanoTime();
			TerrainLocator.Found found = TerrainLocator.locate(context.lookup, terrain, 0, 0, 24000, 30_000L);
			long ms = (System.nanoTime() - start) / 1_000_000L;
			if (found == null) {
				System.out.printf("LOCATE %-20s %9d %9s%n", name, ms, "not found");
			} else {
				System.out.printf("LOCATE %-20s %9d %9.0f%n", name, ms, Math.sqrt((double) found.x() * found.x() + (double) found.z() * found.z()));
			}
		}
	}

	// the presets the dedicated server benchmarks run with, to copy into config/ultraterraforged/server-preset.json
	@Test
	void writeServerPresets() throws IOException {
		Path folder = Path.of("build", "bench-presets");
		Files.createDirectories(folder);
		PresetLibrary.write(folder.resolve("default.json"), builtin("default"));
		PresetLibrary.write(folder.resolve("default_no_cave_features.json"), change("default", (p) -> replace(p, p.landforms(), p.coasts(), CaveFeatureSettings.makeNone(), p.oceans())));
		PresetLibrary.write(folder.resolve("default_nothing_new.json"), change("default", PerformanceBenchmarkTest::withoutNew));
		PresetLibrary.write(folder.resolve("legacy_default.json"), builtin("legacy_default"));
	}

	private static long cpuTime() {
		return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime();
	}

	private static double median(List<double[]> runs, int index) {
		double[] values = runs.stream().mapToDouble((r) -> r[index]).toArray();
		Arrays.sort(values);
		return values[values.length / 2];
	}
}
