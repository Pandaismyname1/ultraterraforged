package com.pandaismyname1.ultraterraforged.world.worldgen.terrain.region;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public class RegionLerper implements CellPopulator {
    private CellPopulator lower;
    private CellPopulator upper;
    
    public RegionLerper(CellPopulator lower, CellPopulator upper) {
        this.lower = lower;
        this.upper = upper;
    }
    
    @Override
    public void apply(Cell cell, float x, float y) {
        float alpha = cell.terrainRegionEdge;
        if (alpha == 0.0F) {
            this.lower.apply(cell, x, y);
            return;
        }
        if (alpha == 1.0F) {
            this.upper.apply(cell, x, y);
            return;
        }
        
        this.lower.apply(cell, x, y);
        float lowerHeight = cell.height;
        float lowerErosion = cell.erosion;
        float lowerWeirdness = cell.weirdness;
        
        this.upper.apply(cell, x, y);
        float upperHeight = cell.height;
        float upperErosion = cell.erosion;
        float upperWeirdness = cell.weirdness;
        
        cell.height = NoiseUtil.lerp(lowerHeight, upperHeight, alpha);
        cell.erosion = NoiseUtil.lerp(lowerErosion, upperErosion, alpha);
        cell.weirdness = NoiseUtil.lerp(lowerWeirdness, upperWeirdness, alpha);
    }
    
    @Override
    public CellPopulator mapNoise(Noise.Visitor visitor) {
    	return new RegionLerper(this.lower.mapNoise(visitor), this.upper.mapNoise(visitor));
    }
}
