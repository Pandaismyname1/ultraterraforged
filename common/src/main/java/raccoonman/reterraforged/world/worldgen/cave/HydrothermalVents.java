package raccoonman.reterraforged.world.worldgen.cave;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Hydrothermal vents: chimneys of dark rock standing in the rifts of mid-ocean ridges, with magma glowing at their
 * mouths and scattered over the floor around them, dragging the water down in streams of bubbles. Each stands in one
 * column of its chunk.
 */
final class HydrothermalVents implements CaveFeatures.Feature {
	// the chance of a vent in each column of a rift
	private static final float CHANCE = 1.0F / 160.0F;
	private static final BlockState BASALT = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
	private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
	private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();

	private final int seed;

	HydrothermalVents(int seed) {
		this.seed = seed;
	}

	@Override
	public void carve(CaveCarving carving) {
		int sea = carving.levels.waterY;
		for (int dz = 1; dz < 15; dz++) {
			for (int dx = 1; dx < 15; dx++) {
				int x = carving.minX + dx;
				int z = carving.minZ + dz;
				if (CaveFeatures.random(this.seed, x, z, 0) >= CHANCE) {
					continue;
				}
				Cell cell = carving.tileCell(dx, dz);
				if (cell.terrain != TerrainType.OCEAN_RIDGE) {
					continue;
				}
				int floor = carving.ground(x, z);
				int height = 3 + (int) (CaveFeatures.random(this.seed, x, z, 1) * 6.0F);
				if (floor + height + 2 >= sea) {
					continue;
				}
				for (int y = floor + 1; y < floor + height; y++) {
					carving.set(x, y, z, (y & 1) == 0 ? BASALT : BLACKSTONE);
				}
				carving.set(x, floor + height, z, MAGMA);
				// magma scattered on the floor around it
				for (int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
					if (CaveFeatures.random(this.seed, x + step[0], z + step[1], 2) < 0.5F) {
						int around = carving.ground(x + step[0], z + step[1]);
						carving.set(x + step[0], around, z + step[1], MAGMA);
					}
				}
			}
		}
	}
}
