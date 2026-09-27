package com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.neoforge;

import com.mojang.serialization.Codec;

import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier;

public interface ForgeBiomeModifier extends BiomeModifier, net.minecraftforge.common.world.BiomeModifier {
	Codec<? extends ForgeBiomeModifier> codec();
}
