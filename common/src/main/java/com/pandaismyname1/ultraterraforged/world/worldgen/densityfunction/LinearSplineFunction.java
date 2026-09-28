package com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Interval;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;

public record LinearSplineFunction(DensityFunction input, List<Pair<Double, DensityFunction>> points) implements DensityFunction {
	public static final MapCodec<LinearSplineFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		DensityFunction.CODEC.fieldOf("input").forGetter(LinearSplineFunction::input),
		ExtraCodecs.nonEmptyList(Codec.pair(Codec.DOUBLE, DensityFunction.CODEC).listOf()).fieldOf("points").forGetter(LinearSplineFunction::points)
	).apply(instance, LinearSplineFunction::new));

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		double[] locations = new double[this.points.size()];
		DensitySampler[] values = new DensitySampler[this.points.size()];
		for (int i = 0; i < locations.length; i++) {
			Pair<Double, DensityFunction> point = this.points.get(i);
			locations[i] = point.getFirst();
			values[i] = point.getSecond().compileSampler(context);
		}
		return new Sampler(this.input.compileSampler(context), locations, values);
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		return new LinearSplineFunction(rule.rewrite(this.input), this.points.stream().map((point) -> {
			return Pair.of(point.getFirst(), rule.rewrite(point.getSecond()));
		}).toList());
	}

	@Override
	public Interval range() {
		return Interval.encapsulating(this.points.stream().map((point) -> point.getSecond().range()).toList());
	}

	@Override
	public @Axes int domainAxes() {
		int axes = this.input.domainAxes();
		for (Pair<Double, DensityFunction> point : this.points) {
			axes |= point.getSecond().domainAxes();
		}
		return axes;
	}

	@Override
	public MapCodec<LinearSplineFunction> codec() {
		return CODEC;
	}

	public static LinearSplineFunction.Builder builder(DensityFunction input) {
		return new LinearSplineFunction.Builder(input);
	}

	private record Sampler(DensitySampler input, double[] locations, DensitySampler[] values) implements DensitySampler {

		@Override
		public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
			double input = this.input.sampleValue(context, blockX, blockY, blockZ);
			int last = this.locations.length - 1;
			if(input <= this.locations[0]) {
				return this.values[0].sampleValue(context, blockX, blockY, blockZ);
			}
			if(input >= this.locations[last]) {
				return this.values[last].sampleValue(context, blockX, blockY, blockZ);
			}

			int index = Mth.binarySearch(0, this.locations.length, i -> input < this.locations[i]) - 1;
			double min = this.locations[index];
			double max = this.locations[index + 1];
			double from = this.values[index].sampleValue(context, blockX, blockY, blockZ);
			double to = this.values[index + 1].sampleValue(context, blockX, blockY, blockZ);

			double lerp = NoiseUtil.map(input, 0.0D, 1.0D, min, max);
			lerp = NoiseUtil.clamp(lerp, 0.0D, 1.0D);
			return (float) NoiseUtil.lerp(from, to, lerp);
		}

		@Override
		public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
			DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
		}
	}

	public static class Builder {
		private DensityFunction input;
		private List<Pair<Double, DensityFunction>> points;

		public Builder(DensityFunction input) {
			this.input = input;
			this.points = new ArrayList<>();
		}

		public Builder addPoint(double point, float value) {
			return this.addPoint(point, DensityFunctions.constant(value));
		}

		public Builder addPoint(double point, DensityFunction value) {
			this.points.add(Pair.of(point, value));
			return this;
		}

		public LinearSplineFunction build() {
			return new LinearSplineFunction(this.input, ImmutableList.copyOf(this.points));
		}
	}
}
