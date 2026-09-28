package com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;

public interface ChanceModifier {
	public static final Codec<ChanceModifier> CODEC = UTFCodecs.dispatch(UTFBuiltInRegistries.CHANCE_MODIFIER_TYPE, ChanceModifier::codec);

	// 26.3 has no FeaturePlaceContext anymore; these are the parts of it the modifiers read
	float getChance(ChanceContext chanceCtx, WorldGenLevel level, BlockPos origin);

	Codec<? extends ChanceModifier> codec();
}
