package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.preset.option.PresetOptions;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.ContinentType;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

/**
 * Every continent type the settings offer makes a world with both land and sea: Experimental once failed as soon as
 * the world was made.
 */
public class ContinentTypesTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void everyContinentTypeMakesLandAndSea() {
		for (ContinentType type : PresetOptions.CONTINENT_TYPE.values()) {
			Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
			PresetOptions.CONTINENT_TYPE.set(preset, type);
			Tile tile = PresetRenderer.generate(preset);
			int[] counts = new int[2];
			tile.iterate((cell, x, z) -> counts[cell.continentEdge > 0.5F ? 0 : 1]++);
			System.out.printf("%s: %d inland, %d coast and sea%n", type, counts[0], counts[1]);
			assertTrue(counts[0] > 1000, type + " made almost no land: " + counts[0]);
			assertTrue(counts[1] > 1000, type + " made almost no sea: " + counts[1]);
		}
	}
}
