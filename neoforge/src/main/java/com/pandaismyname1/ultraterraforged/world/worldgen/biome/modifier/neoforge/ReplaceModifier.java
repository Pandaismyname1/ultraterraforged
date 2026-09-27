package com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.neoforge;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeGenerationSettingsBuilder;

record ReplaceModifier(GenerationStep.Decoration step, Optional<HolderSet<Biome>> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) implements NeoForgeBiomeModifier {
	public static final Codec<ReplaceModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(ReplaceModifier::step),
		Biome.LIST_CODEC.optionalFieldOf("biomes").forGetter(ReplaceModifier::biomes),
		Codec.unboundedMap(ResourceKey.codec(Registries.PLACED_FEATURE), PlacedFeature.CODEC).fieldOf("replacements").forGetter(ReplaceModifier::replacements)
	).apply(instance, ReplaceModifier::new));

	@Override
	public void modify(Holder<Biome> biome, BiomeGenerationSettingsBuilder generationSettings) {
		if(this.biomes.isPresent() && !this.biomes.get().contains(biome)) {
			return;
		}
		List<Holder<PlacedFeature>> step = generationSettings.getFeatures(this.step);
		step.replaceAll((f) -> {
			Holder<PlacedFeature> replacement = f.unwrapKey().map(this.replacements::get).orElse(null);
			return replacement != null ? replacement : f;
		});
	}

	@Override
	public Codec<ReplaceModifier> codec() {
		return CODEC;
	}
}
