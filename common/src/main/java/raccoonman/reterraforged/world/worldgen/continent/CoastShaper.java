package raccoonman.reterraforged.world.worldgen.continent;

import raccoonman.reterraforged.concurrent.Resource;
import raccoonman.reterraforged.data.preset.settings.CoastSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.landform.Landform;
import raccoonman.reterraforged.world.worldgen.landform.SiteCache;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.rivermap.Rivermap;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Reshapes a continent's coastline: headlands and bays, peninsulas reaching out into the sea, islands standing off the
 * coast, some tied to it by a sandbar, and sand spits. It works on the continent's own values, how far inland a place
 * lies, so the height of the land, its climate and its beaches all follow the new coast. Sizes are given in blocks and
 * turned into those values by how fast they change across the coast where each feature stands.
 *
 * @param shoreline the continent value where the land meets the sea
 * @param bays pushes the coast out into headlands and in into bays, around 0.5
 */
public record CoastShaper(Continent continent, int seed, float shoreline, CoastSettings settings, Noise bays, float bayStrength, Noise wobble, SiteCache<Arm> peninsulas, SiteCache<Isle> islands, SiteCache<Arm> spits) implements Continent {
	// continent values this far either side of the shoreline are reshaped at all
	private static final float SEAWARD = 0.4F;
	private static final float LANDWARD = 0.12F;
	// headlands and bays push the coast this far, in continent values, at full strength
	private static final float BAY_REACH = 0.06F;
	private static final float PENINSULA_GRID = 1400.0F;
	private static final float ISLAND_GRID = 640.0F;
	private static final float SPIT_GRID = 900.0F;
	// how far the land along a peninsula lies inland of the shoreline, in continent values, at its base and tip
	private static final float BASE_LAND = 0.2F;
	private static final float TIP_LAND = 0.06F;
	// a sandbar lies just above the sea
	private static final float BAR_LAND = 0.006F;
	// the shallow shelf around what's raised reaches this much further out, as a share of its width
	private static final float SHELF = 0.8F;
	private static final float SHELF_DEPTH = 0.02F;
	private static final Arm NO_ARM = new Arm(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
	private static final Isle NO_ISLE = new Isle(0.0F, 0.0F, 0.0F, 0.0F, false, 0.0F, 0.0F, 0.0F);

	/**
	 * A peninsula or a spit: from a point on the coast, out along a curve.
	 *
	 * @param dirX the way out to sea
	 * @param length in blocks
	 * @param baseWidth half its width at the coast, in blocks; tipWidth at its end
	 * @param bend how far it bends sideways over its length, as a share of it, and bendPhase how
	 * @param baseLand how far inland its land lies, in continent values, at its base; tipLand at its tip
	 */
	public record Arm(float x, float z, float dirX, float dirZ, float length, float baseWidth, float tipWidth, float bend, float bendPhase, float baseLand, float tipLand) {
	}

	/**
	 * An island off the coast, and a sandbar tying it to the land if it has one.
	 *
	 * @param landX the way to the mainland, and landDistance how far it is, in blocks
	 */
	public record Isle(float x, float z, float radius, float height, boolean tied, float landX, float landZ, float landDistance) {
	}

	public static Continent wrap(Continent continent, int seed, float shoreline, CoastSettings settings) {
		boolean any = settings.headlands.enabled || settings.peninsulas.enabled || settings.coastalIslands.enabled || settings.spits.enabled;
		if (!any) {
			return continent;
		}
		Noise bays = Noises.perlin(seed, 260, 2);
		bays = Noises.warpPerlin(bays, seed + 1, 120, 1, 60.0F);
		return new CoastShaper(continent, seed, shoreline, settings, bays, settings.headlands.enabled ? settings.headlands.strength : 0.0F, Noises.perlin(seed + 2, 40, 2), SiteCache.make(), SiteCache.make(), SiteCache.make());
	}

	public static final int NONE = 0;
	public static final int PENINSULA = 1;
	public static final int ISLAND = 2;
	public static final int BAR = 3;

	@Override
	public void apply(Cell cell, float x, float z) {
		this.continent.apply(cell, x, z);
		cell.coastFeature = NONE;
		float edge = cell.continentEdge;
		if (edge < this.shoreline - SEAWARD || edge > this.shoreline + LANDWARD) {
			return;
		}
		int[] feature = FEATURE.get();
		feature[0] = NONE;
		float shaped = this.shape(edge, x, z, feature);
		float change = shaped - edge;
		if (change != 0.0F) {
			cell.continentEdge = shaped;
			cell.continentNoise += change;
			cell.coastFeature = feature[0];
		}
	}

	/**
	 * Labels the places the coast's features made, once the land's height is known: the land of peninsulas and islands,
	 * and bars of sand just above the sea.
	 */
	public static void label(Cell cell, Levels levels) {
		if (cell.coastFeature == NONE || cell.height < levels.water || cell.terrain.isRiver() || cell.terrain.isLake() || cell.terrain.isWetland() || cell.terrain.isVolcano() || cell.terrain.overridesCoast()) {
			return;
		}
		if (cell.coastFeature == BAR) {
			if (cell.height < levels.water(4)) {
				cell.terrain = TerrainType.SAND_BAR;
			}
		} else if (cell.height > levels.water(3)) {
			cell.terrain = cell.coastFeature == PENINSULA ? TerrainType.PENINSULA : TerrainType.COASTAL_ISLAND;
		}
	}

	// which feature raised the coast most, so far, per thread
	private static final ThreadLocal<int[]> FEATURE = ThreadLocal.withInitial(() -> new int[1]);

	// the continent value here once the coast is reshaped
	private float shape(float edge, float x, float z, int[] feature) {
		float shaped = edge;
		if (this.bayStrength > 0.0F) {
			float near = 1.0F - Landform.smoothstep(Math.abs(edge - this.shoreline), 0.0F, BAY_REACH * 2.5F);
			shaped += (this.bays.compute(x, z, 0) - 0.5F) * 2.0F * BAY_REACH * this.bayStrength * near;
		}
		float before = shaped;
		if (this.settings.peninsulas.enabled) {
			shaped = this.arms(shaped, x, z, PENINSULA_GRID, this.peninsulas, true);
			if (shaped > before) {
				feature[0] = PENINSULA;
				before = shaped;
			}
		}
		if (this.settings.spits.enabled) {
			shaped = this.arms(shaped, x, z, SPIT_GRID, this.spits, false);
			if (shaped > before) {
				feature[0] = BAR;
				before = shaped;
			}
		}
		if (this.settings.coastalIslands.enabled) {
			shaped = this.islands(shaped, x, z, feature, before);
		}
		return shaped;
	}

	private float arms(float edge, float x, float z, float grid, SiteCache<Arm> cache, boolean peninsula) {
		int gridX = NoiseUtil.floor(x / grid);
		int gridZ = NoiseUtil.floor(z / grid);
		float frequency = peninsula ? this.settings.peninsulas.frequency : this.settings.spits.frequency;
		int purpose = peninsula ? 100 : 200;
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (Landform.random(this.seed + purpose, gx, gz, 0) >= frequency) {
					continue;
				}
				Arm arm = cache.get(gx, gz, (ax, az) -> this.findArm(ax, az, grid, purpose, peninsula));
				if (arm != NO_ARM) {
					edge = this.arm(arm, edge, x, z);
				}
			}
		}
		return edge;
	}

	private float arm(Arm arm, float edge, float x, float z) {
		float dx = x - arm.x();
		float dz = z - arm.z();
		float along = dx * arm.dirX() + dz * arm.dirZ();
		float reach = arm.length() + arm.tipWidth() * (1.0F + SHELF);
		if (along < -arm.baseWidth() || along > reach) {
			return edge;
		}
		float u = along / arm.length();
		float across = dx * -arm.dirZ() + dz * arm.dirX();
		// bending more the further out it reaches
		float clampedU = NoiseUtil.clamp(u, 0.0F, 1.0F);
		across -= arm.length() * arm.bend() * clampedU * NoiseUtil.sin(clampedU * NoiseUtil.PI2 * 0.5F + arm.bendPhase());
		float width = NoiseUtil.lerp(arm.baseWidth(), arm.tipWidth(), clampedU);
		if (u > 1.0F) {
			// a rounded tip
			float beyond = (along - arm.length()) / arm.tipWidth();
			width = arm.tipWidth() * (float) Math.sqrt(Math.max(0.0F, 1.0F - beyond * beyond));
		}
		width *= 1.0F + (this.wobble.compute(x, z, 0) - 0.5F) * 0.5F;
		float distance = Math.abs(across);
		float shelf = width + arm.baseWidth() * SHELF;
		if (distance >= shelf && (u <= 1.0F || distance >= arm.tipWidth() * (1.0F + SHELF))) {
			return edge;
		}
		float land = this.shoreline + NoiseUtil.lerp(arm.baseLand(), arm.tipLand(), clampedU);
		float inside = width <= 0.0F ? 0.0F : Landform.smoothstep(distance / width, 1.0F, 0.6F);
		// the land fades back into the coast behind the arm's base
		float base = Landform.smoothstep(along, -arm.baseWidth(), 0.0F);
		float raised = NoiseUtil.lerp(edge, land, inside * base);
		float shelved = NoiseUtil.lerp(edge, this.shoreline - SHELF_DEPTH, Landform.smoothstep(distance, shelf, width) * base);
		return Math.max(edge, Math.max(raised, shelved));
	}

	private float islands(float edge, float x, float z, int[] feature, float before) {
		int gridX = NoiseUtil.floor(x / ISLAND_GRID);
		int gridZ = NoiseUtil.floor(z / ISLAND_GRID);
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (Landform.random(this.seed + 300, gx, gz, 0) >= this.settings.coastalIslands.frequency) {
					continue;
				}
				Isle isle = this.islands.get(gx, gz, (ix, iz) -> this.findIsle(ix, iz));
				if (isle != NO_ISLE) {
					edge = this.isle(isle, edge, x, z, feature);
				}
			}
		}
		return edge;
	}

	private float isle(Isle isle, float edge, float x, float z, int[] feature) {
		float dx = x - isle.x();
		float dz = z - isle.z();
		float distance = (float) Math.sqrt(dx * dx + dz * dz);
		float shaped = edge;
		float outer = isle.radius() * (1.0F + SHELF);
		if (distance < outer) {
			float d = distance / isle.radius() + (this.wobble.compute(x, z, 0) - 0.5F) * 0.5F;
			float inside = Landform.smoothstep(d, 1.0F, 0.55F);
			float land = this.shoreline + isle.height() * (1.0F - 0.5F * Math.min(1.0F, d));
			shaped = Math.max(shaped, NoiseUtil.lerp(edge, land, inside));
			shaped = Math.max(shaped, NoiseUtil.lerp(edge, this.shoreline - SHELF_DEPTH, Landform.smoothstep(d, 1.0F + SHELF, 1.0F)));
			if (shaped > edge) {
				feature[0] = ISLAND;
			}
		}
		if (isle.tied()) {
			// a narrow sandbar to the mainland
			float along = dx * isle.landX() + dz * isle.landZ();
			if (along > 0.0F && along < isle.landDistance() + 40.0F) {
				float across = Math.abs(dx * -isle.landZ() + dz * isle.landX()) + (this.wobble.compute(x, z, 0) - 0.5F) * 6.0F;
				// running into the island at one end and the mainland at the other
				float width = 7.0F * Landform.smoothstep(along, 0.0F, 12.0F) * (1.0F - Landform.smoothstep(along, isle.landDistance() + 10.0F, isle.landDistance() + 40.0F));
				float bar = NoiseUtil.lerp(edge, this.shoreline + BAR_LAND, Landform.smoothstep(across, width, width * 0.5F));
				if (bar > shaped) {
					shaped = bar;
					feature[0] = BAR;
				}
				shaped = Math.max(shaped, NoiseUtil.lerp(edge, this.shoreline - SHELF_DEPTH * 0.5F, Landform.smoothstep(across, width * 3.0F, width)));
			}
		}
		return shaped;
	}

	private Arm findArm(int gridX, int gridZ, float grid, int purpose, boolean peninsula) {
		float margin = grid * 0.2F;
		float x = gridX * grid + margin + Landform.random(this.seed + purpose, gridX, gridZ, 1) * (grid - margin * 2.0F);
		float z = gridZ * grid + margin + Landform.random(this.seed + purpose, gridX, gridZ, 2) * (grid - margin * 2.0F);
		long coast = this.findCoast(x, z);
		if (coast == Long.MIN_VALUE) {
			return NO_ARM;
		}
		float cx = Float.intBitsToFloat((int) (coast >> 32));
		float cz = Float.intBitsToFloat((int) coast);
		float[] gradient = this.gradient(cx, cz);
		float slope = (float) Math.sqrt(gradient[0] * gradient[0] + gradient[1] * gradient[1]);
		if (slope < 1.0E-6F) {
			return NO_ARM;
		}
		// out to sea, down the continent value
		float dirX = -gradient[0] / slope;
		float dirZ = -gradient[1] / slope;
		float size = peninsula ? this.settings.peninsulas.size : 1.0F;
		float length;
		float baseWidth;
		float tipWidth;
		float bend;
		if (peninsula) {
			length = NoiseUtil.lerp(500.0F, 1400.0F, Landform.random(this.seed + purpose, gridX, gridZ, 3)) * size;
			baseWidth = NoiseUtil.lerp(100.0F, 230.0F, Landform.random(this.seed + purpose, gridX, gridZ, 4)) * size;
			tipWidth = baseWidth * NoiseUtil.lerp(0.3F, 0.6F, Landform.random(this.seed + purpose, gridX, gridZ, 5));
			bend = NoiseUtil.lerp(-0.25F, 0.25F, Landform.random(this.seed + purpose, gridX, gridZ, 6));
		} else {
			// a spit runs along the coast, curving out to sea: start it heading along the coast, a little seaward
			float side = Landform.random(this.seed + purpose, gridX, gridZ, 7) < 0.5F ? -1.0F : 1.0F;
			float angle = side * NoiseUtil.lerp(0.9F, 1.25F, Landform.random(this.seed + purpose, gridX, gridZ, 8));
			float cos = NoiseUtil.cos(angle);
			float sin = NoiseUtil.sin(angle);
			float turnedX = dirX * cos - dirZ * sin;
			float turnedZ = dirX * sin + dirZ * cos;
			dirX = turnedX;
			dirZ = turnedZ;
			length = NoiseUtil.lerp(180.0F, 420.0F, Landform.random(this.seed + purpose, gridX, gridZ, 3));
			baseWidth = NoiseUtil.lerp(7.0F, 11.0F, Landform.random(this.seed + purpose, gridX, gridZ, 4));
			tipWidth = baseWidth * 0.6F;
			bend = side * -NoiseUtil.lerp(0.1F, 0.2F, Landform.random(this.seed + purpose, gridX, gridZ, 6));
		}
		// only out into open sea: not across a bay to more land, nor filling a bay between land either side
		for (float t = 0.35F; t <= 1.3F; t += 0.3F) {
			if (this.baseEdge(cx + dirX * length * t, cz + dirZ * length * t) >= this.shoreline - (peninsula ? 0.02F : 0.005F)) {
				return NO_ARM;
			}
		}
		if (peninsula) {
			for (float side : new float[] { -0.5F, 0.5F }) {
				for (float t : new float[] { 0.6F, 1.0F }) {
					float px = cx + dirX * length * t - dirZ * length * side;
					float pz = cz + dirZ * length * t + dirX * length * side;
					if (this.baseEdge(px, pz) >= this.shoreline) {
						return NO_ARM;
					}
				}
			}
		}
		float baseLand = peninsula ? BASE_LAND : BAR_LAND;
		float tipLand = peninsula ? TIP_LAND : BAR_LAND;
		return new Arm(cx - dirX * baseWidth * 0.5F, cz - dirZ * baseWidth * 0.5F, dirX, dirZ, length, baseWidth, tipWidth, bend, Landform.random(this.seed + purpose, gridX, gridZ, 9) * NoiseUtil.PI2, baseLand, tipLand);
	}

	private Isle findIsle(int gridX, int gridZ) {
		float margin = ISLAND_GRID * 0.25F;
		float x = gridX * ISLAND_GRID + margin + Landform.random(this.seed + 300, gridX, gridZ, 1) * (ISLAND_GRID - margin * 2.0F);
		float z = gridZ * ISLAND_GRID + margin + Landform.random(this.seed + 300, gridX, gridZ, 2) * (ISLAND_GRID - margin * 2.0F);
		float edge = this.baseEdge(x, z);
		// out on the shelf, not far from the coast
		if (edge >= this.shoreline - 0.02F || edge < this.shoreline - SEAWARD * 0.6F) {
			return NO_ISLE;
		}
		float[] gradient = this.gradient(x, z);
		float slope = (float) Math.sqrt(gradient[0] * gradient[0] + gradient[1] * gradient[1]);
		if (slope < 1.0E-6F) {
			return NO_ISLE;
		}
		float landDistance = (this.shoreline - edge) / slope;
		if (landDistance < 70.0F || landDistance > 650.0F) {
			return NO_ISLE;
		}
		float size = this.settings.coastalIslands.size;
		float radius = NoiseUtil.lerp(45.0F, 160.0F, Landform.random(this.seed + 300, gridX, gridZ, 3)) * size;
		// kept clear of the mainland
		if (radius * (1.0F + SHELF) > landDistance) {
			radius = landDistance / (1.0F + SHELF);
			if (radius < 30.0F) {
				return NO_ISLE;
			}
		}
		float height = NoiseUtil.lerp(0.06F, 0.2F, Landform.random(this.seed + 300, gridX, gridZ, 4));
		boolean tied = Landform.random(this.seed + 300, gridX, gridZ, 5) < this.settings.coastalIslands.tombolos && landDistance < 350.0F;
		return new Isle(x, z, radius, height, tied, gradient[0] / slope, gradient[1] / slope, landDistance);
	}

	// a point on the coast near the given one, packed as two floats, or Long.MIN_VALUE if there's none
	private long findCoast(float x, float z) {
		for (int i = 0; i < 6; i++) {
			float edge = this.baseEdge(x, z);
			if (edge < this.shoreline - SEAWARD * 1.5F || edge > this.shoreline + 0.3F) {
				return Long.MIN_VALUE;
			}
			if (Math.abs(edge - this.shoreline) < 0.002F) {
				return ((long) Float.floatToIntBits(x) << 32) | (Float.floatToIntBits(z) & 0xFFFFFFFFL);
			}
			float[] gradient = this.gradient(x, z);
			float slope2 = gradient[0] * gradient[0] + gradient[1] * gradient[1];
			if (slope2 < 1.0E-12F) {
				return Long.MIN_VALUE;
			}
			// a step straight towards the shoreline, no more than a few hundred blocks at a time
			float step = (this.shoreline - edge) / slope2;
			float stepX = gradient[0] * step;
			float stepZ = gradient[1] * step;
			float length = (float) Math.sqrt(stepX * stepX + stepZ * stepZ);
			if (length > 400.0F) {
				stepX *= 400.0F / length;
				stepZ *= 400.0F / length;
			}
			x += stepX;
			z += stepZ;
		}
		return Math.abs(this.baseEdge(x, z) - this.shoreline) < 0.01F ? ((long) Float.floatToIntBits(x) << 32) | (Float.floatToIntBits(z) & 0xFFFFFFFFL) : Long.MIN_VALUE;
	}

	// how fast the continent value changes, per block
	private float[] gradient(float x, float z) {
		float step = 48.0F;
		return new float[] {
			(this.baseEdge(x + step, z) - this.baseEdge(x - step, z)) / (2.0F * step),
			(this.baseEdge(x, z + step) - this.baseEdge(x, z - step)) / (2.0F * step)
		};
	}

	// the continent value before the coast is reshaped
	private float baseEdge(float x, float z) {
		return this.continent.getEdgeValue(x, z);
	}

	@Override
	public float getEdgeValue(float x, float z) {
		try (Resource<Cell> resource = Cell.getResource()) {
			Cell cell = resource.get();
			this.apply(cell, x, z);
			return cell.continentEdge;
		}
	}

	@Override
	public long getNearestCenter(float x, float z) {
		return this.continent.getNearestCenter(x, z);
	}

	@Override
	public Rivermap getRivermap(int x, int z) {
		return this.continent.getRivermap(x, z);
	}

	@Override
	public Rivermap getRivermap(Cell cell) {
		return this.continent.getRivermap(cell);
	}
}
