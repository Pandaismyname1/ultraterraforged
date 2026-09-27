package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import com.pandaismyname1.ultraterraforged.data.preset.settings.OceanSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Ocean trenches: long, narrow, curving chasms plunging far below the deep ocean floor, with sheer walls and a flat
 * bottom, tapering out at both ends. Each follows an arc, in its own cell of a coarse grid.
 *
 * @param wobble roughens the walls
 */
public record Trenches(int seed, float frequency, float depth, float shoreline, Noise wobble, Levels levels, SiteCache<Arc> arcs) implements Landform {
	private static final float GRID = 3000.0F;
	// only in deep, open ocean
	private static final float OPEN_SEA = 0.22F;
	private static final int MIN_DEPTH = 22;
	private static final Arc NONE = new Arc(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

	/**
	 * @param start the angle it starts at, around its centre, and sweep how far it goes
	 */
	record Arc(float x, float z, float radius, float start, float sweep, float width) {
	}

	public static Trenches make(int seed, OceanSettings.Trenches settings, float shoreline, Levels levels) {
		return new Trenches(seed, NoiseUtil.clamp(settings.frequency, 0.0F, 1.0F), settings.depth, shoreline, Noises.perlin(seed + 1, 60, 2), levels, SiteCache.make());
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.height > this.levels.water(-MIN_DEPTH) || cell.continentEdge > this.shoreline - OPEN_SEA) {
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
				if (arc != NONE) {
					this.cut(cell, x, z, arc);
				}
			}
		}
	}

	private void cut(Cell cell, float x, float z, Arc arc) {
		float dx = x - arc.x();
		float dz = z - arc.z();
		float distance = (float) Math.sqrt(dx * dx + dz * dz);
		float off = Math.abs(distance - arc.radius());
		if (off > arc.width() * 1.5F) {
			return;
		}
		float angle = (float) Math.atan2(dz, dx) - arc.start();
		angle -= NoiseUtil.floor(angle / NoiseUtil.PI2) * NoiseUtil.PI2;
		if (angle > arc.sweep()) {
			return;
		}
		float along = angle / arc.sweep();
		float taper = Landform.smoothstep(along, 0.0F, 0.15F) * (1.0F - Landform.smoothstep(along, 0.85F, 1.0F));
		float width = arc.width() * taper * NoiseUtil.lerp(0.8F, 1.2F, this.wobble.compute(x, z, 0));
		if (width <= 0.0F || off >= width) {
			return;
		}
		// sheer walls down to a flat floor
		float inside = Landform.smoothstep(1.0F - off / width, 0.0F, 0.35F);
		cell.height -= this.depth * taper * inside * this.levels.unit;
		cell.erosionMask = true;
		if (inside > 0.5F) {
			cell.terrain = TerrainType.OCEAN_TRENCH;
		}
	}

	private Arc find(int gridX, int gridZ, Heightmap heightmap) {
		float x = (gridX + Landform.random(this.seed, gridX, gridZ, 1)) * GRID;
		float z = (gridZ + Landform.random(this.seed, gridX, gridZ, 2)) * GRID;
		float radius = NoiseUtil.lerp(1500.0F, 3000.0F, Landform.random(this.seed, gridX, gridZ, 3));
		float start = Landform.random(this.seed, gridX, gridZ, 4) * NoiseUtil.PI2;
		float sweep = NoiseUtil.lerp(700.0F, 2400.0F, Landform.random(this.seed, gridX, gridZ, 5)) / radius;
		// it has to run through open ocean nearly all along; where it doesn't, it isn't cut
		int shallow = 0;
		for (int i = 0; i <= 6; i++) {
			float angle = start + sweep * i / 6.0F;
			Cell ground = heightmap.sampleGround(x + NoiseUtil.cos(angle) * radius, z + NoiseUtil.sin(angle) * radius);
			if (ground.continentEdge > this.shoreline - OPEN_SEA || ground.height > this.levels.water(-MIN_DEPTH)) {
				shallow++;
			}
		}
		if (shallow > 2) {
			return NONE;
		}
		return new Arc(x, z, radius, start, sweep, NoiseUtil.lerp(30.0F, 60.0F, Landform.random(this.seed, gridX, gridZ, 6)));
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Trenches(this.seed, this.frequency, this.depth, this.shoreline, this.wobble.mapAll(visitor), this.levels, this.arcs);
	}
}
