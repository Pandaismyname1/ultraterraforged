package com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

class NoiseFilter implements PlacementFilter {
	public static final MapCodec<NoiseFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Noise.CODEC.fieldOf("noise").forGetter((filter) -> filter.noise),
		Codec.FLOAT.fieldOf("threshold").forGetter((filter) -> filter.threshold)
	).apply(instance, NoiseFilter::new));

	private Holder<Noise> noise;
	private float threshold;
	
	public NoiseFilter(Holder<Noise> noise, float threshold) {
		this.noise = noise;
		this.threshold = threshold;
	}
	
	@Override
	public boolean shouldPlace(PlacementContext ctx, RandomSource rand, BlockPos pos) {
		return this.noise.value().compute(pos.getX(), pos.getZ(), (int) ctx.getLevel().getSeed()) > this.threshold;
	}
	
	@Override
	public MapCodec<NoiseFilter> codec() {
		return CODEC;
	}
}
