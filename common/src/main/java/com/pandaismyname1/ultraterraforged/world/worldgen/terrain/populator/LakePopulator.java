package com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil.Vec2f;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.lake.LakeConfig;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.Boundsf;

public class LakePopulator {
    // where the land around a raised lake is lower than its water, it's built up into an embankment no further than this
    // many blocks out from the water
    private static final float SHORE = 24.0F;
    protected float valley;
    protected float valley2;
    protected float lakeDistance2;
    protected float valleyDistance2;
    protected float bankAlphaMin;
    protected float bankAlphaMax;
    protected float bankAlphaRange;
    private float depth;
    private float bankMin;
    private float bankMax;
    protected Vec2f center;
    private float water;
    private float unit;
    // how far the lake's water stands above the sea
    private float shift;
    
    private LakePopulator2 populator2;
    
    public LakePopulator(Vec2f center, float radius, float multiplier, LakeConfig config) {
    	this.populator2 = new LakePopulator2(center, radius, multiplier, config);
    	
    	float lake = radius * multiplier;
        float valley = 275.0F * multiplier;
        this.valley = valley;
        this.valley2 = valley * valley;
        this.center = center;
        this.depth = config.depth;
        this.bankMin = config.bankMin;
        this.bankMax = config.bankMax;
        this.bankAlphaMin = config.bankMin;
        this.bankAlphaMax = Math.min(1.0F, this.bankAlphaMin + 0.275F);
        this.bankAlphaRange = this.bankAlphaMax - this.bankAlphaMin;
        this.lakeDistance2 = lake * lake;
        this.valleyDistance2 = this.valley2 - this.lakeDistance2;
        this.water = config.water;
        this.unit = config.unit;
    }
    
    public Vec2f center() {
    	return this.center;
    }
    
    public float radius() {
    	return (float) Math.sqrt(this.lakeDistance2);
    }

    // how far out from its middle the shore holding the lake's water in reaches
    public float shoreRadius() {
    	return this.radius() + SHORE;
    }

    // how far out from its middle the lake's valley brings the land down
    public float valleyRadius() {
    	return this.valley;
    }

    public float distance2(float x, float z) {
    	return this.getDistance2(x, z);
    }

    // the ground the lake's valley leaves land of the given height at, as apply does, at a squared distance from its
    // middle; in the lake itself, where it holds its water, the land is left as it is
    public float valleyGround(float height, float distance2) {
    	if (distance2 <= this.lakeDistance2 || distance2 > this.valley2) {
    		return height;
    	}
    	float bankHeight = this.bankHeight(height);
    	if (height < bankHeight) {
    		return height;
    	}
    	return NoiseUtil.lerp(height, bankHeight, this.valleyAlpha(distance2));
    }

    // the ground the lake leaves land of the given height at, as apply does but for its embankment, at a squared distance
    // from its middle: in the lake itself its bed
    public float carvedGround(float height, float distance2) {
    	if (distance2 > this.lakeDistance2) {
    		return this.valleyGround(height, distance2);
    	}
    	height = Math.min(this.bankHeight(height), height);
    	if (distance2 < this.lakeDistance2) {
    		float depthAlpha = NoiseUtil.clamp(1.0F - distance2 / this.lakeDistance2, 0.0F, 1.0F);
    		height = NoiseUtil.lerp(height, Math.min(height, this.depth + this.shift), depthAlpha);
    	}
    	return height;
    }
    
    // the lake's water surface, when lakes lie above the sea
    public void setWaterLevel(float level) {
    	this.shift = level - this.water;
    }

    public float waterLevel() {
    	return this.water + this.shift;
    }

    // whether a place at a squared distance from the lake's middle is in the lake
    public boolean holds(float distance2) {
    	return distance2 < this.lakeDistance2;
    }
    
    public void apply(Cell cell, float x, float z) {
        float distance2 = this.getDistance2(x, z);
        if (distance2 > this.valley2) {
            return;
        }
        float level = this.water + this.shift;
        boolean raised = this.shift > this.unit;
        cell.riverBank = Math.min(cell.riverBank, (float) Math.sqrt(distance2 / this.lakeDistance2));
        float bankHeight = this.bankHeight(cell.height);
        if (distance2 <= this.lakeDistance2) {
            cell.height = this.carvedGround(cell.height, distance2);
            if (distance2 < this.lakeDistance2) {
                float depthAlpha = NoiseUtil.clamp(1.0F - distance2 / this.lakeDistance2, 0.0F, 1.0F);
                cell.terrain = TerrainType.LAKE;
                cell.riverDistance = Math.min(cell.riverDistance, 1.0F - depthAlpha);
                if (raised && cell.height < level) {
                	cell.waterLevel = cell.waterLevel > 0.0F ? Math.min(cell.waterLevel, level) : level;
                }
            }
            return;
        }
        float valleyAlpha = this.valleyAlpha(distance2);
        if (raised && cell.height < level + this.unit && distance2 <= this.shoreRadius() * this.shoreRadius() && cell.waterLevel == 0.0F && !cell.terrain.isRiver()) {
        	// the shore is lower than the lake's water: an embankment keeps it in
        	cell.height = level + this.unit;
        	cell.erosionMask = true;
        }
        if (cell.height < bankHeight) {
            return;
        }
        cell.height = NoiseUtil.lerp(cell.height, bankHeight, valleyAlpha);
        cell.riverDistance *= 1.0F - valleyAlpha;
        cell.riverDistance = Math.min(cell.riverDistance, 1.0F - valleyAlpha);
    }
    
    public void recordBounds(Boundsf.Builder builder) {
        builder.record(this.center.x() - this.valley * 1.2F, this.center.y() - this.valley * 1.2F);
        builder.record(this.center.x() + this.valley * 1.2F, this.center.y() + this.valley * 1.2F);
    }
    
    public boolean overlaps(float x, float z, float radius2) {
        float dist2 = this.getDistance2(x, z);
        return dist2 < this.lakeDistance2 + radius2;
    }
    
    protected float getDistance2(float x, float z) {
        float dx = this.center.x() - x;
        float dz = this.center.y() - z;
        return dx * dx + dz * dz;
    }
    
    private float valleyAlpha(float distance2) {
        float valleyAlpha = 1.0F - (distance2 - this.lakeDistance2) / this.valleyDistance2;
        if (valleyAlpha < 0.0F) {
            return 0.0F;
        }
        return Math.min(valleyAlpha, 1.0F);
    }

    protected float bankHeight(float height) {
        float bankHeightAlpha = NoiseUtil.map(height - this.shift, this.bankAlphaMin, this.bankAlphaMax, this.bankAlphaRange);
        return NoiseUtil.lerp(this.bankMin, this.bankMax, bankHeightAlpha) + this.shift;
    }
}
