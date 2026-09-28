package com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement;

import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

// Places as if it weren't there. It tells apart the copies of a feature that replaces several vanilla ones (see
// BiomeModifiers.distinctReplacements): vanilla orders a biome's features by their content, so each copy names the
// feature it stands in for.
public final class ReplacesModifier extends PlacementModifier {
	public static final Codec<ReplacesModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ResourceKey.codec(Registries.PLACED_FEATURE).fieldOf("feature").forGetter((modifier) -> modifier.feature)
	).apply(instance, ReplacesModifier::new));

	private final ResourceKey<PlacedFeature> feature;

	public ReplacesModifier(ResourceKey<PlacedFeature> feature) {
		this.feature = feature;
	}

	@Override
	public Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource random, BlockPos pos) {
		return Stream.of(pos);
	}

	@Override
	public PlacementModifierType<?> type() {
		return UTFPlacementModifiers.REPLACES;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ReplacesModifier replaces && replaces.feature.equals(this.feature);
	}

	@Override
	public int hashCode() {
		return this.feature.hashCode();
	}
}
