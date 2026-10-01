package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.Identifier;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.preset.option.PresetOptions;

// The datapack a preset turns into when a world is created must build and serialize, and reflect the settings
public class PresetDatapackTest {
	private static final String PRESET_FILE = "data/ultraterraforged/ultraterraforged/worldgen/preset/preset.json";
	private static final String NOISE_SETTINGS = "data/minecraft/worldgen/noise_settings/overworld.json";
	private static final String CAVE_CARVER = "data/minecraft/worldgen/configured_carver/cave.json";
	private static final String VILLAGES = "data/minecraft/worldgen/structure_set/villages.json";

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@TestFactory
	Stream<DynamicTest> everyBuiltinPresetGeneratesAValidDatapack() {
		return BuiltinPresetRenderTest.presets().entrySet().stream().map((entry) -> DynamicTest.dynamicTest(entry.getKey(), () -> {
			Preset preset = entry.getValue().get();
			PresetDatapack.Result pack = PresetDatapack.generate(preset);
			assertTrue(pack.errors().isEmpty(), String.join("\n", pack.errors()));
			for (String file : List.of(PRESET_FILE, NOISE_SETTINGS, CAVE_CARVER, "data/minecraft/dimension_type/overworld.json")) {
				assertTrue(pack.files().containsKey(file), "missing " + file);
			}
			// the preset travels with the world and reads back unchanged
			Preset stored = Preset.CODEC.parse(JsonOps.INSTANCE, pack.files().get(PRESET_FILE)).getOrThrow((error) -> new AssertionError(error));
			assertEquals(PresetOptionsTest.encode(preset), PresetOptionsTest.encode(stored));
		}));
	}

	// other dimensions, modded ones especially, build on vanilla's overworld functions: replacing those gave them the
	// overworld's terrain
	@Test
	void vanillaDensityFunctionsAreLeftAlone() {
		PresetDatapack.Result pack = PresetDatapack.generate(defaultPreset());
		assertTrue(pack.files().keySet().stream().noneMatch((file) -> file.startsWith("data/minecraft/worldgen/density_function/")), String.join("\n", pack.files().keySet()));
		assertTrue(pack.files().containsKey("data/ultraterraforged/worldgen/density_function/overworld/sloped_cheese.json"));
		// the overworld reads UltraTerraForged's, and vanilla's only where they're unchanged
		String noise = pack.files().get(NOISE_SETTINGS).toString();
		assertTrue(noise.contains("\"ultraterraforged:overworld/sloped_cheese\""), noise);
		for (String replaced : List.of("continents", "erosion", "ridges", "offset", "factor", "depth", "jaggedness", "sloped_cheese", "caves/noodle", "caves/entrances", "caves/spaghetti_2d")) {
			assertTrue(!noise.contains("\"minecraft:overworld/" + replaced + "\""), replaced);
		}
	}

	@Test
	void worldHeightReachesTheDimension() {
		Preset preset = defaultPreset();
		PresetOptions.WORLD_HEIGHT.set(preset, 512);
		PresetOptions.WORLD_DEPTH.set(preset, 128);
		JsonObject noise = file(preset, NOISE_SETTINGS).getAsJsonObject().getAsJsonObject("noise");
		assertEquals(-128, noise.get("min_y").getAsInt());
		assertEquals(640, noise.get("height").getAsInt());
	}

	@Test
	void caveSettingsReachTheCarvers() {
		Preset preset = defaultPreset();
		preset.caves().caveCarverProbability = 0.5F;
		assertEquals(0.5F, file(preset, CAVE_CARVER).getAsJsonObject().getAsJsonObject("config").get("probability").getAsFloat());
	}

	@Test
	void structuresAreOnlyOverriddenWhenChanged() {
		assertTrue(PresetDatapack.generate(defaultPreset()).files().keySet().stream().noneMatch((file) -> file.contains("/worldgen/structure_set/")));
	}

	@Test
	void structureSpacingAndDisablingReachTheStructureSets() {
		Preset preset = defaultPreset();
		Identifier villages = Identifier.parse("villages");
		preset.structures().getOrCreate(villages).spacing = 20;
		preset.structures().getOrCreate(villages).separation = 25;
		preset.structures().getOrCreate(Identifier.parse("pillager_outposts")).enabled = false;

		PresetDatapack.Result pack = PresetDatapack.generate(preset);
		assertTrue(pack.errors().isEmpty(), String.join("\n", pack.errors()));
		JsonObject placement = pack.files().get(VILLAGES).getAsJsonObject().getAsJsonObject("placement");
		assertEquals(20, placement.get("spacing").getAsInt());
		// separation must stay below spacing or vanilla rejects the placement
		assertEquals(19, placement.get("separation").getAsInt());
		assertTrue(pack.files().get(VILLAGES).getAsJsonObject().getAsJsonArray("structures").size() > 0);

		JsonObject outposts = pack.files().get("data/minecraft/worldgen/structure_set/pillager_outposts.json").getAsJsonObject();
		assertEquals(0, outposts.getAsJsonArray("structures").size());
		// the exclusion zone that keeps outposts away from villages still points at the village set
		assertEquals("minecraft:villages", outposts.getAsJsonObject("placement").getAsJsonObject("exclusion_zone").get("other_set").getAsString());
	}

	private static JsonElement file(Preset preset, String path) {
		PresetDatapack.Result pack = PresetDatapack.generate(preset);
		assertTrue(pack.errors().isEmpty(), String.join("\n", pack.errors()));
		return pack.files().get(path);
	}

	private static Preset defaultPreset() {
		return BuiltinPresetRenderTest.presets().get("default").get();
	}
}
