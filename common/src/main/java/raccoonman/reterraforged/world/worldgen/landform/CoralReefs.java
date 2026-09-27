package raccoonman.reterraforged.world.worldgen.landform;

import raccoonman.reterraforged.data.preset.settings.OceanSettings;
import raccoonman.reterraforged.world.worldgen.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Coral reefs: off warm coasts, a band of reef a little way out from the shore, growing up almost to the surface, broken
 * into patches with channels between them. The surface rules cover them in coral.
 *
 * @param patches breaks the reef into patches, above threshold
 */
public record CoralReefs(float shoreline, Noise patches, float threshold, Noise crest, Levels levels, ClimateGrid climate) implements Landform {
	// the reef lies between these continent values out from the shoreline
	private static final float INNER = 0.012F;
	private static final float OUTER = 0.06F;
	private static final int MAX_DEPTH = 22;

	public static CoralReefs make(int seed, OceanSettings.CoralReefs settings, float shoreline, Levels levels) {
		Noise patches = Noises.perlin(seed, 40, 2);
		float coverage = NoiseUtil.clamp(settings.coverage, 0.0F, 1.0F);
		float threshold = coverage <= 0.0F ? Float.POSITIVE_INFINITY : Landform.quantile(patches, 1.0F - coverage * 0.7F);
		return new CoralReefs(shoreline, patches, threshold, Noises.perlin(seed + 1, 8, 1), levels, ClimateGrid.of(128.0F, BiomeType.TROPICAL_RAINFOREST, BiomeType.SAVANNA));
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.height >= this.levels.water(-1) || cell.height < this.levels.water(-MAX_DEPTH) || cell.terrain.isRiver() || cell.terrain.isLake()) {
			return;
		}
		float out = this.shoreline - cell.continentEdge;
		if (out < INNER * 0.5F || out > OUTER) {
			return;
		}
		float band = Landform.smoothstep(out, INNER * 0.5F, INNER) * (1.0F - Landform.smoothstep(out, OUTER * 0.7F, OUTER));
		float patch = Landform.smoothstep(this.patches.compute(x, z, 0), this.threshold, this.threshold + 0.06F);
		float mask = band * patch;
		if (mask <= 0.0F) {
			return;
		}
		mask *= Landform.smoothstep(this.climate.get(x, z, heightmap), 0.4F, 0.9F);
		if (mask <= 0.0F) {
			return;
		}
		// the crest a block or two under the surface
		float crest = this.levels.water - NoiseUtil.lerp(1.0F, 3.0F, this.crest.compute(x, z, 0)) * this.levels.unit;
		float height = NoiseUtil.lerp(cell.height, crest, mask);
		if (height > cell.height) {
			cell.height = height;
			cell.erosionMask = true;
			if (mask > 0.3F) {
				cell.terrain = TerrainType.CORAL_REEF;
			}
		}
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new CoralReefs(this.shoreline, this.patches.mapAll(visitor), this.threshold, this.crest.mapAll(visitor), this.levels, this.climate);
	}
}
