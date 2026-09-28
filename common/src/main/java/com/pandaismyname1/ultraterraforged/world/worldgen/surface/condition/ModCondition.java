package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import net.minecraft.world.level.levelgen.material.condition.ConditionEvaluator;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;

record ModCondition(String modId) implements MaterialCondition {
	public static final Codec<ModCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("mod_id").forGetter(ModCondition::modId)
	).apply(instance, ModCondition::new));
	
	@Override
	public ConditionEvaluator compile(MaterialRuleContext t) {
		boolean isModLoaded = ModLoaderUtil.isLoaded(this.modId);
		return () -> isModLoaded;
	}

	@Override
	public MapCodec<ModCondition> codec() {
		return UTFCodecs.asMap(CODEC);
	}
}
