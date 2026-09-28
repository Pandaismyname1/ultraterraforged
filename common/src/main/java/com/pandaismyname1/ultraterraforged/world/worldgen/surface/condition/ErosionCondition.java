package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

class ErosionCondition extends ThresholdCondition {
	
	public ErosionCondition(MaterialRuleContext context, Noise threshold, Noise variance) {
		super(context, threshold, variance);
	}

	@Override
	protected float sample(Cell cell) {
		return cell.localErosion2;
	}
	
	public record Source(Holder<Noise> threshold, Holder<Noise> variance) implements MaterialCondition {
		public static final Codec<Source> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Noise.CODEC.fieldOf("threshold").forGetter(Source::threshold),
			Noise.CODEC.fieldOf("variance").forGetter(Source::variance)
		).apply(instance, Source::new));

		@Override
		public ErosionCondition compile(MaterialRuleContext ctx) {
			return new ErosionCondition(ctx, this.threshold.value(), this.variance.value());
		}

		@Override
		public MapCodec<Source> codec() {
			return UTFCodecs.asMap(CODEC);
		}
	}
}
