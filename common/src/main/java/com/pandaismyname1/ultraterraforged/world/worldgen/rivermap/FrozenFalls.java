package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;

/**
 * Frozen waterfalls. Where a river above the sea steps down, RaisedWater leaves the water at the step to flow over it,
 * but where it's cold enough, vanilla freezes the top of the water first: the step stays a wall of standing water under
 * a lid of ice. Instead, the fall freezes: a curtain of ice from the frozen river above down to the water below.
 * <p>
 * Runs once a chunk's features are placed, freezing included. Whether a river freezes is decided as vanilla decides
 * it, by how cold its biome is, not by whether the chunk next door has frozen yet, so both sides of a chunk border
 * agree; each chunk only places the ice in its own columns.
 */
public final class FrozenFalls {
	private static final int[][] SIDES = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
	// how far a fall may drop; below that it's left alone, e.g. a river over a cliff into a cave
	private static final int MAX_DROP = 32;

	private FrozenFalls() {
	}

	public static void apply(ChunkAccess chunk, List<ChunkAccess> region, GeneratorContext context) {
		ChunkPos chunkPos = chunk.getPos();
		int minX = chunkPos.getMinBlockX();
		int minZ = chunkPos.getMinBlockZ();
		Levels levels = context.levels;
		// the surface of the raised water in this chunk and the ring of columns around it, or NONE
		int[] surfaces = new int[18 * 18];
		boolean any = false;
		Cell cell = new Cell();
		for (int dz = -1; dz <= 16; dz++) {
			for (int dx = -1; dx <= 16; dx++) {
				context.lookup.apply(cell.reset(), minX + dx, minZ + dz);
				int surface = cell.waterLevel > 0.0F ? levels.scale(cell.waterLevel) : Integer.MIN_VALUE;
				surfaces[(dz + 1) * 18 + dx + 1] = surface > levels.waterY ? surface : Integer.MIN_VALUE;
				any |= surface > levels.waterY;
			}
		}
		if (!any) {
			return;
		}

		BlockState ice = Blocks.ICE.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos from = new BlockPos.MutableBlockPos();
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				int x = minX + dx;
				int z = minZ + dz;
				// the highest frozen river beside this column that falls into it
				int top = Integer.MIN_VALUE;
				for (int[] side : SIDES) {
					int surface = surfaces[(dz + 1 + side[1]) * 18 + dx + 1 + side[0]];
					if (surface <= top) {
						continue;
					}
					ChunkAccess source = chunkAt(chunk, region, x + side[0], z + side[1]);
					if (source == null) {
						continue;
					}
					BlockState water = source.getBlockState(from.set(x + side[0], surface, z + side[1]));
					boolean frozen = water.is(Blocks.ICE) || (water.is(Blocks.WATER) && !source.getNoiseBiome(QuartPos.fromBlock(from.getX()), QuartPos.fromBlock(surface), QuartPos.fromBlock(from.getZ())).value().warmEnoughToRain(from));
					// open to the fall: this column is air just below the river's surface
					if (frozen && chunk.getBlockState(pos.set(x, surface - 1, z)).isAir()) {
						top = surface;
					}
				}
				if (top == Integer.MIN_VALUE) {
					continue;
				}
				// the curtain, down to whatever is below
				int bottom = top - 1;
				while (bottom > top - MAX_DROP && chunk.getBlockState(pos.set(x, bottom - 1, z)).isAir()) {
					bottom--;
				}
				if (bottom <= top - MAX_DROP) {
					continue;
				}
				for (int y = bottom; y < top; y++) {
					chunk.setBlockState(pos.set(x, y, z), ice, false);
				}
			}
		}
	}

	@Nullable
	private static ChunkAccess chunkAt(ChunkAccess center, List<ChunkAccess> region, int blockX, int blockZ) {
		int chunkX = blockX >> 4;
		int chunkZ = blockZ >> 4;
		ChunkPos pos = center.getPos();
		if (chunkX == pos.x && chunkZ == pos.z) {
			return center;
		}
		for (ChunkAccess chunk : region) {
			ChunkPos other = chunk.getPos();
			if (other.x == chunkX && other.z == chunkZ) {
				return chunk;
			}
		}
		return null;
	}
}
