package com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFCompileContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

// one of UltraTerraForged's 2D noises, seeded by the world
public record NoiseSampler(Holder<Noise> noise) implements DensityFunction {
	public static final MapCodec<NoiseSampler> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Noise.CODEC.fieldOf("noise").forGetter(NoiseSampler::noise)
	).apply(instance, NoiseSampler::new));

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		RandomState randomState = UTFCompileContext.randomState(context);
		return new Sampler(this.noise.value(), randomState != null ? (int) randomState.seed() : 0);
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		return this;
	}

	@Override
	public Interval range() {
		Noise noise = this.noise.value();
		return Interval.of(noise.minValue(), noise.maxValue());
	}

	@Override
	public @Axes int domainAxes() {
		return AXIS_X | AXIS_Z;
	}

	@Override
	public MapCodec<NoiseSampler> codec() {
		return CODEC;
	}

	private record Sampler(Noise noise, int seed) implements DensitySampler {

		@Override
		public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
			return this.noise.compute(blockX, blockZ, this.seed);
		}

		@Override
		public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
			int sizeY = volume.sizeY();
			for (int z = 0; z < volume.sizeZ(); z++) {
				int blockZ = volume.blockZ(z);
				for (int x = 0; x < volume.sizeX(); x++) {
					outputBuffer.setRange(volume.indexUnchecked(x, 0, z), sizeY, this.noise.compute(volume.blockX(x), blockZ, this.seed));
				}
			}
		}
	}
}
