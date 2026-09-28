package com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.MapMaker;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement.ReplacesModifier;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;

public class BiomeModifiers {
	private static final Map<Object, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>>> DISTINCT_REPLACEMENTS = new MapMaker().weakKeys().makeMap();

	/**
	 * A biome's features must run in the same order in every biome that has them. A modifier putting one feature in place
	 * of two vanilla ones (the trees of plains and of rivers, say) breaks that when vanilla orders those two differently
	 * around a third, as 26.1 does with its bushes, and vanilla then refuses to generate. So a replacement used for more
	 * than one feature is copied once per replaced feature, and each copy takes that feature's place everywhere: the order
	 * stays vanilla's. Up to 26.2 vanilla told features apart by their content, so each copy's placement starts with a
	 * ReplacesModifier naming the feature it stands in for. 26.3's FeatureSorter tells them apart by identity instead, which
	 * the copies being separate objects already covers; the modifier stays so they differ by content too (hasFeature, sets).
	 * The copies are made once per modifier, so every biome gets the same ones, which identity needs.
	 */
	public static Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> distinctReplacements(Object modifier, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		return DISTINCT_REPLACEMENTS.computeIfAbsent(modifier, (m) -> {
			Map<PlacedFeature, Integer> uses = new IdentityHashMap<>();
			replacements.values().forEach((holder) -> uses.merge(holder.value(), 1, Integer::sum));
			Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> distinct = new HashMap<>();
			replacements.forEach((key, holder) -> {
				PlacedFeature feature = holder.value();
				if (uses.get(feature) > 1) {
					List<PlacementModifier> placement = new ArrayList<>();
					placement.add(new ReplacesModifier(key));
					placement.addAll(feature.placement());
					distinct.put(key, Holder.direct(new PlacedFeature(feature.feature(), placement)));
				} else {
					distinct.put(key, holder);
				}
			});
			return distinct;
		});
	}


	@ExpectPlatform
	public static void bootstrap() {
		throw new UnsupportedOperationException();
	}

	@SafeVarargs
	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Holder<PlacedFeature>... features) {
		return add(order, step, HolderSet.direct(features));
	}
	
	public static BiomeModifier add(Order order, GenerationStep.Decoration step, HolderSet<PlacedFeature> features) {
		return add(order, step, Optional.empty(), features);
	}

	@SafeVarargs
	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Filter.Behavior filterBehavior, HolderSet<Biome> biomes, Holder<PlacedFeature>... features) {
		return add(order, step, filterBehavior, biomes, HolderSet.direct(features));
	}

	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Filter.Behavior filterBehavior, HolderSet<Biome> biomes, HolderSet<PlacedFeature> features) {
		return add(order, step, Optional.of(Pair.of(filterBehavior, biomes)), features);
	}
	
	@ExpectPlatform
	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Optional<Pair<Filter.Behavior, HolderSet<Biome>>> biomes, HolderSet<PlacedFeature> features) {
		throw new UnsupportedOperationException();
	}

	public static BiomeModifier replace(GenerationStep.Decoration step, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		return replace(step, Optional.empty(), replacements);
	}

	public static BiomeModifier replace(GenerationStep.Decoration step, HolderSet<Biome> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		return replace(step, Optional.of(biomes), replacements);
	}
	
	@ExpectPlatform
	public static BiomeModifier replace(GenerationStep.Decoration step, Optional<HolderSet<Biome>> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		throw new UnsupportedOperationException();
	}
	
	public static void register(String name, Codec<? extends BiomeModifier> value) {
		RegistryUtil.register(UTFBuiltInRegistries.BIOME_MODIFIER_TYPE, name, value);
	}
}
