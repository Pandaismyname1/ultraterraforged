package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import com.pandaismyname1.ultraterraforged.data.preset.settings.LandformSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainCategory;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Outcrops of bare rock on hilltops, like the tors of Dartmoor or the kopjes of the savanna: a few blocky stacks of
 * rock slabs, each slab a little smaller than the one below, with boulders strewn over the ground around them. Each
 * tor sits in its own cell of a grid, on the highest ground near the cell's chosen spot, if that's a hilltop.
 *
 * @param wobble roughens the edges of the slabs
 */
public record Tors(int seed, float density, float height, Noise wobble, Levels levels, ThreadLocal<Long2ObjectOpenHashMap<Site>> sites) implements Landform {
	private static final float GRID = 128.0F;
	// the chosen spot stays this far inside its grid cell, so that the tor, moved to the highest ground nearby, and its
	// boulders stay inside it too
	private static final float MARGIN = 48.0F;
	private static final int MAX_STACKS = 3;
	// each slab is this much narrower than the one below it, as a share of the stack's radius
	private static final float SLAB_STEP = 0.13F;
	// boulders are strewn out to this many times the main stack's radius
	private static final float BOULDER_REACH = 3.0F;
	private static final float BOULDER_CHANCE = 0.3F;
	// tors stand on hilltops at least this high above the sea
	private static final float MIN_HEIGHT = 12.0F;
	private static final float HILL_RADIUS = 24.0F;
	// how far from the chosen spot the highest ground is looked for
	private static final float SEARCH_RADIUS = 16.0F;
	private static final Site ABSENT = new Site(0.0F, 0.0F, 0.0F);

	// where a tor stands and the height of the ground there
	private record Site(float x, float z, float ground) {
	}

	public static Tors make(int seed, LandformSettings.Tors settings, Levels levels) {
		return new Tors(seed, settings.density, settings.height, Noises.perlin(seed + 1, 6, 1), levels, ThreadLocal.withInitial(Long2ObjectOpenHashMap::new));
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.terrain.isSubmerged() || cell.terrain.isRiver() || cell.riverDistance < 0.3F) {
			return;
		}
		int gridX = NoiseUtil.floor(x / GRID);
		int gridZ = NoiseUtil.floor(z / GRID);
		if (random(this.seed, gridX, gridZ, 0) >= this.density) {
			return;
		}
		Site site = this.siteAt(gridX, gridZ, heightmap);
		if (site == ABSENT) {
			return;
		}
		float mainRadius = NoiseUtil.lerp(4.0F, 9.0F, random(this.seed, gridX, gridZ, 3));
		// the boulders reach further than the other stacks
		float reach = mainRadius * BOULDER_REACH + 2.0F;
		float dx = x - site.x();
		float dz = z - site.z();
		if (dx * dx + dz * dz > reach * reach) {
			return;
		}
		float ground = site.ground();
		// the slabs of each stack, measured up from the ground at the tor's centre
		float wobble = this.wobble.compute(x, z, 0) * 0.12F;
		float top = 0.0F;
		int stacks = 1 + (int) (random(this.seed, gridX, gridZ, 4) * MAX_STACKS);
		for (int i = 0; i < stacks; i++) {
			top = Math.max(top, this.stack(dx, dz, wobble, mainRadius, gridX, gridZ, i));
		}
		if (top > 0.0F) {
			float height = ground + top * this.levels.unit;
			if (height > cell.height) {
				cell.height = height;
			}
			cell.terrain = TerrainType.TOR;
			cell.erosionMask = true;
			return;
		}
		// boulders a block or two high, each two blocks across, thinning out away from the tor
		float distance = (float) Math.sqrt(dx * dx + dz * dz) / mainRadius;
		float chance = BOULDER_CHANCE * Math.max(0.0F, 1.0F - (distance - 1.0F) / (BOULDER_REACH - 1.0F));
		int boulderX = NoiseUtil.floor(x / 2.0F);
		int boulderZ = NoiseUtil.floor(z / 2.0F);
		if (chance > 0.0F && random(this.seed + 7, boulderX, boulderZ, 0) < chance) {
			cell.height += (random(this.seed + 7, boulderX, boulderZ, 1) < 0.35F ? 2.0F : 1.0F) * this.levels.unit;
			cell.terrain = TerrainType.TOR;
			cell.erosionMask = true;
		}
	}

	// how many blocks above the ground this stack stands here, or 0 outside it
	private float stack(float dx, float dz, float wobble, float mainRadius, int gridX, int gridZ, int index) {
		int purpose = 10 + index * 8;
		float radius = mainRadius * (index == 0 ? 1.0F : NoiseUtil.lerp(0.5F, 0.8F, random(this.seed, gridX, gridZ, purpose)));
		float offsetX = 0.0F;
		float offsetZ = 0.0F;
		if (index > 0) {
			float angle = random(this.seed, gridX, gridZ, purpose + 1) * NoiseUtil.PI2;
			float offset = mainRadius + radius * NoiseUtil.lerp(0.3F, 1.2F, random(this.seed, gridX, gridZ, purpose + 2));
			offsetX = NoiseUtil.cos(angle) * offset;
			offsetZ = NoiseUtil.sin(angle) * offset;
		}
		float height = this.height * NoiseUtil.lerp(0.5F, 1.0F, random(this.seed, gridX, gridZ, purpose + 3)) * (index == 0 ? 1.0F : 0.7F);
		float slab = NoiseUtil.lerp(2.0F, 3.5F, random(this.seed, gridX, gridZ, purpose + 4));
		int slabs = Math.max(1, Math.round(height / slab));
		// a blocky oval, turned any way
		float angle = random(this.seed, gridX, gridZ, purpose + 5) * NoiseUtil.PI2;
		float aspect = NoiseUtil.lerp(0.6F, 1.0F, random(this.seed, gridX, gridZ, purpose + 6));
		float cos = NoiseUtil.cos(angle);
		float sin = NoiseUtil.sin(angle);
		float px = dx - offsetX;
		float pz = dz - offsetZ;
		float u = (px * cos + pz * sin) / radius;
		float v = (pz * cos - px * sin) / (radius * aspect);
		float distance = (float) Math.sqrt(Math.sqrt(u * u * u * u + v * v * v * v)) + wobble;
		// the highest slab that reaches out this far
		int covering = Math.min(slabs, NoiseUtil.floor((1.0F - distance) / SLAB_STEP) + 1);
		return covering <= 0 ? 0.0F : covering * slab;
	}

	// where the tor of this grid cell stands, or ABSENT if there's no hilltop for it; remembered for the cells around it
	private Site siteAt(int gridX, int gridZ, Heightmap heightmap) {
		Long2ObjectOpenHashMap<Site> sites = this.sites.get();
		long key = ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
		Site site = sites.get(key);
		if (site != null) {
			return site;
		}
		if (sites.size() > 4096) {
			sites.clear();
		}
		site = this.findSite(gridX, gridZ, heightmap);
		sites.put(key, site);
		return site;
	}

	private Site findSite(int gridX, int gridZ, Heightmap heightmap) {
		float free = GRID - MARGIN * 2.0F;
		float spotX = gridX * GRID + MARGIN + random(this.seed, gridX, gridZ, 1) * free;
		float spotZ = gridZ * GRID + MARGIN + random(this.seed, gridX, gridZ, 2) * free;
		// the highest ground near the chosen spot
		float bestX = spotX;
		float bestZ = spotZ;
		float best = heightmap.sampleGround(spotX, spotZ).height;
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4.0D;
			float sampleX = spotX + (float) Math.cos(angle) * SEARCH_RADIUS;
			float sampleZ = spotZ + (float) Math.sin(angle) * SEARCH_RADIUS;
			float height = heightmap.sampleGround(sampleX, sampleZ).height;
			if (height > best) {
				best = height;
				bestX = sampleX;
				bestZ = sampleZ;
			}
		}
		Cell center = heightmap.sampleTerrain(bestX, bestZ);
		boolean stands = suits(center.terrain) && center.riverDistance > 0.5F && center.height > this.levels.water + MIN_HEIGHT * this.levels.unit && this.hilltop(center.height, bestX, bestZ, heightmap);
		return stands ? new Site(bestX, bestZ, center.height) : ABSENT;
	}

	// rolling land, not the badlands and plateaus where buttes stand, nor mountains that are rock already
	private static boolean suits(Terrain terrain) {
		TerrainCategory category = terrain.getCategory();
		boolean rolling = category == TerrainCategory.FLATLAND || category == TerrainCategory.LOWLAND || category == TerrainCategory.COAST;
		return rolling && !terrain.isVolcano() && !terrain.includes(TerrainType.BADLANDS) && !terrain.includes(TerrainType.PLATEAU);
	}

	// no higher ground around, and most of it falling away
	private boolean hilltop(float height, float x, float z, Heightmap heightmap) {
		int higher = 0;
		int lower = 0;
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4.0D;
			float around = heightmap.sampleGround(x + (float) Math.cos(angle) * HILL_RADIUS, z + (float) Math.sin(angle) * HILL_RADIUS).height;
			if (around > height + this.levels.unit) {
				higher++;
			} else if (around < height - 2.0F * this.levels.unit) {
				lower++;
			}
		}
		return higher <= 2 && lower >= 3;
	}

	// 0 to 1, fixed for each grid cell and purpose
	private static float random(int seed, int gridX, int gridZ, int purpose) {
		return (NoiseUtil.valCoord2D(seed + purpose * 1013, gridX, gridZ) + 1.0F) * 0.5F;
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Tors(this.seed, this.density, this.height, this.wobble.mapAll(visitor), this.levels, this.sites);
	}
}
