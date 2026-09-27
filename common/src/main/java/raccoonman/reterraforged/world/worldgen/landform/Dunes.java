package raccoonman.reterraforged.world.worldgen.landform;

import raccoonman.reterraforged.data.preset.settings.LandformSettings;
import raccoonman.reterraforged.world.worldgen.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainCategory;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Fields of sand dunes in hot deserts, away from the badlands and plateaus where canyons and buttes stand: long, wavy
 * crests across the wind, each with a gentle slope on the side the wind comes from and a steep face on the other. The
 * wind blows the same way over the whole world, as a prevailing wind would. Dunes only add to the ground, so they never
 * dig below it.
 *
 * @param field where the dune fields lie, between its values fieldLow and fieldHigh
 * @param warp bends the crests
 * @param crest how high the crests rise along their length, between crestLow and crestHigh, so they break up
 */
public record Dunes(float height, float cosWind, float sinWind, Noise field, float fieldLow, float fieldHigh, Noise warp, Noise detail, Noise crest, float crestLow, float crestHigh, Levels levels, ClimateGrid climate) implements Landform {
	// blocks from one crest to the next
	private static final float WAVELENGTH = 56.0F;
	// the share of the way from one crest to the next taken by the gentle slope; the steep face takes the rest
	private static final float WINDWARD = 0.72F;
	private static final float WARP = 70.0F;
	private static final float DETAIL = 12.0F;

	public static Dunes make(int seed, LandformSettings.Dunes settings, Levels levels) {
		float wind = (NoiseUtil.valCoord2D(seed, 0, 0) + 1.0F) * (float) Math.PI;
		Noise field = Noises.perlin(seed + 1, 700, 2);
		float coverage = NoiseUtil.clamp(settings.coverage, 0.0F, 1.0F);
		float fieldLow = coverage >= 1.0F ? Float.NEGATIVE_INFINITY : Landform.quantile(field, 1.0F - coverage);
		float fieldHigh = coverage >= 1.0F ? Float.NEGATIVE_INFINITY : Landform.quantile(field, Math.min(1.0F, 1.0F - coverage + 0.1F));
		Noise crest = Noises.perlin(seed + 4, 140, 2);
		return new Dunes(settings.height, NoiseUtil.cos(wind), NoiseUtil.sin(wind), field, fieldLow, fieldHigh, Noises.perlin(seed + 2, 240, 2), Noises.perlin(seed + 3, 60, 1), crest, Landform.quantile(crest, 0.15F), Landform.quantile(crest, 0.85F), levels, ClimateGrid.of(64.0F, BiomeType.DESERT));
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.terrain.isRiver() || cell.terrain.isLake() || cell.terrain.isWetland() || cell.terrain.isVolcano() || cell.terrain.includes(TerrainType.BADLANDS) || cell.terrain.includes(TerrainType.PLATEAU)) {
			return;
		}
		TerrainCategory category = cell.terrain.getCategory();
		if (category != TerrainCategory.FLATLAND && category != TerrainCategory.LOWLAND) {
			return;
		}
		// not on the beach or along rivers
		float mask = Landform.smoothstep((cell.height - this.levels.water) * this.levels.worldHeight, 3.0F, 10.0F);
		mask *= Landform.smoothstep(cell.riverDistance, 0.3F, 0.65F);
		if (mask <= 0.0F) {
			return;
		}
		if (this.fieldHigh != Float.NEGATIVE_INFINITY) {
			mask *= Landform.smoothstep(this.field.compute(x, z, 0), this.fieldLow, this.fieldHigh);
			if (mask <= 0.0F) {
				return;
			}
		}
		// fading out well inside the desert, since the climate grid only roughly follows its edge
		mask *= Landform.smoothstep(this.climate.get(x, z, heightmap), 0.5F, 1.0F);
		if (mask <= 0.0F) {
			return;
		}
		// how far downwind, bent so the crests wave
		float downwind = x * this.cosWind + z * this.sinWind + this.warp.compute(x, z, 0) * WARP + this.detail.compute(x, z, 0) * DETAIL;
		float phase = downwind / WAVELENGTH;
		phase -= NoiseUtil.floor(phase);
		float profile = phase < WINDWARD ? Landform.smoothstep(phase, 0.0F, WINDWARD) : 1.0F - Landform.smoothstep(phase, WINDWARD, 1.0F);
		float crest = NoiseUtil.lerp(0.25F, 1.0F, Landform.smoothstep(this.crest.compute(x, z, 0), this.crestLow, this.crestHigh));
		cell.height += this.height * this.levels.unit * mask * crest * profile;
		// keeps the slopes sand rather than bare rock, and the erosion filters off the crests
		if (mask > 0.05F) {
			cell.terrain = TerrainType.DUNES;
		}
		cell.erosionMask = true;
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Dunes(this.height, this.cosWind, this.sinWind, this.field.mapAll(visitor), this.fieldLow, this.fieldHigh, this.warp.mapAll(visitor), this.detail.mapAll(visitor), this.crest.mapAll(visitor), this.crestLow, this.crestHigh, this.levels, this.climate);
	}
}
