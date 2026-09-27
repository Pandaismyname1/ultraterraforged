package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import java.util.ArrayList;
import java.util.List;

import com.pandaismyname1.ultraterraforged.concurrent.cache.ExpiringEntry;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domain;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.gen.GenWarp;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.Network;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.River;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.RiverPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

public class Rivermap implements ExpiringEntry {
    private int x;
    private int z;
    private Domain lakeWarp;
    private Domain riverWarp;
    private Network[] networks;
    private long timestamp;
    private boolean raisedWater;
    private int gorgeDepth;
    private boolean winding;
    private volatile boolean levelled;
    private volatile List<Mouth> mouths;
    
    public Rivermap(int x, int z, Network[] networks, GenWarp warp, boolean raisedWater, int gorgeDepth, boolean winding) {
        this.timestamp = System.currentTimeMillis();
        this.x = x;
        this.z = z;
        this.networks = networks;
        this.lakeWarp = warp.lake();
        this.riverWarp = warp.river();
        this.raisedWater = raisedWater;
        this.gorgeDepth = gorgeDepth;
        this.winding = winding;
    }
    
    /**
     * Moves rivers onto the lowest ground they can reach, and works out where the water of rivers, lakes and wetlands
     * above the sea lies, from the land they run through; once, by the first heightmap to use this map.
     */
    public void levelWater(Heightmap heightmap) {
    	if (!this.raisedWater && !this.winding || this.levelled) {
    		return;
    	}
    	synchronized (this) {
    		if (!this.levelled) {
    			RiverRoutes.route(this.networks, this.riverWarp, heightmap, this.winding);
    			if (this.raisedWater) {
    				WaterLevels.level(this.networks, this.gorgeDepth, heightmap);
    			}
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
    			long mouth = RiverRoutes.channel(carver, 1.0F);
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
