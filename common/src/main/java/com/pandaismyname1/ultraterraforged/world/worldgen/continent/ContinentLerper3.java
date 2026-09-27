package com.pandaismyname1.ultraterraforged.world.worldgen.continent;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.Interpolation;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public class ContinentLerper3 implements CellPopulator {
    private CellPopulator lower;
    private CellPopulator middle;
    private CellPopulator upper;
    private Interpolation interpolation;
    private float midpoint;
    private float blendLower;
    private float blendUpper;
    private float lowerRange;
    private float upperRange;
    
    public ContinentLerper3(CellPopulator lower, CellPopulator middle, CellPopulator upper, float min, float mid, float max) {
        this(lower, middle, upper, min, mid, max, Interpolation.CURVE3);
    }
    
    public ContinentLerper3(CellPopulator lower, CellPopulator middle, CellPopulator upper, float min, float mid, float max, Interpolation interpolation) {
        this.lower = lower;
        this.upper = upper;
        this.middle = middle;
        this.interpolation = interpolation;
        this.midpoint = mid;
        this.blendLower = min;
        this.blendUpper = max;
        this.lowerRange = this.midpoint - this.blendLower;
        this.upperRange = this.blendUpper - this.midpoint;
    }
    
    @Override
    public void apply(Cell cell, float x, float y) {
        float select = cell.continentEdge;
        if (select < this.blendLower) {
            this.lower.apply(cell, x, y);
            return;
        }
        if (select > this.blendUpper) {
            this.upper.apply(cell, x, y);
            return;
        }
        if (select < this.midpoint) {
            float alpha = this.interpolation.apply((select - this.blendLower) / this.lowerRange);
            this.lower.apply(cell, x, y);
            float lowerHeight = cell.height;
            this.middle.apply(cell, x, y);
            cell.height = NoiseUtil.lerp(lowerHeight, cell.height, alpha);
        } else {
            float alpha = this.interpolation.apply((select - this.midpoint) / this.upperRange);
            this.middle.apply(cell, x, y);
            float lowerHeight = cell.height;
            this.upper.apply(cell, x, y);
            cell.height = NoiseUtil.lerp(lowerHeight, cell.height, alpha);
        }
    }


    @Override
    public CellPopulator mapNoise(Noise.Visitor visitor) {
    	return new ContinentLerper3(this.lower.mapNoise(visitor), this.middle.mapNoise(visitor), this.upper.mapNoise(visitor), this.blendLower, this.midpoint, this.blendUpper, this.interpolation);
    }
}
