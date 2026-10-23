package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.UTFDensityFunctions;

// the terrain height is rounded to the terrain scaler's steps: its bounds have to hold every value it gives, as density
// compilers go by them
public class ClampToNearestUnitTest {

	@BeforeAll
	public static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	public void anUnboundedFunctionStaysUnbounded() {
		DensityFunction unbounded = new DensityFunction.SimpleFunction() {

			@Override
			public double compute(FunctionContext ctx) {
				return 0.3D;
			}

			@Override
			public double minValue() {
				return Double.NEGATIVE_INFINITY;
			}

			@Override
			public double maxValue() {
				return Double.POSITIVE_INFINITY;
			}

			@Override
			public net.minecraft.util.KeyDispatchDataCodec<? extends DensityFunction> codec() {
				throw new UnsupportedOperationException();
			}
		};
		DensityFunction clamped = UTFDensityFunctions.clampToNearestUnit(unbounded, 64);
		assertEquals(Double.NEGATIVE_INFINITY, clamped.minValue());
		assertEquals(Double.POSITIVE_INFINITY, clamped.maxValue());
	}

	@Test
	public void boundsHoldEveryValue() {
		for (int resolution : new int[] { 1, 7, 64, 512 }) {
			for (double min = -1.0D; min <= 1.0D; min += 0.125D) {
				double max = min + 0.375D;
				DensityFunction clamped = UTFDensityFunctions.clampToNearestUnit(DensityFunctions.constant(min), resolution);
				DensityFunction clampedMax = UTFDensityFunctions.clampToNearestUnit(DensityFunctions.constant(max), resolution);
				assertTrue(clamped.minValue() <= clamped.compute(new DensityFunction.SinglePointContext(0, 0, 0)));
				assertTrue(clamped.minValue() <= clampedMax.maxValue(), resolution + " " + min);
			}
		}
	}
}
