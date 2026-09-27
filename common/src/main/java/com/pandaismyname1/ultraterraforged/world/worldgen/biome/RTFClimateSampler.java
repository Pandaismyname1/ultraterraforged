package com.pandaismyname1.ultraterraforged.world.worldgen.biome;

import net.minecraft.core.BlockPos;

public interface RTFClimateSampler {
	void setSpawnSearchCenter(BlockPos center);
	
	BlockPos getSpawnSearchCenter();
}
