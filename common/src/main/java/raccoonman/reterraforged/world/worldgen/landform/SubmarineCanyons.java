package raccoonman.reterraforged.world.worldgen.landform;

import raccoonman.reterraforged.data.preset.settings.OceanSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.continent.Continent;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Submarine canyons: deep, winding canyons cut into the continental shelf, starting near the coast and running out
 * over the drop-off, deepening as they go, then fading out on the deep sea floor. Each starts in its own cell of a
 * grid, wherever that cell holds a stretch of coast.
 */
public record SubmarineCanyons(int seed, float frequency, float shoreline, Levels levels, SiteCache<Canyon> canyons) implements Landform {
	private static final float GRID = 800.0F;
	// their heads lie this far out to sea, in continent values
	private static final float HEAD = 0.02F;
	private static final float MAX_DEPTH = 30.0F;
	private static final Canyon NONE = new Canyon(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

	/**
	 * @param dirX the way out to sea
	 */
	record Canyon(float x, float z, float dirX, float dirZ, float length, float headWidth, float footWidth, float phase) {
	}

	public static SubmarineCanyons make(int seed, OceanSettings.SubmarineCanyons settings, float shoreline, Levels levels) {
		return new SubmarineCanyons(seed, NoiseUtil.clamp(settings.frequency, 0.0F, 1.0F), shoreline, levels, SiteCache.make());
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.height >= this.levels.water || cell.continentEdge > this.shoreline || cell.terrain.isRiver() || cell.terrain.isLake()) {
			return;
		}
		int gridX = NoiseUtil.floor(x / GRID);
		int gridZ = NoiseUtil.floor(z / GRID);
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (Landform.random(this.seed, gx, gz, 0) >= this.frequency) {
					continue;
				}
				Canyon canyon = this.canyons.get(gx, gz, (cx, cz) -> this.find(cx, cz, heightmap.continent()));
				if (canyon != NONE) {
					this.cut(cell, x, z, canyon);
				}
			}
		}
	}

	private void cut(Cell cell, float x, float z, Canyon canyon) {
		float dx = x - canyon.x();
		float dz = z - canyon.z();
		float along = dx * canyon.dirX() + dz * canyon.dirZ();
		if (along < -20.0F || along > canyon.length()) {
			return;
		}
		float u = Math.max(0.0F, along) / canyon.length();
		// winding
		float across = dx * -canyon.dirZ() + dz * canyon.dirX() - NoiseUtil.sin(along / 70.0F + canyon.phase()) * 16.0F * u;
		float width = NoiseUtil.lerp(canyon.headWidth(), canyon.footWidth(), u);
		float inside = 1.0F - (across * across) / (width * width);
		if (inside <= 0.0F) {
			return;
		}
		float depth = MAX_DEPTH * Landform.smoothstep(u, 0.0F, 0.5F) * (1.0F - Landform.smoothstep(u, 0.8F, 1.0F)) + 5.0F * (1.0F - Landform.smoothstep(u, 0.8F, 1.0F));
		// a rounded head
		depth *= Landform.smoothstep(along, -20.0F, 10.0F);
		cell.height -= depth * (float) Math.sqrt(inside) * this.levels.unit;
		cell.erosionMask = true;
		if (inside > 0.3F && !cell.terrain.isDeepOcean()) {
			cell.terrain = TerrainType.SUBMARINE_CANYON;
		}
	}

	private Canyon find(int gridX, int gridZ, Continent continent) {
		float x = (gridX + 0.2F + Landform.random(this.seed, gridX, gridZ, 1) * 0.6F) * GRID;
		float z = (gridZ + 0.2F + Landform.random(this.seed, gridX, gridZ, 2) * 0.6F) * GRID;
		float target = this.shoreline - HEAD;
		// towards the place just off the coast where the head lies
		for (int i = 0; i < 6; i++) {
			float edge = continent.getEdgeValue(x, z);
			if (edge < target - 0.3F || edge > target + 0.3F) {
				return NONE;
			}
			float gx = (continent.getEdgeValue(x + 32.0F, z) - continent.getEdgeValue(x - 32.0F, z)) / 64.0F;
			float gz = (continent.getEdgeValue(x, z + 32.0F) - continent.getEdgeValue(x, z - 32.0F)) / 64.0F;
			float slope2 = gx * gx + gz * gz;
			if (slope2 < 1.0E-12F) {
				return NONE;
			}
			if (Math.abs(edge - target) < 0.003F) {
				float slope = (float) Math.sqrt(slope2);
				float length = NoiseUtil.lerp(350.0F, 800.0F, Landform.random(this.seed, gridX, gridZ, 3));
				// out to open sea, not across a bay
				if (continent.getEdgeValue(x - gx / slope * length, z - gz / slope * length) > this.shoreline - 0.05F) {
					return NONE;
				}
				return new Canyon(x, z, -gx / slope, -gz / slope, length, NoiseUtil.lerp(8.0F, 14.0F, Landform.random(this.seed, gridX, gridZ, 4)), NoiseUtil.lerp(24.0F, 40.0F, Landform.random(this.seed, gridX, gridZ, 5)), Landform.random(this.seed, gridX, gridZ, 6) * NoiseUtil.PI2);
			}
			float step = (target - edge) / slope2;
			float stepX = gx * step;
			float stepZ = gz * step;
			float length = (float) Math.sqrt(stepX * stepX + stepZ * stepZ);
			if (length > 300.0F) {
				stepX *= 300.0F / length;
				stepZ *= 300.0F / length;
			}
			x += stepX;
			z += stepZ;
		}
		return NONE;
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return this;
	}

}
