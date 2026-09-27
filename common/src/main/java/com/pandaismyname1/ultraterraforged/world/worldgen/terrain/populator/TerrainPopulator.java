package com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator;

import com.pandaismyname1.ultraterraforged.data.preset.settings.TerrainSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.region.RegionSelector;

public record TerrainPopulator(float weight, Terrain type, Noise ground, Noise height, Noise erosion, Noise weirdness, float baseScale, float heightScale) implements CellPopulator, RegionSelector.Weighted {
    
	public TerrainPopulator(float weight, Terrain type, Noise base, Noise height, Noise erosion, Noise weirdness) {
		this(weight, type, base, height, erosion, weirdness, 1.0F, 1.0F);
	}
	
    @Override
    public void apply(Cell cell, float x, float z) {
        float ground = this.ground.compute(x, z, 0) * this.baseScale;
        float height = this.height.compute(x, z, 0) * this.heightScale;

        //TODO remove river check
        if(cell.terrain != TerrainType.RIVER) {
        	cell.terrain = this.type;
        }
        
        cell.height = Math.max(ground + height, 0.0F);
        cell.erosion = this.erosion.compute(x, z, 0);
        cell.weirdness = this.weirdness.compute(x, z, 0);
    }
    
    @Override
    public TerrainPopulator mapNoise(Noise.Visitor visitor) {
    	// keep the scales, or the base and vertical scale settings of every terrain type are lost
    	return new TerrainPopulator(this.weight, this.type, this.ground.mapAll(visitor), this.height.mapAll(visitor), this.erosion.mapAll(visitor), this.weirdness.mapAll(visitor), this.baseScale, this.heightScale);
    }
    
    public static TerrainPopulator make(Terrain type, Noise ground, Noise height, Noise erosion, Noise weirdness, TerrainSettings.Terrain settings) {
    	return new TerrainPopulator(settings.weight, type, ground, height, erosion, weirdness, settings.baseScale, settings.verticalScale);
    }
}
