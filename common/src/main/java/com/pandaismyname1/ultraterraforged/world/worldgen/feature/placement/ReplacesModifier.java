package com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

// Places as if it weren't there. It tells apart the copies of a feature that replaces several vanilla ones (see
// BiomeModifiers.distinctReplacements): up to 26.2 vanilla ordered a biome's features by their content, so each copy
// names the feature it stands in for. A record, so two copies are equal only if they name the same feature.
public record ReplacesModifier(ResourceKey<PlacedFeature> feature) implements PlacementModifier {
	public static final MapCodec<ReplacesModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceKey.codec(Registries.PLACED_FEATURE).fieldOf("feature").forGetter(ReplacesModifier::feature)
	).apply(instance, ReplacesModifier::new));

	@Override
	public void modify(PlacementContext ctx, RandomSource random, BlockPos pos, Consumer<BlockPos> output) {
		output.accept(pos);
	}

	@Override
	public MapCodec<ReplacesModifier> codec() {
		return CODEC;
	}
}
