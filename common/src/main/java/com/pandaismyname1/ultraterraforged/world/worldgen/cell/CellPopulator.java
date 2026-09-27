package com.pandaismyname1.ultraterraforged.world.worldgen.cell;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public interface CellPopulator {
    void apply(Cell cell, float x, float z);
    
    default CellPopulator mapNoise(Noise.Visitor visitor) {
    	return this;
    }
}
