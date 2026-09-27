package com.pandaismyname1.ultraterraforged.world.worldgen.biome;

import net.minecraft.core.BlockPos;

public interface UTFClimateSampler {
	void setSpawnSearchCenter(BlockPos center);
	
	BlockPos getSpawnSearchCenter();
}
