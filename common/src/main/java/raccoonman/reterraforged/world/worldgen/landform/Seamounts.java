package raccoonman.reterraforged.world.worldgen.landform;

import raccoonman.reterraforged.data.preset.settings.OceanSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Seamounts and guyots: old volcanoes rising from the deep ocean floor without reaching the surface. A guyot's top
 * was worn flat by the waves when it stood higher, long ago, and it has sunk since. Each stands in its own cell of a
 * grid, wherever that holds deep, open ocean.
 *
 * @param wobble roughens their slopes
 */
public record Seamounts(int seed, float frequency, float shoreline, Noise wobble, Levels levels, SiteCache<Mount> mounts) implements Landform {
	private static final float GRID = 700.0F;
	private static final float OPEN_SEA = 0.2F;
	private static final int MIN_DEPTH = 24;
	private static final float GUYOT_CHANCE = 0.4F;
	private static final Mount NONE = new Mount(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false);

	/**
	 * @param top the height of its peak, or its flat top, and floor that of the sea floor around it
	 */
	record Mount(float x, float z, float radius, float top, float floor, boolean guyot) {
	}

	public static Seamounts make(int seed, OceanSettings.Seamounts settings, float shoreline, Levels levels) {
		return new Seamounts(seed, NoiseUtil.clamp(settings.frequency, 0.0F, 1.0F), shoreline, Noises.perlin(seed + 1, 30, 2), levels, SiteCache.make());
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.height >= this.levels.water(-6) || cell.continentEdge > this.shoreline - OPEN_SEA * 0.5F) {
			return;
		}
		int gridX = NoiseUtil.floor(x / GRID);
		int gridZ = NoiseUtil.floor(z / GRID);
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (Landform.random(this.seed, gx, gz, 0) >= this.frequency * 0.5F) {
					continue;
				}
				Mount mount = this.mounts.get(gx, gz, (mx, mz) -> this.find(mx, mz, heightmap));
				if (mount != NONE) {
					this.raise(cell, x, z, mount);
				}
			}
		}
	}

	private void raise(Cell cell, float x, float z, Mount mount) {
		float dx = x - mount.x();
		float dz = z - mount.z();
		float d = (float) Math.sqrt(dx * dx + dz * dz) / mount.radius() + (this.wobble.compute(x, z, 0) - 0.5F) * 0.25F;
		if (d >= 1.0F) {
			return;
		}
		float height;
		if (mount.guyot()) {
			// steep sides up to a broad, flat top
			height = NoiseUtil.lerp(mount.top(), mount.floor(), Landform.smoothstep(d, 0.55F, 1.0F));
		} else {
			height = NoiseUtil.lerp(mount.top(), mount.floor(), (float) Math.pow(d, 0.7F));
		}
		if (height > cell.height) {
			cell.height = height;
			cell.erosionMask = true;
			if (height > mount.floor() + 6.0F * this.levels.unit) {
				cell.terrain = mount.guyot() ? TerrainType.GUYOT : TerrainType.SEAMOUNT;
			}
		}
	}

	private Mount find(int gridX, int gridZ, Heightmap heightmap) {
		float margin = 180.0F;
		float x = gridX * GRID + margin + Landform.random(this.seed, gridX, gridZ, 1) * (GRID - margin * 2.0F);
		float z = gridZ * GRID + margin + Landform.random(this.seed, gridX, gridZ, 2) * (GRID - margin * 2.0F);
		Cell ground = heightmap.sampleGround(x, z);
		if (ground.continentEdge > this.shoreline - OPEN_SEA || ground.height > this.levels.water(-MIN_DEPTH)) {
			return NONE;
		}
		float radius = NoiseUtil.lerp(70.0F, 170.0F, Landform.random(this.seed, gridX, gridZ, 3));
		boolean guyot = Landform.random(this.seed, gridX, gridZ, 4) < GUYOT_CHANCE;
		// well under the surface: a guyot's top a little deeper and flat
		float below = guyot ? NoiseUtil.lerp(10.0F, 18.0F, Landform.random(this.seed, gridX, gridZ, 5)) : NoiseUtil.lerp(6.0F, 20.0F, Landform.random(this.seed, gridX, gridZ, 5));
		return new Mount(x, z, radius, this.levels.water - below * this.levels.unit, ground.height, guyot);
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Seamounts(this.seed, this.frequency, this.shoreline, this.wobble.mapAll(visitor), this.levels, this.mounts);
	}
}
