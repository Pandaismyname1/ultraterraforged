package raccoonman.reterraforged.world.worldgen.tile.filter;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.tile.Tile;

/**
 * Keeps the water of rivers, lakes and wetlands above the sea tidy after erosion and smoothing: dry ground right beside
 * it that's lower than the water, where a bank was worn down, is raised to the water's surface, and single spots of
 * the bed left standing out of the water are sunk back under it.
 */
public record WaterRim(Levels levels) implements Filter {

	@Override
	public void apply(Tile tile, int regionX, int regionZ, int iterationsPerChunks) {
		int size = tile.getBlockSize().total();
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++) {
				Cell cell = tile.getCellRaw(x, z);
				int y = this.levels.scale(cell.height);
				if (cell.waterLevel > 0.0F && !this.wet(cell) && x > 0 && z > 0 && x < size - 1 && z < size - 1) {
					int around = this.wetCount(tile.getCellRaw(x - 1, z)) + this.wetCount(tile.getCellRaw(x + 1, z)) + this.wetCount(tile.getCellRaw(x, z - 1)) + this.wetCount(tile.getCellRaw(x, z + 1));
					if (around >= 3) {
						cell.height = Math.min(cell.height, cell.waterLevel - this.levels.unit);
						continue;
					}
				}
				// wet ground, and ground under the sea, stay as they are
				if (this.wet(cell) || y < this.levels.waterY) {
					continue;
				}
				float water = 0.0F;
				if (x > 0) water = Math.max(water, this.surface(tile.getCellRaw(x - 1, z)));
				if (x < size - 1) water = Math.max(water, this.surface(tile.getCellRaw(x + 1, z)));
				if (z > 0) water = Math.max(water, this.surface(tile.getCellRaw(x, z - 1)));
				if (z < size - 1) water = Math.max(water, this.surface(tile.getCellRaw(x, z + 1)));
				if (y < this.levels.scale(water)) {
					cell.height = water;
				}
			}
		}
	}

	private boolean wet(Cell cell) {
		return cell.waterLevel > 0.0F && this.levels.scale(cell.height) < this.levels.scale(cell.waterLevel);
	}

	private int wetCount(Cell cell) {
		return this.wet(cell) ? 1 : 0;
	}

	// the water surface over a cell, or 0 if it's dry
	private float surface(Cell cell) {
		return this.wet(cell) ? cell.waterLevel : 0.0F;
	}
}
