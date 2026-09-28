package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

/**
 * Puts the water of rivers, lakes and wetlands above the sea into a chunk once its terrain is filled in, before the
 * surface is built on it: the noise only fills water up to sea level. Where the water steps down to a lower stretch
 * of river, or might meet another chunk's, it's left to flow.
 */
public final class RaisedWater {
	// water is filled at most this deep below its surface; anything deeper is a cave under the river bed
	private static final int MAX_DEPTH = 24;
	private static final int NONE = Integer.MIN_VALUE;

	private RaisedWater() {
	}

	public static void fill(ChunkAccess chunk, GeneratorContext context) {
		ChunkPos chunkPos = chunk.getPos();
		Tile.Chunk cells = context.cache.provideAtChunk(chunkPos.x(), chunkPos.z()).getChunkReader(chunkPos.x(), chunkPos.z());
		Levels levels = context.levels;
		int[] surfaces = new int[256];
		boolean any = false;
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				Cell cell = cells.getCell(dx, dz);
				int surface = cell.waterLevel > 0.0F ? levels.scale(cell.waterLevel) : NONE;
				// the noise has filled the sea already
				if (surface <= levels.waterY) {
					surface = NONE;
				}
				surfaces[dz << 4 | dx] = surface;
				any |= surface != NONE;
			}
		}
		if (!any) {
			return;
		}
		int minX = chunkPos.getMinBlockX();
		int minZ = chunkPos.getMinBlockZ();
		int bottom = chunk.getMinY();
		BlockState water = Blocks.WATER.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		// the top water block of each column, or NONE
		int[] tops = new int[256];
		for (int i = 0; i < 256; i++) {
			tops[i] = NONE;
			int surface = surfaces[i];
			if (surface == NONE) {
				continue;
			}
			int x = minX + (i & 15);
			int z = minZ + (i >> 4);
			for (int y = surface; y >= Math.max(bottom, surface - MAX_DEPTH); y--) {
				BlockState state = chunk.getBlockState(pos.set(x, y, z));
				if (state.isAir()) {
					chunk.setBlockState(pos, water, 0);
					tops[i] = Math.max(tops[i], y);
				} else if (state.getFluidState().isEmpty()) {
					break;
				}
			}
		}
		// water that can spill: beside a column with less or no water, or at the edge of the chunk
		for (int i = 0; i < 256; i++) {
			int top = tops[i];
			if (top == NONE) {
				continue;
			}
			int dx = i & 15;
			int dz = i >> 4;
			boolean spills = dx == 0 || dz == 0 || dx == 15 || dz == 15
				|| tops[i - 1] < top || tops[i + 1] < top || tops[i - 16] < top || tops[i + 16] < top;
			if (spills) {
				chunk.markPosForPostprocessing(pos.set(minX + dx, top, minZ + dz));
			}
		}
	}
}
