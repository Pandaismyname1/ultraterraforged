package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.CellSampler;

/**
 * The cell a worker thread last looked up is only reused for the same world: two worlds, open one after the other or
 * side by side, share the threads (audit A02, discussion #12).
 */
public class CellCacheTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void anotherWorldAtTheSamePlaceIsLookedUpAgain() {
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		GeneratorContext first = GeneratorContext.makeCached(preset, 1234, 3, 6, false);
		GeneratorContext second = GeneratorContext.makeCached(preset, 98765, 3, 6, false);
		int x = 1000, z = -2000;
		float firstHeight = heightAt(first, x, z);
		float secondHeight = heightAt(second, x, z);
		assertNotEquals(firstHeight, secondHeight, "the two seeds should differ here");

		CellSampler.Cache2d cache = new CellSampler.Cache2d();
		assertEquals(firstHeight, cache.getAndUpdate(first.lookup, x, z).height);
		assertEquals(secondHeight, cache.getAndUpdate(second.lookup, x, z).height);
		assertEquals(firstHeight, cache.getAndUpdate(first.lookup, x, z).height);
	}

	private static float heightAt(GeneratorContext context, int x, int z) {
		Cell cell = new Cell();
		context.lookup.apply(cell, x, z);
		return cell.height;
	}
}
