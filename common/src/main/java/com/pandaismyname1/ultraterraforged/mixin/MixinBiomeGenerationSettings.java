package com.pandaismyname1.ultraterraforged.mixin;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

//TODO do this with access wideners instead
@Deprecated
@Mixin(BiomeGenerationSettings.class)
public interface MixinBiomeGenerationSettings {
	@Accessor
	List<HolderSet<PlacedFeature>> getFeatures();

	// what vanilla works out once from the features, and has to be worked out again when they change; since 26.3 a
	// configured feature is a Feature
	@Mutable
	@Accessor
	void setBoneMealFeatures(Supplier<List<Feature>> boneMealFeatures);

	@Mutable
	@Accessor
	void setFeatureSet(Supplier<Set<PlacedFeature>> featureSet);
}
