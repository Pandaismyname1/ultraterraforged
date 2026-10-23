package com.pandaismyname1.ultraterraforged.world.worldgen;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.RaisedWaterGuard;

/**
 * A chunk's aquifer. The volume ends at the chunk's highest ground (MixinNoiseBasedChunkGenerator), but carvers in the
 * same chunk ask the aquifer about blocks up to the build limit; above the volume there's only air, as the disabled
 * aquifer says. While the carvers run, blocks the guard keeps are left as they are.
 */
public class VolumeAquifer implements Aquifer {
	private final Aquifer inside;
	private final Aquifer above;
	private final int maxY;
	private Aquifer last;
	@Nullable
	private RaisedWaterGuard guard;

	public VolumeAquifer(Aquifer inside, Aquifer above, int maxY) {
		this.inside = inside;
		this.above = above;
		this.maxY = maxY;
		this.last = inside;
	}

	public void setGuard(@Nullable RaisedWaterGuard guard) {
		this.guard = guard;
	}

	@Nullable
	@Override
	public BlockState computeSubstance(int blockX, int blockY, int blockZ, double density) {
		this.last = blockY > this.maxY ? this.above : this.inside;
		BlockState state = this.last.computeSubstance(blockX, blockY, blockZ, density);
		// null leaves the block alone
		if (this.guard != null && state != null && state.getFluidState().isEmpty() && this.guard.keeps(blockX, blockY, blockZ)) {
			return null;
		}
		return state;
	}

	@Override
	public boolean shouldScheduleFluidUpdate() {
		return this.last.shouldScheduleFluidUpdate();
	}
}
