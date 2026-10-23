package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.pandaismyname1.ultraterraforged.concurrent.cache.Cache;
import com.pandaismyname1.ultraterraforged.concurrent.cache.CacheManager;
import com.pandaismyname1.ultraterraforged.concurrent.cache.map.StampedLongMap;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

public class RiverCache {
    protected RiverGenerator generator;
    protected Cache<Rivermap> cache;
    // river maps being made: whoever else asks for one of these waits for it rather than making it again
    private final ConcurrentHashMap<Long, FutureTask<Rivermap>> pending = new ConcurrentHashMap<>();

    public RiverCache(RiverGenerator generator) {
        this.cache = CacheManager.createCache(32, 5L, 1L, TimeUnit.MINUTES, StampedLongMap::new);
        this.generator = generator;
    }

    public Rivermap getRivers(int x, int z) {
        long id = PosUtil.pack(x, z);
        Rivermap rivermap = this.cache.get(id);
        if (rivermap == null) {
            rivermap = this.generate(x, z, id);
        }
        rivermap.touch();
        return rivermap;
    }

    // Made outside the cache's lock: the cache made it while holding its lock for writing, so every river map lookup,
    // of any map, waited for it, and with many big rivers all of the world's generator threads queued behind one
    private Rivermap generate(int x, int z, long id) {
        FutureTask<Rivermap> task = new FutureTask<>(() -> {
            Rivermap made = this.generator.generateRivers(x, z, id);
            return this.cache.computeIfAbsent(id, key -> made);
        });
        FutureTask<Rivermap> running = this.pending.putIfAbsent(id, task);
        if (running == null) {
            running = task;
            try {
                task.run();
            } finally {
                this.pending.remove(id, task);
            }
        }
        try {
            return running.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for the rivers at " + x + ", " + z, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        }
    }
}
