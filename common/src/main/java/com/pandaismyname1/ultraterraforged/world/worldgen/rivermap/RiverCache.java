package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import java.util.concurrent.TimeUnit;

import com.pandaismyname1.ultraterraforged.concurrent.cache.Cache;
import com.pandaismyname1.ultraterraforged.concurrent.cache.CacheManager;
import com.pandaismyname1.ultraterraforged.concurrent.cache.map.StampedLongMap;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

public class RiverCache {
    protected RiverGenerator generator;
    protected Cache<Rivermap> cache;
    
    public RiverCache(RiverGenerator generator) {
        this.cache = CacheManager.createCache(32, 5L, 1L, TimeUnit.MINUTES, StampedLongMap::new);
        this.generator = generator;
    }
    
    public Rivermap getRivers(int x, int z) {
        return this.cache.computeIfAbsent(PosUtil.pack(x, z), id -> {
        	return this.generator.generateRivers(PosUtil.unpackLeft(id), PosUtil.unpackRight(id), id);
        });
    }
}
