package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import com.pandaismyname1.ultraterraforged.compat.generatoraccelerator.GeneratorAcceleratorCodecs;

// what the Generator Accelerator compat writes in its place: vanilla's own names and splines (GeneratorAcceleratorCompat, GeneratorAcceleratorCodecs)
public class GeneratorAcceleratorCompatTest {
	private static final ToFloatFunction<Float> COORDINATE = ToFloatFunction.createUnlimited(x -> x);

	// Generator Accelerator's FastMultipoint has these parts, as a record
	private record ForeignSpline(ToFloatFunction<Float> coordinate, float[] locations, CubicSpline<Float, ToFloatFunction<Float>>[] values, float[] derivatives, float minValue, float maxValue) implements CubicSpline<Float, ToFloatFunction<Float>> {

		@Override
		public float apply(Float x) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String parityString() {
			return "foreign";
		}

		@Override
		public CubicSpline<Float, ToFloatFunction<Float>> mapAll(CoordinateVisitor<ToFloatFunction<Float>> visitor) {
			return this;
		}
	}

	@BeforeAll
	public static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	public void vanillaTypesHaveTheirVanillaNames() {
		DensityFunction a = DensityFunctions.blendAlpha(), b = DensityFunctions.blendOffset(), zero = DensityFunctions.zero();
		for (DensityFunction function : List.of(DensityFunctions.add(a, b), DensityFunctions.add(zero, a), DensityFunctions.mul(a, b), DensityFunctions.mul(zero, a), DensityFunctions.min(a, b), DensityFunctions.max(a, b), a.abs(), a.square(), a.cube(), a.halfNegative(), a.quarterNegative(), a.squeeze(), a.clamp(-1.0D, 1.0D), DensityFunctions.rangeChoice(a, 0.0D, 1.0D, zero, b), DensityFunctions.shiftedNoise2d(a, b, 1.0D, Holder.direct(new NormalNoise.NoiseParameters(0, 1.0D))))) {
			Object codec = function.codec().codec();
			assertEquals(BuiltInRegistries.DENSITY_FUNCTION_TYPE.getResourceKey((com.mojang.serialization.Codec<? extends DensityFunction>) codec).orElseThrow(), GeneratorAcceleratorCodecs.vanillaTypeKey(codec), function.toString());
		}
	}

	@Test
	public void foreignSplinesBecomeVanillaOnes() {
		CubicSpline<Float, ToFloatFunction<Float>> inner = CubicSpline.builder(COORDINATE).addPoint(-0.5F, 1.0F).addPoint(0.5F, -2.0F, 0.3F).build();
		CubicSpline<Float, ToFloatFunction<Float>> vanilla = CubicSpline.builder(COORDINATE).addPoint(-1.0F, inner).addPoint(0.0F, 4.0F).addPoint(0.7F, inner).build();
		CubicSpline<Float, ToFloatFunction<Float>> converted = GeneratorAcceleratorCodecs.vanillaSpline(foreign(vanilla));

		assertInstanceOf(CubicSpline.Multipoint.class, converted);
		assertInstanceOf(CubicSpline.Multipoint.class, ((CubicSpline.Multipoint<Float, ToFloatFunction<Float>>) converted).values().get(0));
		for (float x = -1.5F; x <= 1.5F; x += 0.01F) {
			assertEquals(vanilla.apply(x), converted.apply(x), x + "");
		}
		assertEquals(vanilla.minValue(), converted.minValue());
		assertEquals(vanilla.maxValue(), converted.maxValue());
	}

	@SuppressWarnings("unchecked")
	private static CubicSpline<Float, ToFloatFunction<Float>> foreign(CubicSpline<Float, ToFloatFunction<Float>> spline) {
		if (!(spline instanceof CubicSpline.Multipoint<Float, ToFloatFunction<Float>> multipoint)) {
			return spline;
		}
		CubicSpline<Float, ToFloatFunction<Float>>[] values = multipoint.values().stream().map(GeneratorAcceleratorCompatTest::foreign).toArray(CubicSpline[]::new);
		return new ForeignSpline(multipoint.coordinate(), Arrays.copyOf(multipoint.locations(), multipoint.locations().length), values, multipoint.derivatives(), multipoint.minValue(), multipoint.maxValue());
	}
}
