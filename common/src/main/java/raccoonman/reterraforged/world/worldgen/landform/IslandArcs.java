package raccoonman.reterraforged.world.worldgen.landform;

import raccoonman.reterraforged.data.preset.settings.CoastSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Volcanic island arcs: curved chains of volcanoes rising out of the open sea, as along the Aleutians or the Lesser
 * Antilles. Each is a steep cone of dark rock, some with a crater of lava at the top, standing on a broad underwater
 * base. Each chain lies in its own cell of a coarse grid.
 *
 * @param wobble roughens the cones
 */
public record IslandArcs(int seed, float frequency, float shoreline, Noise wobble, Levels levels, SiteCache<Arc> arcs) implements Landform {
	private static final float GRID = 3600.0F;
	private static final int MIN_ISLANDS = 4;
	private static final int MAX_ISLANDS = 8;
	// continent values below this are the open sea
	private static final float OPEN_SEA = 0.2F;
	// the cones' underwater slopes, in blocks per block
	private static final float SLOPE = 0.45F;
	private static final Arc NONE = new Arc(new float[0]);

	/**
	 * The islands, four floats each: where they stand, how big they are, and how high they rise above the sea.
	 */
	record Arc(float[] islands) {
	}

	public static IslandArcs make(int seed, CoastSettings.IslandArcs settings, float shoreline, Levels levels) {
		return new IslandArcs(seed, NoiseUtil.clamp(settings.frequency, 0.0F, 1.0F), shoreline, Noises.perlin(seed + 1, 20, 2), levels, SiteCache.make());
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.height >= this.levels.water || cell.continentEdge > this.shoreline - OPEN_SEA * 0.5F) {
			return;
		}
		int gridX = NoiseUtil.floor(x / GRID);
		int gridZ = NoiseUtil.floor(z / GRID);
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (Landform.random(this.seed, gx, gz, 0) >= this.frequency) {
					continue;
				}
				Arc arc = this.arcs.get(gx, gz, (ax, az) -> this.find(ax, az, heightmap));
				float[] islands = arc.islands();
				for (int i = 0; i < islands.length; i += 4) {
					this.cone(cell, x, z, islands[i], islands[i + 1], islands[i + 2], islands[i + 3]);
				}
			}
		}
	}

	private void cone(Cell cell, float x, float z, float centerX, float centerZ, float radius, float peak) {
		float dx = x - centerX;
		float dz = z - centerZ;
		// the whole cone, down to the sea floor
		float reach = radius + peak / SLOPE + 60.0F;
		if (dx * dx + dz * dz > reach * reach) {
			return;
		}
		float distance = (float) Math.sqrt(dx * dx + dz * dz) * (1.0F + (this.wobble.compute(x, z, 0) - 0.5F) * 0.6F);
		float d = distance / radius;
		// above the sea: a concave cone; below it, its base slopes on down to the floor
		float blocks = d < 1.0F ? peak * (float) Math.pow(1.0F - d, 1.4F) : -(distance - radius) * SLOPE;
		// the crater
		float crater = radius * 0.16F;
		boolean inCrater = peak > 25.0F && distance < crater;
		if (inCrater) {
			blocks -= Math.min(10.0F, peak * 0.25F) * (1.0F - distance / crater);
		}
		float height = this.levels.water + blocks * this.levels.unit;
		if (height <= cell.height) {
			return;
		}
		cell.height = height;
		cell.erosionMask = true;
		if (height >= this.levels.water) {
			cell.terrain = inCrater ? TerrainType.VOLCANO_PIPE : TerrainType.VOLCANIC_ISLAND;
		}
	}

	private Arc find(int gridX, int gridZ, Heightmap heightmap) {
		float centerX = (gridX + 0.5F) * GRID;
		float centerZ = (gridZ + 0.5F) * GRID;
		float radius = NoiseUtil.lerp(1000.0F, 1700.0F, Landform.random(this.seed, gridX, gridZ, 1));
		float start = Landform.random(this.seed, gridX, gridZ, 2) * NoiseUtil.PI2;
		float sweep = NoiseUtil.lerp(1.1F, 2.0F, Landform.random(this.seed, gridX, gridZ, 3));
		int count = MIN_ISLANDS + (int) (Landform.random(this.seed, gridX, gridZ, 4) * (MAX_ISLANDS - MIN_ISLANDS + 1));
		float[] islands = new float[count * 4];
		int placed = 0;
		for (int i = 0; i < count; i++) {
			float angle = start + sweep * (i + (Landform.random(this.seed, gridX, gridZ, 10 + i) - 0.5F) * 0.5F) / count;
			float x = centerX + NoiseUtil.cos(angle) * radius;
			float z = centerZ + NoiseUtil.sin(angle) * radius;
			Cell ground = heightmap.sampleGround(x, z);
			// out in deep, open sea
			if (ground.continentEdge > this.shoreline - OPEN_SEA || ground.height > this.levels.water(-20)) {
				continue;
			}
			islands[placed * 4] = x;
			islands[placed * 4 + 1] = z;
			islands[placed * 4 + 2] = NoiseUtil.lerp(25.0F, 85.0F, Landform.random(this.seed, gridX, gridZ, 30 + i));
			islands[placed * 4 + 3] = NoiseUtil.lerp(12.0F, 60.0F, Landform.random(this.seed, gridX, gridZ, 50 + i));
			placed++;
		}
		if (placed < 3) {
			return NONE;
		}
		float[] kept = new float[placed * 4];
		System.arraycopy(islands, 0, kept, 0, kept.length);
		return new Arc(kept);
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new IslandArcs(this.seed, this.frequency, this.shoreline, this.wobble.mapAll(visitor), this.levels, this.arcs);
	}
}
