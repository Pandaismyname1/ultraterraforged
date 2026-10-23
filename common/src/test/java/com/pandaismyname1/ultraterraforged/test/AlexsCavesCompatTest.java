package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.compat.alexscaves.AlexsCavesCompat;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;

/**
 * Alex's Caves' inland cave biomes ask for continentalness that UltraTerraForged's land rarely has (#7): the
 * continentalness AlexsCavesCompat gives them reaches those ranges about as often as vanilla's does, and the sea's is
 * left as it is.
 */
public class AlexsCavesCompatTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void inlandAsOftenAsInVanilla() {
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		Cell cell = new Cell();
		int samples = 0, before = 0, after = 0;
		for (int seed : new int[] { 3333, 98765, 42 }) {
			GeneratorContext context = GeneratorContext.makeCached(preset, seed, 3, 6, false);
			for (int x = -16000; x <= 16000; x += 320) {
				for (int z = -16000; z <= 16000; z += 320) {
					context.lookup.apply(cell.reset(), x, z, false);
					samples++;
					before += cell.continentalness >= 0.5F ? 1 : 0;
					after += AlexsCavesCompat.continentalness(cell.continentalness) >= 0.5F ? 1 : 0;
				}
			}
		}
		System.out.printf("continentalness 0.5 or more: %.1f%% of the world, %.1f%% as Alex's Caves sees it%n", 100.0F * before / samples, 100.0F * after / samples);
		// vanilla: 8.5%
		assertTrue(after > samples * 0.065F && after < samples * 0.105F, "as Alex's Caves sees it: " + 100.0F * after / samples + "%");
	}

	@Test
	void keepsOrderAndTheSea() {
		float last = -2.0F;
		for (float terrain = -1.2F; terrain <= 1.0F; terrain += 0.001F) {
			float seen = AlexsCavesCompat.continentalness(terrain);
			assertTrue(seen >= last, "lower inland than nearer the sea at " + terrain);
			assertTrue(seen <= 1.0F, "above vanilla's range at " + terrain);
			last = seen;
		}
		assertEquals(-0.8F, AlexsCavesCompat.continentalness(-0.8F));
		assertEquals(-0.3F, AlexsCavesCompat.continentalness(-0.3F));
	}
}
