package com.pandaismyname1.ultraterraforged.world.worldgen.biome.spawn;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;

/**
 * Vanilla's spawn search, started from the spawn type's search center instead of the world origin. It weighs the
 * distance from the world origin as vanilla did before 26.3 (Climate.SpawnFinder), so worlds spawn where they did on
 * earlier versions.
 */
public final class SpawnFinder {
	private Result result;

	private SpawnFinder(List<SpawnTargetPoint> targetPoints, DensitySamplerSet samplers, BlockPos center) {
		this.result = this.getSpawnPositionAndFitness(samplers, targetPoints, center.getX(), center.getZ());
		this.radialSearch(samplers, targetPoints, 2048.0F, 512.0F);
		this.radialSearch(samplers, targetPoints, 512.0F, 32.0F);
	}

	public static BlockPos findSpawnPosition(List<SpawnTargetPoint> targetPoints, DensitySamplerSet samplers, BlockPos center) {
		return new SpawnFinder(targetPoints, samplers, center).result.location();
	}

	private void radialSearch(DensitySamplerSet samplers, List<SpawnTargetPoint> targetPoints, float maxRadius, float radiusIncrement) {
		float angle = 0.0F;
		float radius = radiusIncrement;
		BlockPos searchOrigin = this.result.location();

		while (radius <= maxRadius) {
			int x = searchOrigin.getX() + (int) (Math.sin(angle) * radius);
			int z = searchOrigin.getZ() + (int) (Math.cos(angle) * radius);
			Result candidate = this.getSpawnPositionAndFitness(samplers, targetPoints, x, z);
			if (candidate.fitness() < this.result.fitness()) {
				this.result = candidate;
			}

			angle += radiusIncrement / radius;
			if (angle > Math.PI * 2) {
				angle = 0.0F;
				radius += radiusIncrement;
			}
		}
	}

	private Result getSpawnPositionAndFitness(DensitySamplerSet samplers, List<SpawnTargetPoint> targetPoints, int blockX, int blockZ) {
		int quartBlockX = QuartPos.toBlock(QuartPos.fromBlock(blockX));
		int quartBlockZ = QuartPos.toBlock(QuartPos.fromBlock(blockZ));
		long minFitness = Long.MAX_VALUE;

		for (SpawnTargetPoint point : targetPoints) {
			minFitness = Math.min(minFitness, point.sampleFitness(samplers, quartBlockX, 0, quartBlockZ));
		}

		double distanceBias = Mth.square(10000.0F) * Math.pow((Mth.square((long) blockX) + Mth.square((long) blockZ)) / Mth.square(2500.0), 2.0);
		return new Result(new BlockPos(blockX, 0, blockZ), (long) distanceBias + minFitness);
	}

	private record Result(BlockPos location, long fitness) {
	}
}
