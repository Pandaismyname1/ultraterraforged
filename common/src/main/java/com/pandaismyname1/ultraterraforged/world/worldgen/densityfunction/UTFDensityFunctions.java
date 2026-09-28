package com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellField;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public class UTFDensityFunctions {

	public static void bootstrap() {
		register("noise_sampler", NoiseSampler.CODEC);
		register("cell", CellSampler.CODEC);
		register("clamp_to_nearest_unit", ClampToNearestUnit.CODEC);
		register("linear_spline", LinearSplineFunction.CODEC);
	}

	public static NoiseSampler noise(Holder<Noise> noise) {
		return new NoiseSampler(noise);
	}

	public static CellSampler cell(CellField field) {
		return new CellSampler(field);
	}

	public static ClampToNearestUnit clampToNearestUnit(DensityFunction function, int resolution) {
		return new ClampToNearestUnit(function, resolution);
	}

	/**
	 * Whether a router reads UltraTerraForged's terrain anywhere: only then does its world need a generator.
	 */
	public static boolean usesCells(NoiseRouter router) {
		CellFinder finder = new CellFinder();
		Stream.of(router.temperature(), router.vegetation(), router.continents(), router.erosion(), router.depth(), router.ridges(), router.chunkSurfaceLevel(), router.finalDensity()).forEach(finder::rewrite);
		return finder.found;
	}

	private static void register(String name, MapCodec<? extends DensityFunction> type) {
		RegistryUtil.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, name, type);
	}

	// walks a function and everything it references, visiting shared functions once
	private static class CellFinder implements DfRewriteRule {
		private final Set<DensityFunction> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		private boolean found;

		@Override
		public DensityFunction rewrite(DensityFunction function) {
			DensityFunction inlined = DfRewriteRule.INLINE_REFERENCE.rewrite(function);
			if (this.found || !this.visited.add(inlined)) {
				return function;
			}
			if (inlined instanceof CellSampler) {
				this.found = true;
			} else {
				inlined.rewriteChildren(this);
			}
			return function;
		}
	}
}
