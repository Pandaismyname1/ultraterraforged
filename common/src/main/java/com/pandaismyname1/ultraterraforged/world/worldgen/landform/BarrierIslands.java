package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import org.jetbrains.annotations.Nullable;

import com.pandaismyname1.ultraterraforged.data.preset.settings.LandformSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Barrier islands: long, thin islands of sand lying a little way off low, flat coasts, parallel to the shore and
 * broken here and there by inlets, with a calm, shallow lagoon between them and the mainland. How far a place lies
 * from the land, and how high that land is, are measured on a coarse grid, so the islands follow the coast's bends.
 *
 * @param coastline where along the coast islands form, above threshold
 * @param inlets where the islands are broken, above inletThreshold
 */
public record BarrierIslands(float shoreline, Noise coastline, float threshold, Noise inlets, float inletThreshold, Noise dunes, Levels levels, SampleGrid distance, SampleGrid landHeight, @Nullable SeaCliffs cliffs) implements Landform {
	private static final float GRID = 24.0F;
	// the distances out from a grid corner that land is looked for at
	private static final float[] RINGS = { 0.0F, 8.0F, 16.0F, 24.0F, 32.0F, 40.0F, 48.0F, 56.0F, 64.0F, 72.0F, 80.0F, 88.0F, 96.0F };
	private static final float NO_LAND = 1000.0F;
	// the island's middle lies this far from the land, and it's this wide
	private static final float OFFSHORE = 62.0F;
	private static final float HALF_WIDTH = 9.0F;
	// the sea floor there is no deeper than this many blocks
	private static final float MAX_DEPTH = 14.0F;
	// the land they lie off is no higher than this many blocks above the sea
	private static final float MAX_LAND = 10.0F;
	// the lagoon behind them is this deep, in blocks
	private static final float LAGOON_DEPTH = 3.0F;
	// continent edge values past this are far from the open sea
	private static final float INLAND = 0.25F;

	/**
	 * @param cliffs the sea cliffs, whose stretches of coast get no islands; null if there are none
	 */
	public static BarrierIslands make(int seed, LandformSettings.BarrierIslands settings, float shoreline, Levels levels, @Nullable SeaCliffs cliffs) {
		Noise coastline = Noises.perlin(seed, 1100, 2);
		float frequency = NoiseUtil.clamp(settings.frequency, 0.0F, 1.0F);
		float threshold = frequency <= 0.0F ? Float.POSITIVE_INFINITY : Landform.quantile(coastline, 1.0F - frequency);
		Noise inlets = Noises.perlin(seed + 1, 140, 1);
		return new BarrierIslands(shoreline, coastline, threshold, inlets, Landform.quantile(inlets, 0.85F), Noises.perlin(seed + 2, 20, 2), levels, SampleGrid.of(GRID), SampleGrid.of(GRID), cliffs);
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		// in the sea, not far off the land
		if (cell.height >= this.levels.water || cell.height < this.levels.water(-(int) MAX_DEPTH - 4) || cell.continentEdge > this.shoreline + INLAND || cell.terrain.isRiver() || cell.terrain.isLake()) {
			return;
		}
		float mask = Landform.smoothstep(this.coastline.compute(x, z, 0), this.threshold, this.threshold + 0.03F);
		if (this.cliffs != null) {
			mask *= 1.0F - this.cliffs.cliffMask(x, z);
		}
		if (mask <= 0.0F) {
			return;
		}
		float distance = this.distance.get(x, z, (sx, sz) -> this.landDistance(sx, sz, heightmap));
		if (distance > OFFSHORE + HALF_WIDTH * 2.0F) {
			return;
		}
		// only off low, flat land
		float land = this.landHeight.get(x, z, (sx, sz) -> this.nearestLandHeight(sx, sz, heightmap));
		mask *= 1.0F - Landform.smoothstep((land - this.levels.water) * this.levels.worldHeight, MAX_LAND * 0.6F, MAX_LAND);
		if (mask <= 0.0F) {
			return;
		}
		float offshore = Math.abs(distance - OFFSHORE);
		boolean inlet = this.inlets.compute(x, z, 0) > this.inletThreshold;
		if (offshore < HALF_WIDTH && !inlet && cell.height > this.levels.water(-(int) MAX_DEPTH)) {
			// the island: a low ridge of dunes along its middle, sloping to the sea on both sides
			float across = 1.0F - offshore / HALF_WIDTH;
			float ridge = 1.0F + across * NoiseUtil.lerp(1.0F, 3.5F, this.dunes.compute(x, z, 0));
			float island = this.levels.water + ridge * this.levels.unit * Landform.smoothstep(across, 0.0F, 0.35F);
			float height = NoiseUtil.lerp(cell.height, island, mask);
			if (height > cell.height) {
				cell.height = height;
				if (cell.height >= this.levels.water) {
					cell.terrain = TerrainType.BARRIER_ISLAND;
				}
				cell.erosionMask = true;
			}
		} else if (distance < OFFSHORE) {
			// the lagoon: its floor built up to a shallow, even depth
			float lagoon = this.levels.water(-(int) LAGOON_DEPTH);
			float fill = mask * Landform.smoothstep(OFFSHORE - distance, 0.0F, 12.0F);
			if (cell.height < lagoon) {
				cell.height = NoiseUtil.lerp(cell.height, lagoon, fill);
			}
			// with a floor of sand
			if (fill > 0.5F) {
				cell.terrain = TerrainType.LAGOON;
			}
		}
	}

	// roughly how far from the nearest land, in blocks
	private float landDistance(float x, float z, Heightmap heightmap) {
		for (float ring : RINGS) {
			for (int i = 0; i < (ring == 0.0F ? 1 : 12); i++) {
				double angle = i * Math.PI / 6.0D;
				if (heightmap.sampleGround(x + (float) Math.cos(angle) * ring, z + (float) Math.sin(angle) * ring).height >= this.levels.water) {
					return ring;
				}
			}
		}
		return NO_LAND;
	}

	// how high the nearest land stands
	private float nearestLandHeight(float x, float z, Heightmap heightmap) {
		for (float ring : RINGS) {
			float highest = Float.NEGATIVE_INFINITY;
			for (int i = 0; i < (ring == 0.0F ? 1 : 12); i++) {
				double angle = i * Math.PI / 6.0D;
				// a little further inland than the shore
				float reach = ring + 24.0F;
				float height = heightmap.sampleGround(x + (float) Math.cos(angle) * reach, z + (float) Math.sin(angle) * reach).height;
				if (height >= this.levels.water) {
					highest = Math.max(highest, height);
				}
			}
			if (highest != Float.NEGATIVE_INFINITY) {
				return highest;
			}
		}
		return this.levels.water;
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new BarrierIslands(this.shoreline, this.coastline.mapAll(visitor), this.threshold, this.inlets.mapAll(visitor), this.inletThreshold, this.dunes.mapAll(visitor), this.levels, this.distance, this.landHeight, this.cliffs);
	}
}
