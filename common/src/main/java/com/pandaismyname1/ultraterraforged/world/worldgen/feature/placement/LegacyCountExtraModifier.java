package com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

@Deprecated
class LegacyCountExtraModifier extends PlacementModifier {
	public static final Codec<LegacyCountExtraModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("count").forGetter((p) -> p.count),
		Codec.FLOAT.fieldOf("extra_chance").forGetter((p) -> p.extraChance),
		Codec.INT.fieldOf("extra_count").forGetter((p) -> p.extraCount)
	).apply(instance, LegacyCountExtraModifier::new));

	private int count;
	private float extraChance;
	private int extraCount;
	
	public LegacyCountExtraModifier(int count, float extraChance, int extraCount) {
		this.count = count;
		this.extraChance = extraChance;
		this.extraCount = extraCount;
	}
	
	@Override
	public Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource random, BlockPos pos) {
	      int i = this.count + (random.nextFloat() < this.extraChance ? this.extraCount : 0);
	      return IntStream.range(0, i).mapToObj((o) -> {
	         return pos;
	      });
	}

	// Generator Accelerator's feature placement on 1.20.1 asks every modifier for this instead of getPositions, and fails
	// for one that doesn't have it (GeneratorAcceleratorCompat): the same positions
	public void generatePositionsFast(PlacementContext ctx, RandomSource random, long packedPos, LongArrayList output) {
		this.getPositions(ctx, random, BlockPos.of(packedPos)).forEach((pos) -> output.add(pos.asLong()));
	}

	@Override
	public PlacementModifierType<LegacyCountExtraModifier> type() {
		return UTFPlacementModifiers.LEGACY_COUNT_EXTRA;
	}
}
