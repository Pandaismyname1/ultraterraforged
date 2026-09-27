package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.preset.settings.LandformSettings;
import raccoonman.reterraforged.data.preset.settings.Preset;

/**
 * How much the landforms slow down terrain generation, over areas at full resolution like world generation uses.
 * Only runs when asked: -Drtf.render=true
 */
public class LandformPerformanceTest {
	private static final int AREAS = 24;

	@Test
	void landformCost() {
		assumeTrue(Boolean.getBoolean("rtf.render"));
		TestBootstrap.init();
		for (String name : new String[] { "default", "badlands", "frozen_north", "tropics" }) {
			Preset with = BuiltinPresetRenderTest.presets().get(name).get();
			Preset without = with.copy();
			without.landforms().buttes.enabled = false;
			without.landforms().canyons.enabled = false;
			without.landforms().seaCliffs.enabled = false;
			without.landforms().fjords.enabled = false;
			without.landforms().atolls.enabled = false;
			without.landforms().dunes.enabled = false;
			without.landforms().tors.enabled = false;
			without.landforms().saltFlats.enabled = false;
			without.landforms().alluvialFans.enabled = false;
			without.landforms().glacialValleys.enabled = false;
			without.landforms().drumlins.enabled = false;
			without.landforms().barrierIslands.enabled = false;
			without.landforms().karst.enabled = false;
			without.landforms().deltas.enabled = false;
			// warm up
			time(with);
			time(without);
			long on = time(with);
			long off = time(without);
			System.out.printf("%-14s landforms on %5d ms, off %5d ms: %+.0f%%%n", name, on, off, (on - off) * 100.0 / off);
		}
	}

	// generates areas spread over the world, each 256 blocks across at one block per cell
	private static long time(Preset preset) {
		long start = System.nanoTime();
		for (int i = 0; i < AREAS; i++) {
			float x = (i % 6 - 3) * 1500.0F;
			float z = (i / 6 - 2) * 1500.0F;
			TerrainViews.view(preset, x, z, 1.0F);
		}
		return (System.nanoTime() - start) / 1_000_000L;
	}

	@SuppressWarnings("unused")
	private static LandformSettings none() {
		return LandformSettings.makeNone();
	}
}
