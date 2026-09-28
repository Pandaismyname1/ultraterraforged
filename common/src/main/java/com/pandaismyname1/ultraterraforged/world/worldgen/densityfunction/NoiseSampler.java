package com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public record NoiseSampler(Holder<Noise> noise, int seed) implements MappedFunction {

	@Override
	public double compute(FunctionContext ctx) {
		return this.noise.value().compute(ctx.blockX(), ctx.blockZ(), this.seed);
	}

	@Override
	public double minValue() {
		return this.noise.value().minValue();
	}

	@Override
	public double maxValue() {
		return this.noise.value().maxValue();
	}
	
	public record Marker(Holder<Noise> noise) implements MappedFunction.Marker {
		public static final Codec<NoiseSampler.Marker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Noise.CODEC.fieldOf("noise").forGetter(NoiseSampler.Marker::noise)
		).apply(instance, NoiseSampler.Marker::new));

		@Override
		public KeyDispatchDataCodec<NoiseSampler.Marker> codec() {
			return UTFCodecs.keyDispatch(CODEC);
		}

	}
}
