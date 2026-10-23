package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;

/**
 * Keeps carvers out of the water of rivers and lakes above the sea. RaisedWater puts that water in before the surface is
 * built, so before the carvers run, and a carver fills what it carves with what the aquifer says, which above the sea is
 * air: a cave crossing a lake left a cave-shaped hole of air in its water (issue #9), and one passing just under its bed
 * or beside its bank left the water standing against air. The carvers get the chunk's aquifer through {@link #wrap},
 * which asks this before turning a block into air, and the block stays if it is water or has water right above it or
 * beside it.
 */
public final class RaisedWaterGuard {
	private static final int NONE = Integer.MIN_VALUE;

	private final ChunkAccess chunk;
	private final GeneratorContext context;
	private final int minX;
	private final int minZ;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	// for the ring of columns around the chunk (18 by 18, the chunk's own unused), the ground under its raised water and
	// the water's surface, or NONE; the neighbouring chunks may not have their water yet
	@Nullable
	private int[] beside;

	public RaisedWaterGuard(ChunkAccess chunk, GeneratorContext context) {
		ChunkPos chunkPos = chunk.getPos();
		this.chunk = chunk;
		this.context = context;
		this.minX = chunkPos.getMinBlockX();
		this.minZ = chunkPos.getMinBlockZ();
	}

	// the chunk's aquifer, as the carvers see it
	public Aquifer wrap(Aquifer aquifer) {
		return new Aquifer() {

			@Override
			@Nullable
			public BlockState computeSubstance(DensityFunction.FunctionContext context, double density) {
				BlockState state = aquifer.computeSubstance(context, density);
				if (state != null && state.getFluidState().isEmpty() && RaisedWaterGuard.this.keeps(context.blockX(), context.blockY(), context.blockZ())) {
					return null;
				}
				return state;
			}

			@Override
			public boolean shouldScheduleFluidUpdate() {
				return aquifer.shouldScheduleFluidUpdate();
			}
		};
	}

	public boolean keeps(int x, int y, int z) {
		return this.water(x, y, z) || this.water(x, y + 1, z)
			|| this.water(x - 1, y, z) || this.water(x + 1, y, z) || this.water(x, y, z - 1) || this.water(x, y, z + 1);
	}

	private boolean water(int x, int y, int z) {
		int dx = x - this.minX;
		int dz = z - this.minZ;
		if (dx >= 0 && dx < 16 && dz >= 0 && dz < 16) {
			return !this.chunk.getBlockState(this.pos.set(x, y, z)).getFluidState().isEmpty();
		}
		int[] beside = this.beside();
		int i = ((dz + 1) * 18 + dx + 1) * 2;
		return y > beside[i] && y <= beside[i + 1];
	}

	private int[] beside() {
		if (this.beside == null) {
			Levels levels = this.context.levels;
			Cell cell = new Cell();
			int[] beside = new int[18 * 18 * 2];
			for (int dz = -1; dz <= 16; dz++) {
				for (int dx = -1; dx <= 16; dx++) {
					int i = ((dz + 1) * 18 + dx + 1) * 2;
					beside[i] = NONE;
					beside[i + 1] = NONE;
					if (dx >= 0 && dx < 16 && dz >= 0 && dz < 16) {
						continue;
					}
					this.context.lookup.apply(cell.reset(), this.minX + dx, this.minZ + dz, true);
					int surface = cell.waterLevel > 0.0F ? levels.scale(cell.waterLevel) : NONE;
					// the sea is the aquifer's
					if (surface > levels.waterY) {
						beside[i] = levels.scale(cell.height) - 1;
						beside[i + 1] = surface;
					}
				}
			}
			this.beside = beside;
		}
		return this.beside;
	}
}
