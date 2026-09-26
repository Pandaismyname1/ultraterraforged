package raccoonman.reterraforged.world.worldgen.landform;

import raccoonman.reterraforged.data.preset.settings.LandformSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Winding, branching canyons cut into badlands and plateaus, with walls stepped into ledges and a flat, dry floor,
 * like the side canyons of a desert plateau. They never cut below the sea, so they stay dry.
 *
 * @param main the big canyons: 1 along their middle, falling off to either side
 * @param side narrower, shallower side canyons
 */
public record Canyons(Noise main, Noise side, float depth, float floorStart, float wallStart, Levels levels) implements Landform {
	// the walls step down in this many ledges
	private static final int LEDGES = 4;
	// side canyons are this deep compared to the main ones
	private static final float SIDE_DEPTH = 0.55F;
	// the floor stays this many blocks above the sea
	private static final int FLOOR_ABOVE_SEA = 3;

	public static Canyons make(int seed, LandformSettings.Canyons settings, Levels levels) {
		// the zero lines of single octave noise meander and branch like a drainage network
		int mainScale = Math.round(520.0F / Math.max(0.2F, settings.frequency));
		Noise main = Noises.perlinRidge(seed, mainScale, 1);
		main = Noises.warpPerlin(main, seed + 1, 120, 2, 70.0F);
		main = Noises.warpPerlin(main, seed + 2, 24, 2, 10.0F);
		int sideScale = Math.max(40, mainScale / 3);
		Noise side = Noises.perlinRidge(seed + 3, sideScale, 1);
		side = Noises.warpPerlin(side, seed + 4, 60, 2, 30.0F);
		side = Noises.warpPerlin(side, seed + 5, 16, 2, 6.0F);
		// wider canyons start their walls further from the middle line
		float width = Math.max(0.3F, settings.width);
		float floorStart = 1.0F - 0.012F * width;
		float wallStart = 1.0F - 0.06F * width;
		return new Canyons(main, side, settings.depth, floorStart, wallStart, levels);
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		float mask = terrainMask(cell) * Landform.smoothstep(cell.terrainRegionEdge, 0.1F, 0.45F);
		if (mask <= 0.0F || cell.terrain.isSubmerged() || cell.terrain.isRiver()) {
			return;
		}
		float mainDepth = this.profile(this.main.compute(x, z, 0));
		float sideDepth = this.profile(this.side.compute(x, z, 0)) * SIDE_DEPTH;
		float cut = Math.max(mainDepth, sideDepth);
		if (cut <= 0.0F) {
			return;
		}
		float floor = this.levels.water(FLOOR_ABOVE_SEA);
		float available = cell.height - floor;
		if (available <= 0.0F) {
			return;
		}
		float depth = Math.min(this.depth * this.levels.unit * cut * mask, available);
		cell.height -= depth;
		if (cut < 1.0F) {
			// keep the stepped walls crisp; the filters would round them off
			cell.erosionMask = true;
		}
	}

	// 0 outside the canyon to 1 on its floor, in ledges
	private float profile(float ridge) {
		if (ridge <= this.wallStart) {
			return 0.0F;
		}
		if (ridge >= this.floorStart) {
			return 1.0F;
		}
		float t = (ridge - this.wallStart) / (this.floorStart - this.wallStart);
		// mostly flat ledges joined by short steep cliffs
		float steps = t * LEDGES;
		int ledge = NoiseUtil.floor(steps);
		float rise = Landform.smoothstep(steps - ledge, 0.55F, 1.0F);
		return Math.min(1.0F, (ledge + rise) / LEDGES);
	}

	private static float terrainMask(Cell cell) {
		return cell.terrain.includes(TerrainType.BADLANDS) || cell.terrain.includes(TerrainType.PLATEAU) ? 1.0F : 0.0F;
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Canyons(this.main.mapAll(visitor), this.side.mapAll(visitor), this.depth, this.floorStart, this.wallStart, this.levels);
	}
}
