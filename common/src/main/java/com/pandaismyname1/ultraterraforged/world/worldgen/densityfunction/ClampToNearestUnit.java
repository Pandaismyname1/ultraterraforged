package com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

record ClampToNearestUnit(DensityFunction function, int resolution) implements DensityFunction {
	public static final MapCodec<ClampToNearestUnit> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		DensityFunction.CODEC.fieldOf("function").forGetter(ClampToNearestUnit::function),
		Codec.INT.fieldOf("resolution").forGetter(ClampToNearestUnit::resolution)
	).apply(instance, ClampToNearestUnit::new));

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		return new Sampler(this.function.compileSampler(context), this.resolution);
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		DensityFunction function = rule.rewrite(this.function);
		return function == this.function ? this : new ClampToNearestUnit(function, this.resolution);
	}

	@Override
	public Interval range() {
		Interval range = this.function.range();
		if (Float.isInfinite(range.min()) || Float.isInfinite(range.max())) {
			return Interval.INFINITE;
		}
		return Interval.of(clamp(range.min(), this.resolution), clamp(range.max(), this.resolution));
	}

	@Override
	public @Axes int domainAxes() {
		return this.function.domainAxes();
	}

	@Override
	public MapCodec<ClampToNearestUnit> codec() {
		return CODEC;
	}

	private static float clamp(float value, int resolution) {
		float scaled = (int) (value * resolution) + 1;
		return scaled / resolution;
	}

	private record Sampler(DensitySampler function, int resolution) implements DensitySampler {

		@Override
		public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
			return clamp(this.function.sampleValue(context, blockX, blockY, blockZ), this.resolution);
		}

		@Override
		public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
			this.function.sampleVolume(context, outputBuffer, volume);
			for (int i = 0; i < volume.size(); i++) {
				outputBuffer.set(i, clamp(outputBuffer.get(i), this.resolution));
			}
		}
	}
}
