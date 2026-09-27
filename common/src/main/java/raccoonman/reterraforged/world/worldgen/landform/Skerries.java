package raccoonman.reterraforged.world.worldgen.landform;

import raccoonman.reterraforged.data.preset.settings.CoastSettings;
import raccoonman.reterraforged.world.worldgen.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Skerries: swarms of small, bare rocky islets in the shallow sea off cold coasts, as off Norway or Sweden, with
 * rocky shoals among them. Each islet stands in its own cell of a fine grid.
 *
 * @param field where the swarms lie, above fieldLow
 * @param wobble roughens the islets' outlines
 */
public record Skerries(int seed, float shoreline, Noise field, float fieldLow, float fieldHigh, Noise wobble, Levels levels, ClimateGrid climate) implements Landform {
	private static final float GRID = 20.0F;
	private static final float CHANCE = 0.5F;
	// continent values this far out from the shoreline get them
	private static final float REACH = 0.12F;
	// the sea there is no deeper than this many blocks
	private static final int MAX_DEPTH = 14;

	public static Skerries make(int seed, CoastSettings.Skerries settings, float shoreline, Levels levels) {
		Noise field = Noises.perlin(seed, 280, 2);
		float density = NoiseUtil.clamp(settings.density, 0.0F, 1.0F);
		float fieldLow = density >= 1.0F ? Float.NEGATIVE_INFINITY : Landform.quantile(field, 1.0F - density);
		float fieldHigh = density >= 1.0F ? Float.NEGATIVE_INFINITY : Landform.quantile(field, Math.min(1.0F, 1.0F - density + 0.1F));
		return new Skerries(seed, shoreline, field, fieldLow, fieldHigh, Noises.perlin(seed + 1, 5, 1), levels, ClimateGrid.of(96.0F, BiomeType.TUNDRA, BiomeType.TAIGA, BiomeType.COLD_STEPPE));
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.height >= this.levels.water || cell.height < this.levels.water(-MAX_DEPTH) || cell.continentEdge < this.shoreline - REACH || cell.continentEdge > this.shoreline || cell.terrain.isRiver() || cell.terrain.isLake()) {
			return;
		}
		float mask = 1.0F;
		if (this.fieldHigh != Float.NEGATIVE_INFINITY) {
			mask = Landform.smoothstep(this.field.compute(x, z, 0), this.fieldLow, this.fieldHigh);
			if (mask <= 0.0F) {
				return;
			}
		}
		mask *= Landform.smoothstep(this.climate.get(x, z, heightmap), 0.4F, 0.9F);
		if (mask <= 0.0F) {
			return;
		}
		int gridX = NoiseUtil.floor(x / GRID);
		int gridZ = NoiseUtil.floor(z / GRID);
		float best = 0.0F;
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (Landform.random(this.seed, gx, gz, 0) >= CHANCE * mask) {
					continue;
				}
				float radius = NoiseUtil.lerp(3.0F, 9.0F, Landform.random(this.seed, gx, gz, 3));
				float centerX = (gx + 0.5F) * GRID + (Landform.random(this.seed, gx, gz, 1) - 0.5F) * (GRID - radius);
				float centerZ = (gz + 0.5F) * GRID + (Landform.random(this.seed, gx, gz, 2) - 0.5F) * (GRID - radius);
				float dx = x - centerX;
				float dz = z - centerZ;
				float distance = (float) Math.sqrt(dx * dx + dz * dz) / radius + (this.wobble.compute(x, z, 0) - 0.5F) * 0.6F;
				if (distance < 1.3F) {
					// blocks above the sea: a rounded hump of rock, its foot a shoal just under the water
					float top = NoiseUtil.lerp(1.0F, 5.0F, Landform.random(this.seed, gx, gz, 4));
					float t = 1.0F - distance / 1.3F;
					best = Math.max(best, (top + 2.5F) * t * t - 2.0F);
				}
			}
		}
		if (best <= -2.0F) {
			return;
		}
		float height = this.levels.water + best * this.levels.unit;
		if (height > cell.height) {
			cell.height = height;
			cell.erosionMask = true;
			if (height >= this.levels.water) {
				cell.terrain = TerrainType.SKERRY;
			}
		}
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Skerries(this.seed, this.shoreline, this.field.mapAll(visitor), this.fieldLow, this.fieldHigh, this.wobble.mapAll(visitor), this.levels, this.climate);
	}
}
