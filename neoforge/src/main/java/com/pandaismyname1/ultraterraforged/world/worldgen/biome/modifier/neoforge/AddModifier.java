package com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeGenerationSettingsBuilder;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.Filter;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.Order;

record AddModifier(Order order, GenerationStep.Decoration step, Optional<Filter> biomes, HolderSet<PlacedFeature> features) implements NeoForgeBiomeModifier {
	public static final Codec<AddModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Order.CODEC.fieldOf("order").forGetter(AddModifier::order),
		GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(AddModifier::step),
		Filter.CODEC.optionalFieldOf("biomes").forGetter(AddModifier::biomes),
		PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(AddModifier::features)
	).apply(instance, AddModifier::new));

	@Override
	public void modify(Holder<Biome> biome, BiomeGenerationSettingsBuilder generationSettings) {
		if(this.biomes.isPresent() && !this.biomes.get().test(biome)) {
			return;
		}
		List<Holder<PlacedFeature>> step = generationSettings.getFeatures(this.step);
		List<Holder<PlacedFeature>> added = this.order.add(new ArrayList<>(step), this.features.stream().toList());
		step.clear();
		step.addAll(added);
	}

	@Override
	public Codec<AddModifier> codec() {
		return CODEC;
	}
}
