package raccoonman.reterraforged.world.worldgen.rivermap;

import java.util.ArrayList;
import java.util.List;

import raccoonman.reterraforged.concurrent.cache.ExpiringEntry;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.noise.domain.Domain;
import raccoonman.reterraforged.world.worldgen.rivermap.gen.GenWarp;
import raccoonman.reterraforged.world.worldgen.rivermap.river.Network;
import raccoonman.reterraforged.world.worldgen.rivermap.river.River;
import raccoonman.reterraforged.world.worldgen.terrain.populator.RiverPopulator;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

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
    private volatile List<Mouth> mouths;
    
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
    
    /**
     * Where the main rivers of this map flow into the sea, in the rivers' space: see {@link #riverX}.
     */
    public List<Mouth> mouths() {
    	List<Mouth> mouths = this.mouths;
    	if (mouths == null) {
    		mouths = new ArrayList<>();
    		for (Network network : this.networks) {
    			RiverPopulator carver = network.riverCarver();
    			if (!carver.main || carver.junction >= 0.0F) {
    				continue;
    			}
    			River river = carver.river;
    			long mouth = WaterLevels.channel(river, carver.warp, 1.0F);
    			mouths.add(new Mouth(PosUtil.unpackLeftf(mouth), PosUtil.unpackRightf(mouth), river.ndx, river.ndz, carver.config.bankWidth, river.length));
    		}
    		this.mouths = mouths = List.copyOf(mouths);
    	}
    	return mouths;
    }
    
    // a place in the world in the rivers' space, where they're laid out as straight lines
    public float riverX(float x, float z) {
    	return this.riverWarp.getX(x, z, 0);
    }
    
    public float riverZ(float x, float z) {
    	return this.riverWarp.getZ(x, z, 0);
    }
    
    /**
     * @param dirX the way the river flows, into the sea
     * @param width the width of its banks at the mouth, in blocks either side of its middle
     * @param length the length of its last straight stretch
     */
    public record Mouth(float x, float z, float dirX, float dirZ, float width, float length) {
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
