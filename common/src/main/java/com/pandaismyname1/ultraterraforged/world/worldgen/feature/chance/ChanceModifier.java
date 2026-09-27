package com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;

public interface ChanceModifier {
	public static final Codec<ChanceModifier> CODEC = UTFBuiltInRegistries.CHANCE_MODIFIER_TYPE.byNameCodec().dispatch(ChanceModifier::codec, Function.identity());
	
	float getChance(ChanceContext chanceCtx, FeaturePlaceContext<?> placeCtx);
	
	Codec<? extends ChanceModifier> codec();
}
