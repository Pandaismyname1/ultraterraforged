package com.pandaismyname1.ultraterraforged.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.RandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;

// A biome search (locate biome, biome compasses, the rings of strongholds) samples the climate over a wide area where no
// tile is made yet: it reads the heightmap's cells, quick and the same on every thread, instead of making a tile for
// every few samples (WorldGenFlags)
@Mixin(BiomeSource.class)
class MixinBiomeSource {

	@WrapMethod(method = "findClosestBiome3d")
	private Pair<BlockPos, Holder<Biome>> ultraterraforged$findClosestBiome3d(BlockPos origin, int searchRadius, int sampleResolutionHorizontal, int sampleResolutionVertical, Predicate<Holder<Biome>> allowed, RandomState randomState, LevelReader level, Operation<Pair<BlockPos, Holder<Biome>>> original) {
		return WorldGenFlags.approximately(() -> original.call(origin, searchRadius, sampleResolutionHorizontal, sampleResolutionVertical, allowed, randomState, level));
	}

	@WrapMethod(method = "findBiomeHorizontal(IIIIILjava/util/function/Predicate;Lnet/minecraft/util/RandomSource;ZLnet/minecraft/world/level/levelgen/RandomState;)Lcom/mojang/datafixers/util/Pair;")
	private Pair<BlockPos, Holder<Biome>> ultraterraforged$findBiomeHorizontal(int x, int y, int z, int searchRadius, int skipSteps, Predicate<Holder<Biome>> allowed, RandomSource random, boolean findClosest, RandomState randomState, Operation<Pair<BlockPos, Holder<Biome>>> original) {
		return WorldGenFlags.approximately(() -> original.call(x, y, z, searchRadius, skipSteps, allowed, random, findClosest, randomState));
	}
}
