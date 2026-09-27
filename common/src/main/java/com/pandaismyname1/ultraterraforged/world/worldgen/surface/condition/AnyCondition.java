package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;

// true when any of the conditions is; vanilla only offers "not" and nesting, which is "and"
public record AnyCondition(List<SurfaceRules.ConditionSource> conditions) implements SurfaceRules.ConditionSource {
	public static final Codec<AnyCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		SurfaceRules.ConditionSource.CODEC.listOf().fieldOf("conditions").forGetter(AnyCondition::conditions)
	).apply(instance, AnyCondition::new));

	@Override
	public SurfaceRules.Condition apply(Context ctx) {
		List<SurfaceRules.Condition> conditions = this.conditions.stream().map((condition) -> condition.apply(ctx)).toList();
		return () -> {
			for (SurfaceRules.Condition condition : conditions) {
				if (condition.test()) {
					return true;
				}
			}
			return false;
		};
	}

	@Override
	public KeyDispatchDataCodec<AnyCondition> codec() {
		return new KeyDispatchDataCodec<>(CODEC);
	}
}
