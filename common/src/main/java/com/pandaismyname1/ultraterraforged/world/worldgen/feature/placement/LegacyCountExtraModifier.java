package com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

@Deprecated
class LegacyCountExtraModifier implements PlacementModifier {
	public static final MapCodec<LegacyCountExtraModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
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
	public void modify(PlacementContext ctx, RandomSource random, BlockPos pos, Consumer<BlockPos> output) {
	      int i = this.count + (random.nextFloat() < this.extraChance ? this.extraCount : 0);
	      for (int n = 0; n < i; n++) {
	    	  output.accept(pos);
	      }
	}

	@Override
	public MapCodec<LegacyCountExtraModifier> codec() {
		return CODEC;
	}
}
