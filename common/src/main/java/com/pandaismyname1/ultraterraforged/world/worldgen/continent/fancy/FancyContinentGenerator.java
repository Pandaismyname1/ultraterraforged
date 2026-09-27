package com.pandaismyname1.ultraterraforged.world.worldgen.continent.fancy;

import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.Continent;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domain;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domains;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.RiverCache;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.Rivermap;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.Seed;

public class FancyContinentGenerator implements Continent {
    private float frequency;
    private Domain warp;
    private FancyContinent source;
    private RiverCache riverCache;
    
    public FancyContinentGenerator(Seed seed, GeneratorContext context) {
        WorldSettings settings = context.preset.world();
        int warpScale = settings.continent.continentScale / 2;
        float warpStrength = warpScale * 0.4F;
        this.source = new FancyContinent(seed.next(), 4, 0.2F, context, this);
        this.frequency = 1.0F / settings.continent.continentScale;
        this.riverCache = new RiverCache(this.source);
        
        Domain warp = Domains.domainSimplex(seed.next(), warpScale, 2, warpStrength);
        warp = Domains.add(warp, Domains.domainPerlin(seed.next(), 80, 2, 40.0F)); 
        warp = Domains.add(warp, Domains.domainPerlin(seed.next(), 20, 1, 15.0F));
        this.warp = warp;
    }
    
    public FancyContinent getSource() {
        return this.source;
    }
    
    @Override
    public Rivermap getRivermap(int x, int y) {
        return this.riverCache.getRivers(x, y);
    }
    
    @Override
    public float getEdgeValue(float x, float y) {
        float px = this.warp.getX(x, y, 0);
        float py = this.warp.getZ(x, y, 0);
        px *= this.frequency;
        py *= this.frequency;
        return this.source.getEdgeValue(px, py, 0);
    }
    
    @Override
    public float getLandValue(float x, float y) {
        float px = this.warp.getX(x, y, 0);
        float py = this.warp.getZ(x, y, 0);
        px *= this.frequency;
        py *= this.frequency;
        float value = this.source.getLandValue(px, py);
        return NoiseUtil.map(value, 0.2F, 0.4F, 0.2F);
    }
    
    @Override
    public long getNearestCenter(float x, float y) {
        long min = this.source.getMin();
        long max = this.source.getMax();
        float width = PosUtil.unpackLeftf(max) - PosUtil.unpackLeftf(min);
        float height = PosUtil.unpackRightf(max) - PosUtil.unpackRightf(min);
        float cx = width * 0.5F;
        float cz = height * 0.5F;
        int centerX = (int)(cx / this.frequency);
        int centerZ = (int)(cz / this.frequency);
        return PosUtil.pack(centerX, centerZ);
    }
    
    @Override
    public void apply(Cell cell, float x, float y) {
        cell.continentX = 0;
        cell.continentZ = 0;
        cell.continentId = 0.0F;
        
        float continentalness = this.getEdgeValue(x, y);
        cell.continentEdge = continentalness;
    }
}
