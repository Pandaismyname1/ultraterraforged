package raccoonman.reterraforged.world.worldgen.landform;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

/**
 * A distinct feature of the land, like a butte or a sea stack, shaped into the heightmap after the terrain types and
 * rivers.
 */
public interface Landform {

	/**
	 * @param heightmap for landforms that need the terrain elsewhere, e.g. at their centre
	 */
	void apply(Cell cell, float x, float z, Heightmap heightmap);

	// like CellPopulator.mapNoise, so the heightmap's per thread caches reach the landform's noises
	Landform mapNoise(Noise.Visitor visitor);

	static float smoothstep(float value, float from, float to) {
		float t = Math.max(0.0F, Math.min(1.0F, (value - from) / (to - from)));
		return t * t * (3.0F - 2.0F * t);
	}
}
