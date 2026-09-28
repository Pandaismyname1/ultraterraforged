package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.condition.ConditionEvaluator;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;

// true when any of the conditions is; vanilla only offers "not" and nesting, which is "and"
public record AnyCondition(List<MaterialCondition> conditions) implements MaterialCondition {
	public static final Codec<AnyCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		MaterialCondition.CODEC.listOf().fieldOf("conditions").forGetter(AnyCondition::conditions)
	).apply(instance, AnyCondition::new));

	@Override
	public ConditionEvaluator compile(MaterialRuleContext ctx) {
		ConditionEvaluator[] conditions = this.conditions.stream().map((condition) -> condition.compile(ctx)).toArray(ConditionEvaluator[]::new);
		return () -> {
			for (ConditionEvaluator condition : conditions) {
				if (condition.test()) {
					return true;
				}
			}
			return false;
		};
	}

	@Override
	public MapCodec<AnyCondition> codec() {
		return UTFCodecs.asMap(CODEC);
	}
}
