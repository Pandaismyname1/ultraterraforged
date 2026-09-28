package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Springs: water welling out of a small cave in a hillside and running away down the slope. Each fits inside its own
 * chunk, so it's carved whole.
 */
final class Springs implements CaveFeatures.Feature {
	private static final int[][] DIRECTIONS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
	// the hill drops at least this many blocks over REACH blocks away from the spring
	private static final int DROP = 4;
	private static final int REACH = 5;
	// how far back into the hill the water comes from
	private static final int DEPTH = 5;

	private final int seed;
	private final float frequency;

	Springs(int seed, float frequency) {
		this.seed = seed;
		this.frequency = frequency;
	}

	@Override
	public void carve(CaveCarving carving) {
		int cx = carving.chunkX;
		int cz = carving.chunkZ;
		if (CaveFeatures.random(this.seed, cx, cz, 0) >= this.frequency * 0.3F) {
			return;
		}
		int x = carving.minX + 5 + (int) (CaveFeatures.random(this.seed, cx, cz, 1) * 6.0F);
		int z = carving.minZ + 5 + (int) (CaveFeatures.random(this.seed, cx, cz, 2) * 6.0F);
		int ground = carving.ground(x, z);
		if (ground < carving.levels.waterY + 4) {
			return;
		}
		// the way down the hill
		int[] best = null;
		int bestDrop = DROP - 1;
		for (int[] direction : DIRECTIONS) {
			int drop = ground - carving.ground(x + direction[0] * REACH, z + direction[1] * REACH);
			if (drop > bestDrop) {
				bestDrop = drop;
				best = direction;
			}
		}
		if (best == null) {
			return;
		}
		int y = ground - 2;
		// a small tunnel from the back of the spring out to where the hillside falls away below it
		for (int step = -DEPTH; step <= REACH; step++) {
			int px = x + best[0] * step;
			int pz = z + best[1] * step;
			if (!carving.contains(px, pz)) {
				break;
			}
			if (step > 0 && carving.ground(px, pz) < y) {
				break;
			}
			carving.clear(px, y, pz, CaveCarving.DRY);
			carving.clear(px, y + 1, pz, CaveCarving.DRY);
		}
		int bx = x - best[0] * DEPTH;
		int bz = z - best[1] * DEPTH;
		BlockState below = carving.get(bx, y - 1, bz);
		if (below.isAir() || !below.getFluidState().isEmpty()) {
			return;
		}
		carving.set(bx, y, bz, Blocks.WATER.defaultBlockState());
		// it flows once the world ticks it
		carving.chunk.markPosForPostProcessing(new BlockPos(bx, y, bz));
	}
}
