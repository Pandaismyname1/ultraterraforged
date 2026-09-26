package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.preset.PresetStrataNoise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule;

/**
 * The noises that bend the rock layers: layers must tilt, differently from region to region, but never so far that
 * they run off the end of a layer stack.
 */
public class StrataNoiseTest {
	private static final int SAMPLES = 200;
	private static final int SPACING = 64;
	// the slope is measured over this many blocks
	private static final int STEP = 16;

	private static PresetStrataNoise.Layers layers;

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
		layers = PresetStrataNoise.make(600);
	}

	@Test
	void offsetStaysWithinTheStackMargin() {
		Noise offset = layers.offset();
		assertTrue(offset.maxValue() <= PresetStrataNoise.MAX_OFFSET + 0.01F && offset.minValue() >= -PresetStrataNoise.MAX_OFFSET - 0.01F, offset.minValue() + " to " + offset.maxValue());
		assertTrue(PresetStrataNoise.MAX_OFFSET < StrataRule.MARGIN / 2.0F);
		float max = 0.0F;
		for (int x = 0; x < SAMPLES; x++) {
			for (int z = 0; z < SAMPLES; z++) {
				max = Math.max(max, Math.abs(offset.compute(x * SPACING, z * SPACING, 0)));
			}
		}
		assertTrue(max <= PresetStrataNoise.MAX_OFFSET + 0.01F, "offset reached " + max);
		// and actually uses a good part of the range, or the layers would look flat
		assertTrue(max > PresetStrataNoise.MAX_OFFSET * 0.4F, "offset only reached " + max);
	}

	@Test
	void layersTiltMoreInSomeRegionsThanOthers() {
		Noise offset = layers.offset();
		float[] slopes = new float[SAMPLES * SAMPLES];
		int i = 0;
		for (int x = 0; x < SAMPLES; x++) {
			for (int z = 0; z < SAMPLES; z++) {
				float px = x * SPACING;
				float pz = z * SPACING;
				float dx = offset.compute(px + STEP, pz, 0) - offset.compute(px, pz, 0);
				float dz = offset.compute(px, pz + STEP, 0) - offset.compute(px, pz, 0);
				slopes[i++] = (float) Math.sqrt(dx * dx + dz * dz) / STEP;
			}
		}
		Arrays.sort(slopes);
		float gentle = slopes[slopes.length / 10];
		float steep = slopes[slopes.length * 9 / 10];
		System.out.printf("layer slope: 10%% %.3f, median %.3f, 90%% %.3f%n", gentle, slopes[slopes.length / 2], steep);
		// visibly tilted somewhere: more than 1 block in 5
		assertTrue(steep > 0.2F, "steepest layers only rise " + steep + " per block");
		// but not everywhere alike
		assertTrue(steep > gentle * 3.0F, "slopes range from " + gentle + " to " + steep);
	}

	@Test
	void thicknessStretchStaysGentle() {
		Noise thickness = layers.thickness();
		assertTrue(thickness.minValue() >= 0.5F && thickness.maxValue() <= 2.0F, thickness.minValue() + " to " + thickness.maxValue());
	}
}
