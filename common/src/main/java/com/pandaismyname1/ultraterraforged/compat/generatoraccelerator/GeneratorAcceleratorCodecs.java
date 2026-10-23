package com.pandaismyname1.ultraterraforged.compat.generatoraccelerator;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import com.mojang.serialization.Codec;

// what Generator Accelerator's density functions and splines are written as (GeneratorAcceleratorCompat)
public final class GeneratorAcceleratorCodecs {

	private GeneratorAcceleratorCodecs() {
	}

	/**
	 * The name of a vanilla density function type whose codec isn't in the registry (Generator Accelerator registered
	 * its own under that name), or null.
	 */
	@Nullable
	public static ResourceKey<Codec<? extends DensityFunction>> vanillaTypeKey(Object codec) {
		return VanillaTypes.KEYS.get(codec);
	}

	/**
	 * The spline with every part another mod made (Generator Accelerator's FastMultipoint, which vanilla's codec can't
	 * write) made a vanilla Multipoint again. A part that isn't a record with Multipoint's parts is kept as it is.
	 */
	@SuppressWarnings("unchecked")
	public static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> vanillaSpline(CubicSpline<C, I> spline) {
		if (spline instanceof CubicSpline.Constant) {
			return spline;
		}
		if (spline instanceof CubicSpline.Multipoint<C, I> multipoint) {
			return new CubicSpline.Multipoint<>(multipoint.coordinate(), multipoint.locations(), vanillaSplines(multipoint.values()), multipoint.derivatives(), multipoint.minValue(), multipoint.maxValue());
		}
		try {
			Object values = component(spline, "values");
			List<CubicSpline<C, I>> list = values instanceof Object[] array ? (List<CubicSpline<C, I>>) (List<?>) Arrays.asList(array) : (List<CubicSpline<C, I>>) values;
			return new CubicSpline.Multipoint<>((I) component(spline, "coordinate"), (float[]) component(spline, "locations"), vanillaSplines(list), (float[]) component(spline, "derivatives"), spline.minValue(), spline.maxValue());
		} catch (ReflectiveOperationException | ClassCastException | NullPointerException e) {
			return spline;
		}
	}

	private static <C, I extends ToFloatFunction<C>> List<CubicSpline<C, I>> vanillaSplines(List<CubicSpline<C, I>> splines) {
		return splines.stream().map(GeneratorAcceleratorCodecs::<C, I>vanillaSpline).toList();
	}

	private static Object component(Object record, String name) throws ReflectiveOperationException {
		for (RecordComponent component : record.getClass().getRecordComponents()) {
			if (component.getName().equals(name)) {
				Method accessor = component.getAccessor();
				accessor.trySetAccessible();
				return accessor.invoke(record);
			}
		}
		throw new NoSuchMethodException(name);
	}

	// the codecs of vanilla's types, from functions made in code as vanilla makes them
	private static final class VanillaTypes {
		private static final Map<Object, ResourceKey<Codec<? extends DensityFunction>>> KEYS = new IdentityHashMap<>();

		static {
			DensityFunction a = DensityFunctions.blendAlpha();
			DensityFunction b = DensityFunctions.blendOffset();
			DensityFunction zero = DensityFunctions.zero();
			// add and mul with a constant argument are a different class, with the same type
			put("add", DensityFunctions.add(a, b), DensityFunctions.add(zero, a));
			put("mul", DensityFunctions.mul(a, b), DensityFunctions.mul(zero, a));
			put("min", DensityFunctions.min(a, b));
			put("max", DensityFunctions.max(a, b));
			put("abs", a.abs());
			put("square", a.square());
			put("cube", a.cube());
			put("half_negative", a.halfNegative());
			put("quarter_negative", a.quarterNegative());
			put("squeeze", a.squeeze());
			put("clamp", a.clamp(-1.0D, 1.0D));
			put("range_choice", DensityFunctions.rangeChoice(a, 0.0D, 1.0D, zero, b));
			put("shifted_noise", DensityFunctions.shiftedNoise2d(a, b, 1.0D, Holder.direct(new NormalNoise.NoiseParameters(0, 1.0D))));
		}

		private static void put(String name, DensityFunction... functions) {
			ResourceKey<Codec<? extends DensityFunction>> key = ResourceKey.create(Registries.DENSITY_FUNCTION_TYPE, new ResourceLocation(name));
			for (DensityFunction function : functions) {
				KEYS.put(function.codec().codec(), key);
			}
		}
	}
}
