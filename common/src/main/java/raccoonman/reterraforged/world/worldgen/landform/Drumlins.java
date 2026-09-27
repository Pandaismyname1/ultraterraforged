package raccoonman.reterraforged.world.worldgen.landform;

import raccoonman.reterraforged.data.preset.settings.LandformSettings;
import raccoonman.reterraforged.world.worldgen.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * What the ice sheets left on cold lowlands: swarms of drumlins, smooth oval hills all lying the way the ice flowed,
 * blunt at the end it came from and tapering away from it; and moraines, long, low, winding ridges of rubble dropped
 * at the edge of the ice. The ice flowed the same way over the whole world, as it would away from one ice sheet. Both
 * only add to the ground.
 *
 * @param field where the drumlin swarms and moraines lie, above fieldLow
 * @param cosFlow the direction the ice flowed
 * @param moraine traces the moraines along lines where it's near its middle value
 */
public record Drumlins(int seed, float height, Noise field, float fieldLow, float fieldHigh, float cosFlow, float sinFlow, Noise moraine, Noise detail, Levels levels, ClimateGrid climate) implements Landform {
	// drumlins are laid out on a grid lined up with the ice flow, this long along it and this wide across it
	private static final float CELL_LENGTH = 90.0F;
	private static final float CELL_WIDTH = 40.0F;
	private static final float DRUMLIN_CHANCE = 0.75F;
	// moraine ridges are about this wide, in blocks
	private static final float MORAINE_WIDTH = 9.0F;
	// the moraine noise changes by about this much per block, to turn its values into distances
	private static final float MORAINE_GRADIENT = 1.0F / 420.0F;
	private static final float MORAINE_HEIGHT = 0.7F;

	public static Drumlins make(int seed, LandformSettings.Drumlins settings, Levels levels) {
		Noise field = Noises.perlin(seed, 520, 2);
		float density = NoiseUtil.clamp(settings.density, 0.0F, 1.0F);
		float fieldLow = density >= 1.0F ? Float.NEGATIVE_INFINITY : Landform.quantile(field, 1.0F - density);
		float fieldHigh = density >= 1.0F ? Float.NEGATIVE_INFINITY : Landform.quantile(field, Math.min(1.0F, 1.0F - density + 0.12F));
		float flow = (NoiseUtil.valCoord2D(seed + 1, 0, 0) + 1.0F) * (float) Math.PI;
		return new Drumlins(seed, settings.height, field, fieldLow, fieldHigh, NoiseUtil.cos(flow), NoiseUtil.sin(flow), Noises.perlin(seed + 2, 300, 2), Noises.perlin(seed + 3, 24, 1), levels, ClimateGrid.of(64.0F, BiomeType.TUNDRA, BiomeType.TAIGA, BiomeType.COLD_STEPPE));
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (!Landform.isRollingLand(cell.terrain) || cell.terrain == TerrainType.TOR) {
			return;
		}
		// not on the shore or by rivers
		float mask = Landform.smoothstep((cell.height - this.levels.water) * this.levels.worldHeight, 2.0F, 6.0F);
		mask *= Landform.smoothstep(cell.riverDistance, 0.3F, 0.6F);
		if (mask <= 0.0F) {
			return;
		}
		if (this.fieldHigh != Float.NEGATIVE_INFINITY) {
			mask *= Landform.smoothstep(this.field.compute(x, z, 0), this.fieldLow, this.fieldHigh);
			if (mask <= 0.0F) {
				return;
			}
		}
		mask *= Landform.smoothstep(this.climate.get(x, z, heightmap), 0.5F, 1.0F);
		if (mask <= 0.0F) {
			return;
		}
		// along and across the flow
		float u = x * this.cosFlow + z * this.sinFlow;
		float v = z * this.cosFlow - x * this.sinFlow;
		float drumlin = this.drumlin(u, v);
		float moraine = this.moraine(x, z);
		float rise = Math.max(drumlin, moraine) * this.height * mask;
		if (rise <= 0.0F) {
			return;
		}
		cell.height += rise * this.levels.unit;
		if (rise > 1.5F) {
			cell.terrain = moraine > drumlin ? TerrainType.MORAINE : TerrainType.DRUMLINS;
		}
	}

	// 0 to 1: the highest drumlin here, from the cells of the flow-aligned grid around
	private float drumlin(float u, float v) {
		// every other row is shifted half a cell, so they're staggered
		int row = NoiseUtil.floor(v / CELL_WIDTH);
		float best = 0.0F;
		for (int r = row - 1; r <= row + 1; r++) {
			float shift = (r & 1) == 0 ? 0.0F : CELL_LENGTH * 0.5F;
			int column = NoiseUtil.floor((u - shift) / CELL_LENGTH);
			for (int c = column - 1; c <= column + 1; c++) {
				if (Landform.random(this.seed, c, r, 10) >= DRUMLIN_CHANCE) {
					continue;
				}
				float centerU = shift + (c + 0.5F) * CELL_LENGTH + (Landform.random(this.seed, c, r, 11) - 0.5F) * CELL_LENGTH * 0.3F;
				float centerV = (r + 0.5F) * CELL_WIDTH + (Landform.random(this.seed, c, r, 12) - 0.5F) * CELL_WIDTH * 0.3F;
				float length = NoiseUtil.lerp(0.3F, 0.5F, Landform.random(this.seed, c, r, 13)) * CELL_LENGTH;
				float width = NoiseUtil.lerp(0.25F, 0.4F, Landform.random(this.seed, c, r, 14)) * CELL_WIDTH;
				float du = u - centerU;
				// blunt where the ice came from, tapering away downstream
				float along = du / (du < 0.0F ? length * 0.65F : length);
				float across = (v - centerV) / width;
				float distance = along * along + across * across;
				if (distance < 1.0F) {
					float size = NoiseUtil.lerp(0.55F, 1.0F, Landform.random(this.seed, c, r, 15));
					// a smooth dome
					float t = 1.0F - distance;
					best = Math.max(best, size * t * t * (3.0F - 2.0F * t));
				}
			}
		}
		return best;
	}

	// 0 to 1: a winding ridge where the moraine noise is near its middle
	private float moraine(float x, float z) {
		float value = this.moraine.compute(x, z, 0);
		float distance = Math.abs(value - 0.5F) / MORAINE_GRADIENT;
		if (distance >= MORAINE_WIDTH) {
			return 0.0F;
		}
		float t = 1.0F - distance / MORAINE_WIDTH;
		// hummocky along its length
		float hummocks = NoiseUtil.lerp(0.55F, 1.0F, this.detail.compute(x, z, 0));
		return MORAINE_HEIGHT * hummocks * t * t * (3.0F - 2.0F * t);
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Drumlins(this.seed, this.height, this.field.mapAll(visitor), this.fieldLow, this.fieldHigh, this.cosFlow, this.sinFlow, this.moraine.mapAll(visitor), this.detail.mapAll(visitor), this.levels, this.climate);
	}
}
