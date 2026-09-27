package com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;

import com.mojang.serialization.Codec;

import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;

public interface BiomeModifier {
    public static final Codec<BiomeModifier> CODEC = UTFCodecs.dispatch(UTFBuiltInRegistries.BIOME_MODIFIER_TYPE, BiomeModifier::codec);
    
	Codec<? extends BiomeModifier> codec();
}
