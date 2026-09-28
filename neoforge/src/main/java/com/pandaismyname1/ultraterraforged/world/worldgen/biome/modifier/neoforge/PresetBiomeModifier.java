package com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.neoforge;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;

// The one NeoForge biome modifier UTF ships (data/ultraterraforged/neoforge/biome_modifier/preset.json): applies the
// modifiers of the world's preset, from UTF's own registry, which is empty in worlds without a preset. NeoForge runs
// biome modifiers as the server starts, and since 26.3 hands them the server's registries.
public final class PresetBiomeModifier implements BiomeModifier {
	public static final PresetBiomeModifier INSTANCE = new PresetBiomeModifier();
	public static final MapCodec<PresetBiomeModifier> CODEC = MapCodec.unit(INSTANCE);
	private static RegistryAccess reported;

	private PresetBiomeModifier() {
	}

	@Override
	public void modify(RegistryAccess registries, Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		if (phase != Phase.AFTER_EVERYTHING) {
			return;
		}
		registries.lookup(UTFRegistries.BIOME_MODIFIER).ifPresent((modifiers) -> {
			if (reported != registries) {
				reported = registries;
				UTFCommon.LOGGER.info("Applying the {} biome modifiers of the world's preset", modifiers.size());
			}
			for (com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier modifier : modifiers) {
				if (modifier instanceof NeoForgeBiomeModifier neoForgeModifier) {
					neoForgeModifier.modify(biome, builder.getGenerationSettings());
				}
			}
		});
	}

	@Override
	public MapCodec<PresetBiomeModifier> codec() {
		return CODEC;
	}
}
