package com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.neoforge;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;

// The one NeoForge biome modifier UTF ships (data/ultraterraforged/neoforge/biome_modifier/preset.json): applies the
// modifiers of the world's preset, from UTF's own registry, which is empty in worlds without a preset. NeoForge runs
// biome modifiers as the server starts, once it's the current server.
public final class PresetBiomeModifier implements BiomeModifier {
	public static final PresetBiomeModifier INSTANCE = new PresetBiomeModifier();
	public static final MapCodec<PresetBiomeModifier> CODEC = MapCodec.unit(INSTANCE);
	private static MinecraftServer reported;

	private PresetBiomeModifier() {
	}

	@Override
	public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (phase != Phase.AFTER_EVERYTHING || server == null) {
			return;
		}
		server.registryAccess().registry(UTFRegistries.BIOME_MODIFIER).ifPresent((modifiers) -> {
			if (reported != server) {
				reported = server;
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
