package com.pandaismyname1.ultraterraforged.world.worldgen.tile.filter;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.filter.Filter.Visitor;

/**
 * How steep the ground is. On land the sea counts as a flat surface at its level. Under the sea, with seaFloor, the
 * slope of the sea floor itself: without it, every place under the sea came out as steep as can be, since its
 * neighbours were measured at the sea's level.
 */
public record Steepness(int radius, float scaler, float waterLevel, boolean seaFloor) implements Filter, Visitor {

	@Override
	public void apply(Tile tile, int seedX, int seedZ, int iterations) {
		this.iterate(tile, this);
	}

	@Override
	public void visit(Tile tile, Cell cell, int cx, int cz) {
		boolean underwater = this.seaFloor && cell.height < this.waterLevel;
		float totalHeightDif = 0.0F;
		for (int dz = -1; dz <= 2; ++dz) {
			for (int dx = -1; dx <= 2; ++dx) {
				if (dx != 0 || dz != 0) {
					int x = cx + dx * this.radius;
					int z = cz + dz * this.radius;
					Cell neighbour = tile.getCellRaw(x, z);
					if (!neighbour.isAbsent()) {
						float height = underwater ? neighbour.height : Math.max(neighbour.height, this.waterLevel);
						totalHeightDif += Math.abs(cell.height - height) / this.radius;
					}
				}
			}
		}
		cell.gradient = Math.min(1.0F, totalHeightDif * this.scaler);
	}
	
	public static Steepness make(int radius, float scaler, Levels levels, boolean seaFloor) {
		return new Steepness(radius, scaler, levels.water, seaFloor);
	}
}
