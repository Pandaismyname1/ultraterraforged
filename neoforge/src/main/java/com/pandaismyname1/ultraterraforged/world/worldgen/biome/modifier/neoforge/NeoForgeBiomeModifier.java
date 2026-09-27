package com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.neoforge;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeGenerationSettingsBuilder;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier;

// a biome modifier from a preset, applied by PresetBiomeModifier
interface NeoForgeBiomeModifier extends BiomeModifier {
	void modify(Holder<Biome> biome, BiomeGenerationSettingsBuilder generationSettings);
}
