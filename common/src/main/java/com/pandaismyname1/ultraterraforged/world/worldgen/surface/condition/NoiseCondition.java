package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public class NoiseCondition extends MaterialRuleContext.LazyXZCondition {
	private Noise noise;
	private float threshold;
	
	private NoiseCondition(MaterialRuleContext context, Noise noise, float threshold) {
		super(context);
		
		this.noise = noise;
		this.threshold = threshold;
	}

	@Override
	protected boolean compute() {
		return this.noise.compute(this.context.blockX(), this.context.blockZ(), 0) > this.threshold;
	}
	
	public record Source(Holder<Noise> noise, float threshold) implements MaterialCondition {
		public static final Codec<Source> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Noise.CODEC.fieldOf("noise").forGetter(Source::noise),
			Codec.FLOAT.fieldOf("threshold").forGetter(Source::threshold)
		).apply(instance, Source::new));
		
		@Override
		public NoiseCondition compile(MaterialRuleContext ctx) {
			return new NoiseCondition(ctx, this.noise.value(), this.threshold);
		}

		@Override
		public MapCodec<Source> codec() {
			return UTFCodecs.asMap(CODEC);
		}
	}
}
