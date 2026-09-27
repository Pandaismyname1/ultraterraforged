package raccoonman.reterraforged.world.worldgen.rivermap;

import raccoonman.reterraforged.concurrent.cache.ExpiringEntry;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.noise.domain.Domain;
import raccoonman.reterraforged.world.worldgen.rivermap.gen.GenWarp;
import raccoonman.reterraforged.world.worldgen.rivermap.river.Network;

public class Rivermap implements ExpiringEntry {
    private int x;
    private int z;
    private Domain lakeWarp;
    private Domain riverWarp;
    private Network[] networks;
    private long timestamp;
    private boolean raisedWater;
    private int gorgeDepth;
    private volatile boolean levelled;
    
    public Rivermap(int x, int z, Network[] networks, GenWarp warp, boolean raisedWater, int gorgeDepth) {
        this.timestamp = System.currentTimeMillis();
        this.x = x;
        this.z = z;
        this.networks = networks;
        this.lakeWarp = warp.lake();
        this.riverWarp = warp.river();
        this.raisedWater = raisedWater;
        this.gorgeDepth = gorgeDepth;
    }
    
    /**
     * Works out where the water of rivers, lakes and wetlands above the sea lies, from the land they run through; once,
     * by the first heightmap to use this map.
     */
    public void levelWater(Heightmap heightmap) {
    	if (!this.raisedWater || this.levelled) {
    		return;
    	}
    	synchronized (this) {
    		if (!this.levelled) {
    			WaterLevels.level(this.networks, this.riverWarp, this.gorgeDepth, heightmap);
    			this.levelled = true;
    		}
    	}
    }
    
    public void apply(Cell cell, float x, float z) {
        float rx = this.riverWarp.getX(x, z, 0);
        float rz = this.riverWarp.getZ(x, z, 0);
        float lx = this.lakeWarp.getOffsetX(rx, rz, 0);
        float lz = this.lakeWarp.getOffsetZ(rx, rz, 0);
        for (Network network : this.networks) {
            if (network.contains(rx, rz)) {
                network.carve(cell, rx, rz, lx, lz);
            }
        }
    }
    
    @Override
    public long getTimestamp() {
        return this.timestamp;
    }
    
    public int getX() {
        return this.x;
    }
    
    public int getZ() {
        return this.z;
    }
    
    public static Rivermap get(Cell cell, Rivermap instance, Heightmap heightmap) {
        return get(cell.continentX, cell.continentZ, instance, heightmap);
    }
    
    public static Rivermap get(int x, int z, Rivermap instance, Heightmap heightmap) {
        if (instance != null && x == instance.getX() && z == instance.getZ()) {
            return instance;
        }
        Rivermap rivermap = heightmap.continent().getRivermap(x, z);
        rivermap.levelWater(heightmap);
        return rivermap;
    }
}
