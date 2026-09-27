package com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator;

import com.pandaismyname1.ultraterraforged.world.worldgen.biome.Weirdness;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil.Vec2f;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Line;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.Boundsf;

public class WetlandPopulator {
    private Vec2f a;
    private Vec2f b;
    private float radius;
    private float radiusSq;
    private float bed;
    private float banks;
    private float moundMin;
    private float moundMax;
    private float moundVariance;
    private Noise moundShape;
    private Noise moundHeight;
    private Noise terrainEdge;
    // where along its river the wetland starts and ends, from 0 at the source to 1 at the mouth
    public float startT;
    public float endT;
    private float water;
    // how far the wetland's water stands above the sea
    private float shift;
    // off where the river drops too steeply for a marsh
    private boolean enabled = true;
    
    public WetlandPopulator(int seed, Vec2f a, Vec2f b, float radius, Levels levels) {
        this.a = a;
        this.b = b;
        this.radius = radius;
        this.radiusSq = radius * radius;
        this.bed = levels.water(-1) - 0.5F / levels.worldHeight;
        this.banks = levels.ground(3);
        this.moundMin = levels.water(1);
        this.moundMax = levels.water(2);
        this.moundVariance = this.moundMax - this.moundMin;
        this.water = levels.water;
        
        Noise moundShape = Noises.perlin(++seed, 10, 1);
        moundShape = Noises.clamp(moundShape, 0.3F, 0.6F);
        moundShape = Noises.map(moundShape, 0.0F, 1.0F);
        this.moundShape = moundShape;
        
        Noise moundHeight = Noises.simplex(++seed, 20, 1);
        moundHeight = Noises.clamp(moundHeight, 0.0F, 0.3F);
        moundHeight = Noises.map(moundHeight, 0.0F, 1.0F);
        this.moundHeight = moundHeight;
        
        Noise terrainEdge = Noises.perlin(++seed, 8, 1);
        terrainEdge = Noises.clamp(terrainEdge, 0.2F, 0.8F);
        terrainEdge = Noises.map(terrainEdge, 0.0F, 0.9F);
        this.terrainEdge = terrainEdge;
    }
    
    // the wetland's water surface, when wetlands lie above the sea
    public void setWaterLevel(float level) {
    	this.shift = level - this.water;
    }
    
    public void disable() {
    	this.enabled = false;
    }
    
    public void apply(Cell cell, float rx, float rz, float x, float z) {
        if (!this.enabled) {
        	return;
        }
        float bed = this.bed + this.shift;
        float banks = this.banks + this.shift;
        float moundMin = this.moundMin + this.shift;
        float moundMax = this.moundMax + this.shift;
        if (cell.height < bed) {
            return;
        }
        float t = Line.distanceOnLine(rx, rz, this.a.x(), this.a.y(), this.b.x(), this.b.y());
        float d2 = getDistanceSq(rx, rz, this.a.x(), this.a.y(), this.b.x(), this.b.y(), t);
        if (d2 > this.radiusSq) {
            return;
        }
        float dist = 1.0F - d2 / this.radiusSq;
        if (dist <= 0.0F) {
            return;
        }
        float valleyAlpha = NoiseUtil.map(dist, 0.0F, 0.65F, 0.65F);
        if (cell.height > banks) {
            cell.height = NoiseUtil.lerp(cell.height, banks, valleyAlpha);
        }

        float poolsAlpha = NoiseUtil.map(dist, 0.65F, 0.7F, 0.050000012F);
        if (cell.height > bed && cell.height <= banks) {
            cell.height = NoiseUtil.lerp(cell.height, bed, poolsAlpha);
        }
        if (poolsAlpha >= 1.0F) {
            cell.erosionMask = true;
        }
        if (dist > 0.65F && poolsAlpha > this.terrainEdge.compute(x, z, 0)) {
            cell.terrain = TerrainType.WETLAND;
        }
        if (cell.height >= bed && cell.height < moundMax) {
            float shapeAlpha = this.moundShape.compute(x, z, 0) * poolsAlpha;
            float mounds = moundMin + this.moundHeight.compute(x, z, 0) * this.moundVariance;
            cell.height = NoiseUtil.lerp(cell.height, mounds, shapeAlpha);
        }
        if (this.shift > 0.0F && cell.height < this.water + this.shift && poolsAlpha > 0.0F) {
        	float level = this.water + this.shift;
        	cell.waterLevel = cell.waterLevel > 0.0F ? Math.min(cell.waterLevel, level) : level;
        }
        cell.riverDistance = Math.min(cell.riverDistance, 1.0F - valleyAlpha);
    }
    
    public void recordBounds(Boundsf.Builder builder) {
        builder.record(Math.min(this.a.x(), this.b.x()) - this.radius, Math.min(this.a.y(), this.b.y()) - this.radius);
        builder.record(Math.max(this.a.x(), this.b.x()) + this.radius, Math.max(this.a.y(), this.b.y()) + this.radius);
    }
    
    private static float getDistanceSq(float x, float y, float ax, float ay, float bx, float by, float t) {
        if (t <= 0.0f) {
            return Line.distSq(x, y, ax, ay);
        }
        if (t >= 1.0f) {
            return Line.distSq(x, y, bx, by);
        }
        float px = ax + t * (bx - ax);
        float py = ay + t * (by - ay);
        return Line.distSq(x, y, px, py);
    }
}
