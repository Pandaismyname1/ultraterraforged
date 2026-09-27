package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;

/**
 * Lava tubes: long, round tunnels left where lava drained out from under its own crust, running downhill from
 * volcanoes a few blocks under the ground and lined with basalt and blackstone. Here and there the roof has fallen in,
 * leaving a skylight.
 */
final class LavaTubes implements CaveFeatures.Feature {
	private static final float GRID = 320.0F;
	private static final int STEPS = 24;
	private static final float STEP = 10.0F;
	private static final float RADIUS = 3.2F;
	private static final float HEIGHT = 2.8F;
	private static final Tube NONE = new Tube(new float[0], new float[0], new float[0], new boolean[0], 0.0F, 0.0F, 0.0F, 0.0F);
	private static final BlockState BASALT = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
	private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
	private static final BlockState SMOOTH_BASALT = Blocks.SMOOTH_BASALT.defaultBlockState();

	private final int seed;
	private final float frequency;
	private final SiteCache<Tube> tubes = SiteCache.make();

	record Tube(float[] xs, float[] ys, float[] zs, boolean[] skylights, float minX, float minZ, float maxX, float maxZ) {
	}

	LavaTubes(int seed, float frequency) {
		this.seed = seed;
		this.frequency = frequency;
	}

	@Override
	public void carve(CaveCarving carving) {
		int gridX = NoiseUtil.floor(carving.minX / GRID);
		int gridZ = NoiseUtil.floor(carving.minZ / GRID);
		CaveCarving.Lining lining = (x, y, z) -> {
			int hash = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791);
			int pick = Math.floorMod(hash, 10);
			return pick < 5 ? BASALT : pick < 8 ? BLACKSTONE : SMOOTH_BASALT;
		};
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (CaveFeatures.random(this.seed, gx, gz, 0) >= this.frequency) {
					continue;
				}
				Tube tube = this.tubes.get(gx, gz, (tx, tz) -> this.find(tx, tz, carving));
				if (tube == NONE || !carving.reaches(tube.minX(), tube.minZ(), tube.maxX(), tube.maxZ())) {
					continue;
				}
				CaveFeatures.along(tube.xs(), tube.ys(), tube.zs(), 1.5F, (x, y, z, index) -> carving.line(x, y, z, RADIUS, HEIGHT, RADIUS, 1.2F, lining));
				CaveFeatures.along(tube.xs(), tube.ys(), tube.zs(), 1.5F, (x, y, z, index) -> carving.hollow(x, y, z, RADIUS, HEIGHT, RADIUS, CaveCarving.DRY, 2));
				for (int i = 0; i < tube.xs().length; i++) {
					if (tube.skylights()[i]) {
						float x = tube.xs()[i];
						float z = tube.zs()[i];
						for (float y = tube.ys()[i]; y <= tube.ys()[i] + 12.0F; y += 1.0F) {
							carving.hollow(x, y, z, 2.2F, 1.0F, 2.2F, CaveCarving.DRY, CaveCarving.OPEN);
						}
					}
				}
			}
		}
	}

	Tube find(int gridX, int gridZ, CaveCarving carving) {
		float x = (gridX + CaveFeatures.random(this.seed, gridX, gridZ, 1)) * GRID;
		float z = (gridZ + CaveFeatures.random(this.seed, gridX, gridZ, 2)) * GRID;
		Cell start = carving.cell(x, z);
		if (!start.terrain.isVolcano()) {
			return NONE;
		}
		int sea = carving.levels.waterY;
		// downhill, the way the lava flowed
		float heading = 0.0F;
		int lowest = Integer.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			float angle = i * NoiseUtil.PI2 / 8.0F;
			int ground = carving.terrainHeight(x + NoiseUtil.cos(angle) * 24.0F, z + NoiseUtil.sin(angle) * 24.0F);
			if (ground < lowest) {
				lowest = ground;
				heading = angle;
			}
		}
		float depth = NoiseUtil.lerp(6.0F, 9.0F, CaveFeatures.random(this.seed, gridX, gridZ, 3));
		float[] xs = new float[STEPS];
		float[] ys = new float[STEPS];
		float[] zs = new float[STEPS];
		boolean[] skylights = new boolean[STEPS];
		int count = 0;
		for (int i = 0; i < STEPS; i++) {
			int ground = carving.terrainHeight(x, z);
			// it reached the sea
			if (ground < sea + 3) {
				break;
			}
			xs[i] = x;
			ys[i] = ground - depth;
			zs[i] = z;
			skylights[i] = i % 6 == 3 && CaveFeatures.random(this.seed, gridX, gridZ, 20 + i) < 0.6F;
			count++;
			// the lowest of three ways on
			float best = heading;
			int bestGround = Integer.MAX_VALUE;
			for (float turn : new float[] { -0.4F, 0.0F, 0.4F }) {
				float angle = heading + turn;
				int next = carving.terrainHeight(x + NoiseUtil.cos(angle) * STEP, z + NoiseUtil.sin(angle) * STEP);
				if (next < bestGround) {
					bestGround = next;
					best = angle;
				}
			}
			heading = best;
			x += NoiseUtil.cos(heading) * STEP;
			z += NoiseUtil.sin(heading) * STEP;
		}
		if (count < 5) {
			return NONE;
		}
		float[] keptX = java.util.Arrays.copyOf(xs, count);
		float[] keptY = java.util.Arrays.copyOf(ys, count);
		float[] keptZ = java.util.Arrays.copyOf(zs, count);
		boolean[] keptSky = java.util.Arrays.copyOf(skylights, count);
		// smoothing the tube's height, so it doesn't follow every bump of the ground
		for (int pass = 0; pass < 2; pass++) {
			float[] smooth = keptY.clone();
			for (int i = 1; i < count - 1; i++) {
				smooth[i] = Math.min(keptY[i], (keptY[i - 1] + keptY[i] * 2.0F + keptY[i + 1]) * 0.25F);
			}
			keptY = smooth;
		}
		float reach = RADIUS + 2.0F;
		float minX = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (int i = 0; i < count; i++) {
			minX = Math.min(minX, keptX[i] - reach);
			maxX = Math.max(maxX, keptX[i] + reach);
			minZ = Math.min(minZ, keptZ[i] - reach);
			maxZ = Math.max(maxZ, keptZ[i] + reach);
		}
		return new Tube(keptX, keptY, keptZ, keptSky, minX, minZ, maxX, maxZ);
	}
}
