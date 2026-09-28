package com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

record NoiseRule(Holder<Noise> noise, List<Pair<Float, MaterialRule>> rules) implements MaterialRule {
	public static final Codec<NoiseRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Noise.CODEC.fieldOf("noise").forGetter(NoiseRule::noise),
		entryCodec().listOf().fieldOf("rules").forGetter(NoiseRule::rules)
	).apply(instance, NoiseRule::new));

	@Override
	public Rule compile(MaterialRuleContext ctx) {
		return new Rule(this.noise.value(), this.rules.stream().map((pair) -> {
			return Pair.of(pair.getFirst(), pair.getSecond().compile(ctx));
		}).sorted((p1, p2) -> p2.getFirst().compareTo(p1.getFirst())).toList());
	}

	@Override
	public MapCodec<NoiseRule> codec() {
		return UTFCodecs.asMap(CODEC);
	}

	private static Codec<Pair<Float, MaterialRule>> entryCodec() {
		return RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.fieldOf("threshold").forGetter(Pair::getFirst),
			MaterialRule.CODEC.fieldOf("rule").forGetter(Pair::getSecond)
		).apply(instance, Pair::new));
	}

	private static class Rule implements RuleEvaluator {
		private Noise noise;
		private List<Pair<Float, RuleEvaluator>> rules;
		private long lastPos;
		private RuleEvaluator rule;

		public Rule(Noise noise, List<Pair<Float, RuleEvaluator>> rules) {
			this.noise = noise;
			this.rules = rules;
			this.lastPos = Long.MIN_VALUE;
		}

		@Override
		public BlockState tryApply(int x, int y, int z) {
			long pos = PosUtil.pack(x, z);
			if(this.lastPos != pos) {
				float noise = this.noise.compute(x, z, 0);
				RuleEvaluator newRule = null;
				for(Pair<Float, RuleEvaluator> entry : this.rules) {
					if(noise > entry.getFirst()) {
						newRule = entry.getSecond();
						break;
					}
				}
				this.lastPos = pos;
				this.rule = newRule;
			}
			return this.rule != null ? this.rule.tryApply(x, y, z) : null;
		}
	}
}
