package com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;

public interface ChanceModifier {
	public static final Codec<ChanceModifier> CODEC = UTFCodecs.dispatch(UTFBuiltInRegistries.CHANCE_MODIFIER_TYPE, ChanceModifier::codec);
	
	float getChance(ChanceContext chanceCtx, FeaturePlaceContext<?> placeCtx);
	
	Codec<? extends ChanceModifier> codec();
}
