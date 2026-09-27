package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import java.util.List;

import com.pandaismyname1.ultraterraforged.data.preset.settings.LandformSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.Rivermap;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * River deltas: where a main river meets the sea on a low, flat coast, the mud and sand it carries builds out into the
 * sea as a fan of low, marshy islands, and the river splits into channels that spread across it. Worked out in the
 * rivers' own space, where they're laid out as straight lines, from where each river's line crosses the shore.
 *
 * @param lobe wobbles the fan's seaward edge
 * @param meander bends the channels
 */
public record Deltas(int seed, float size, float shoreline, Noise lobe, Noise meander, Noise marsh, Levels levels, SiteCache<Site> sites) implements Landform {
	private static final float RADIUS = 125.0F;
	// the fan starts this far inland from the shore
	private static final float INLAND = 36.0F;
	// and spreads to this angle either side of the river's line, as its cosine: fully up to the first, fading out by the
	// second
	private static final float CORE_COS = 0.62F;
	private static final float EDGE_COS = 0.4F;
	private static final int MIN_CHANNELS = 3;
	private static final int MAX_CHANNELS = 6;
	// the channels spread to this angle either side, in radians
	private static final float SPREAD = 0.75F;
	// the land by the shore is no higher than this many blocks, or there's no delta
	private static final float MAX_LAND = 8.0F;
	private static final float CHANNEL_DEPTH = 3.0F;
	// the sea floor beyond the islands shelves this far out, as a share of the radius
	private static final float SHELF = 0.35F;
	// continent edge values past this are far from the open sea
	private static final float INLAND_EDGE = 0.3F;
	private static final Site ABSENT = new Site(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new float[0]);

	/**
	 * @param x where the river crosses the shore, in the rivers' space
	 * @param dirX the way the river flows, out to sea
	 * @param width the river's width either side of its middle
	 * @param channels the angle each channel heads off at
	 */
	record Site(float x, float z, float dirX, float dirZ, float radius, float width, float[] channels) {
	}

	public static Deltas make(int seed, LandformSettings.Deltas settings, float shoreline, Levels levels) {
		return new Deltas(seed, Math.max(0.1F, settings.size), shoreline, Noises.perlin(seed, 40, 2), Noises.perlin(seed + 1, 60, 2), Noises.perlin(seed + 2, 18, 2), levels, SiteCache.make());
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.continentEdge > this.shoreline + INLAND_EDGE || cell.terrain.isLake() || cell.terrain.isVolcano()) {
			return;
		}
		Rivermap rivermap = Rivermap.get(cell, null, heightmap);
		List<Rivermap.Mouth> mouths = rivermap.mouths();
		if (mouths.isEmpty()) {
			return;
		}
		float rx = rivermap.riverX(x, z);
		float rz = rivermap.riverZ(x, z);
		float reach = RADIUS * this.size * (1.0F + SHELF) + INLAND + 32.0F;
		for (Rivermap.Mouth mouth : mouths) {
			float dx = rx - mouth.x();
			float dz = rz - mouth.z();
			// the shore lies somewhere along the river's last stretch
			float along = dx * mouth.dirX() + dz * mouth.dirZ();
			float across = dx * mouth.dirZ() - dz * mouth.dirX();
			if (along > reach || along < -mouth.length() || Math.abs(across) > reach) {
				continue;
			}
			Site site = this.sites.get(NoiseUtil.floor(mouth.x()), NoiseUtil.floor(mouth.z()), (mx, mz) -> this.find(mouth, rivermap, heightmap));
			if (site != ABSENT) {
				this.shape(cell, rx, rz, x, z, site);
			}
		}
	}

	private void shape(Cell cell, float rx, float rz, float x, float z, Site site) {
		// from the fan's apex, inland of the shore
		float apexX = site.x() - site.dirX() * INLAND;
		float apexZ = site.z() - site.dirZ() * INLAND;
		float dx = rx - apexX;
		float dz = rz - apexZ;
		float distance = (float) Math.sqrt(dx * dx + dz * dz);
		float edge = site.radius() * NoiseUtil.lerp(0.75F, 1.05F, this.lobe.compute(x, z, 0));
		if (distance >= edge * (1.0F + SHELF) || distance < 1.0F) {
			return;
		}
		float along = dx * site.dirX() + dz * site.dirZ();
		float fan = Landform.smoothstep(along / distance, EDGE_COS, CORE_COS);
		if (fan <= 0.0F) {
			return;
		}
		// hills by the shore are left alone
		float low = 1.0F - Landform.smoothstep((cell.height - this.levels.water) * this.levels.worldHeight, MAX_LAND * 0.5F, MAX_LAND);
		float weight = fan * low;
		if (weight <= 0.0F) {
			return;
		}
		float across = dx * site.dirZ() - dz * site.dirX();
		float angle = (float) Math.atan2(across, along);
		boolean channel = false;
		float spread = Landform.smoothstep(distance, 0.0F, site.radius() * 0.45F);
		float meander = (this.meander.compute(x, z, 0) - 0.5F) * 0.35F;
		for (float heading : site.channels()) {
			float width = NoiseUtil.lerp(site.width(), 2.5F, distance / site.radius());
			if (Math.abs(angle - heading * spread - meander * spread) * distance < width) {
				channel = true;
				break;
			}
		}
		if (distance > edge) {
			// the sea floor shelving away beyond the islands
			float shelf = this.levels.water(-(int) CHANNEL_DEPTH);
			if (cell.height < shelf) {
				cell.height = NoiseUtil.lerp(shelf, cell.height, Landform.smoothstep(distance, edge, edge * (1.0F + SHELF)) * weight + (1.0F - weight));
			}
			return;
		}
		float target = channel ? this.levels.water(-(int) CHANNEL_DEPTH) : this.levels.water + NoiseUtil.lerp(0.6F, 1.6F, this.marsh.compute(x, z, 0)) * this.levels.unit;
		// the islands sink into the sea at the fan's edge
		if (!channel) {
			target = NoiseUtil.lerp(target, this.levels.water(-1), Landform.smoothstep(distance, edge * 0.94F, edge));
		}
		cell.height = NoiseUtil.lerp(cell.height, target, weight);
		if (weight > 0.5F) {
			cell.terrain = TerrainType.DELTA;
			cell.erosionMask = true;
		}
	}

	private Site find(Rivermap.Mouth mouth, Rivermap rivermap, Heightmap heightmap) {
		// back up the river from its mouth to where it crosses the shore
		float step = 12.0F;
		float shoreX = Float.NaN;
		float shoreZ = Float.NaN;
		boolean seaward = this.ground(mouth.x(), mouth.z(), rivermap, heightmap) < this.levels.water;
		if (!seaward) {
			return ABSENT;
		}
		for (float back = step; back < mouth.length(); back += step) {
			float px = mouth.x() - mouth.dirX() * back;
			float pz = mouth.z() - mouth.dirZ() * back;
			if (this.ground(px, pz, rivermap, heightmap) >= this.levels.water) {
				shoreX = px + mouth.dirX() * step * 0.5F;
				shoreZ = pz + mouth.dirZ() * step * 0.5F;
				break;
			}
		}
		if (Float.isNaN(shoreX)) {
			return ABSENT;
		}
		// on a low, flat coast
		for (float side : new float[] { -1.0F, 0.0F, 1.0F }) {
			float px = shoreX - mouth.dirX() * INLAND * 1.5F + mouth.dirZ() * side * 40.0F;
			float pz = shoreZ - mouth.dirZ() * INLAND * 1.5F - mouth.dirX() * side * 40.0F;
			if ((this.ground(px, pz, rivermap, heightmap) - this.levels.water) * this.levels.worldHeight > MAX_LAND) {
				return ABSENT;
			}
		}
		int gridX = NoiseUtil.floor(mouth.x());
		int gridZ = NoiseUtil.floor(mouth.z());
		int count = MIN_CHANNELS + (int) (Landform.random(this.seed, gridX, gridZ, 1) * (MAX_CHANNELS - MIN_CHANNELS + 1));
		float[] channels = new float[count];
		for (int i = 0; i < count; i++) {
			// spread evenly across the fan, each nudged a little
			float even = count == 1 ? 0.0F : (i / (float) (count - 1)) * 2.0F - 1.0F;
			channels[i] = (even + (Landform.random(this.seed, gridX, gridZ, 2 + i) - 0.5F) * 0.3F) * SPREAD;
		}
		float radius = RADIUS * this.size * NoiseUtil.lerp(0.8F, 1.2F, Landform.random(this.seed, gridX, gridZ, 20));
		float width = NoiseUtil.clamp(mouth.width() * 0.35F, 3.0F, 8.0F);
		return new Site(shoreX, shoreZ, mouth.dirX(), mouth.dirZ(), radius, width, channels);
	}

	// the ground at a place in the rivers' space, found in the world by undoing their warp
	private float ground(float x, float z, Rivermap rivermap, Heightmap heightmap) {
		float worldX = x;
		float worldZ = z;
		for (int i = 0; i < 3; i++) {
			worldX += x - rivermap.riverX(worldX, worldZ);
			worldZ += z - rivermap.riverZ(worldX, worldZ);
		}
		return heightmap.sampleGround(worldX, worldZ).height;
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Deltas(this.seed, this.size, this.shoreline, this.lobe.mapAll(visitor), this.meander.mapAll(visitor), this.marsh.mapAll(visitor), this.levels, this.sites);
	}
}
