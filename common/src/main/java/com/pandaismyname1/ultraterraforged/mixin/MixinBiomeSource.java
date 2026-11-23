package com.pandaismyname1.ultraterraforged.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;

// A biome search (locate biome, biome compasses, the rings of strongholds) samples the climate over a wide area where no
// tile is made yet: it reads the heightmap's cells, quick and the same on every thread, instead of making a tile for
// every few samples (WorldGenFlags)
@Mixin(BiomeSource.class)
class MixinBiomeSource {
	private static final String FIND_BIOME_HORIZONTAL = "findBiomeHorizontal(IIIIILjava/util/function/Predicate;Lnet/minecraft/util/RandomSource;ZLnet/minecraft/world/level/biome/Climate$Sampler;)Lcom/mojang/datafixers/util/Pair;";

	@Inject(method = "findClosestBiome3d", at = @At("HEAD"))
	private void ultraterraforged$findClosestBiome3dHead(BlockPos origin, int searchRadius, int sampleResolutionHorizontal, int sampleResolutionVertical, Predicate<Holder<Biome>> allowed, Climate.Sampler sampler, LevelReader level, CallbackInfoReturnable<Pair<BlockPos, Holder<Biome>>> callback) {
		WorldGenFlags.enterApproximate();
	}

	@Inject(method = "findClosestBiome3d", at = @At("RETURN"))
	private void ultraterraforged$findClosestBiome3dReturn(BlockPos origin, int searchRadius, int sampleResolutionHorizontal, int sampleResolutionVertical, Predicate<Holder<Biome>> allowed, Climate.Sampler sampler, LevelReader level, CallbackInfoReturnable<Pair<BlockPos, Holder<Biome>>> callback) {
		WorldGenFlags.leaveApproximate();
	}

	@Inject(method = FIND_BIOME_HORIZONTAL, at = @At("HEAD"))
	private void ultraterraforged$findBiomeHorizontalHead(int x, int y, int z, int searchRadius, int skipSteps, Predicate<Holder<Biome>> allowed, RandomSource random, boolean findClosest, Climate.Sampler sampler, CallbackInfoReturnable<Pair<BlockPos, Holder<Biome>>> callback) {
		WorldGenFlags.enterApproximate();
	}

	@Inject(method = FIND_BIOME_HORIZONTAL, at = @At("RETURN"))
	private void ultraterraforged$findBiomeHorizontalReturn(int x, int y, int z, int searchRadius, int skipSteps, Predicate<Holder<Biome>> allowed, RandomSource random, boolean findClosest, Climate.Sampler sampler, CallbackInfoReturnable<Pair<BlockPos, Holder<Biome>>> callback) {
		WorldGenFlags.leaveApproximate();
	}
}
