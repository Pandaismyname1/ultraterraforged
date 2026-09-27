package com.pandaismyname1.ultraterraforged.world.worldgen.tile.filter;

import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings.ControlPoints;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Size;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

public record BeachDetect(Levels levels, ControlPoints transition) implements Filter {
    
    @Override
    public void apply(Tile tile, int seedX, int seedZ, int iterations) {
        Size size = tile.getBlockSize();
        int total = size.total();
        
        for(int x = 0; x < total; x++) {
        	for(int z = 0; z < total; z++) {
        		Cell cell = tile.getCellRaw(x, z);
	        	if (cell.terrain.isCoast() && !cell.terrain.isWetland() && cell.continentEdge < this.transition.beach) {
	                Cell n = tile.getCellRaw(x, z - 8);
	                Cell s = tile.getCellRaw(x, z + 8);
	                Cell e = tile.getCellRaw(x + 8, z);
	                Cell w = tile.getCellRaw(x - 8, z);
	                float gx = this.grad(e, w, cell);
	                float gz = this.grad(n, s, cell);
	                float d2 = gx * gx + gz * gz;
	                if (d2 < 0.275F) {
                		tile.getCellRaw(x, z).terrain = TerrainType.BEACH;
	                }
	            }
	        }
        }
    }
    
    private float grad(Cell a, Cell b, Cell def) {
        int distance = 17;
        if (a.isAbsent()) {
            a = def;
            distance -= 8;
        }
        if (b.isAbsent()) {
            b = def;
            distance -= 8;
        }
        return (a.height - b.height) / distance;
    }
    
    public static BeachDetect make(GeneratorContext ctx) {
    	Levels levels = ctx.levels;
    	ControlPoints transition = ctx.preset.world().controlPoints;
    	return new BeachDetect(levels, transition);
    }
}
