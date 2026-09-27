package raccoonman.reterraforged.world.worldgen.terrain.populator;

import raccoonman.reterraforged.world.worldgen.biome.Erosion;
import raccoonman.reterraforged.world.worldgen.biome.Weirdness;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.CellPopulator;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.terrain.region.RegionSelector;

public record VolcanoPopulator(float weight, Noise ground, Noise cone, Noise height, Noise lowlands, float inversionPoint, float blendLower, float blendUpper, float blendRange, Levels levels) implements CellPopulator, RegionSelector.Weighted {
    
    // the inner part of the crater, as a share of its radius, is a flat floor
    private static final float CRATER_FLOOR = 0.55F;

    @Override
    public void apply(Cell cell, float x, float z) {
        float value = this.cone.compute(x, z, 0);
        float limit = this.height.compute(x, z, 0);
        float maxHeight = limit * this.inversionPoint;
        if (value > maxHeight) {
            float steepnessModifier = 1.0F;
            float delta = (value - maxHeight) * steepnessModifier;
            float range = limit - maxHeight;
            float alpha = delta / range;
            // the crater: walls fall from the rim to a flat floor, which holds a lava lake
            float floor = Math.min(1.0F, alpha / CRATER_FLOOR);
            cell.terrain = alpha > CRATER_FLOOR ? TerrainType.VOLCANO_PIPE : TerrainType.VOLCANO;
            value = maxHeight - maxHeight / 5.0F * floor;
            // the filters would fill the crater in
            cell.erosionMask = true;
        } else if (value < this.blendLower) {
            float lowlands = this.lowlands.compute(x, z, 0);
            value += lowlands;
            cell.terrain = TerrainType.VOLCANIC_LOWLANDS;
        } else if (value < this.blendUpper) {
            float alpha2 = 1.0F - (value - this.blendLower) / this.blendRange;
            float lowlands = this.lowlands.compute(x, z, 0);
            value += lowlands * alpha2;
            // the foot of the cone
            cell.terrain = alpha2 > 0.5F ? TerrainType.VOLCANIC_LOWLANDS : TerrainType.VOLCANO;
        } else {
            // the cone itself; left unmarked, its slopes got no volcanic rock
            cell.terrain = TerrainType.VOLCANO;
        }
        cell.height = this.ground.compute(x, z, 0) + value;
        cell.weirdness = Weirdness.LOW_SLICE_VARIANT_ASCENDING.midpoint();
        cell.erosion = Erosion.LEVEL_4.midpoint();
    }
    
    @Override
    public CellPopulator mapNoise(Noise.Visitor visitor) {
    	return new VolcanoPopulator(this.weight, this.ground.mapAll(visitor), this.cone.mapAll(visitor), this.height.mapAll(visitor), this.lowlands.mapAll(visitor), this.inversionPoint, this.blendLower, this.blendUpper, this.blendRange, this.levels);
    }
    
    public static void modifyVolcanoType(Cell cell, Levels levels) {
        // no lava where a river runs; river valleys reach far wider, so only close to the river itself
        if (cell.terrain == TerrainType.VOLCANO_PIPE && (cell.height < levels.water || cell.riverDistance < 0.3F)) {
            cell.terrain = TerrainType.VOLCANO;
        }
    }
}
