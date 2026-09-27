package com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import com.pandaismyname1.ultraterraforged.registries.RTFBuiltInRegistries;

public interface BiomeModifier {
    public static final Codec<BiomeModifier> CODEC = RTFBuiltInRegistries.BIOME_MODIFIER_TYPE.byNameCodec().dispatch(BiomeModifier::codec, Function.identity());
    
	Codec<? extends BiomeModifier> codec();
}
