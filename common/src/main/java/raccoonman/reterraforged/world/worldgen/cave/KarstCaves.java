package raccoonman.reterraforged.world.worldgen.cave;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.landform.SiteCache;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Karst caves: under karst country, great chambers worn out of the limestone, joined by passages, some with a pool on
 * their floor and some with a pothole dropping into them from the surface. Each chamber lies under its own cell of a
 * grid, joined to the chambers of the next cells along.
 */
final class KarstCaves implements CaveFeatures.Feature {
	private static final float GRID = 80.0F;
	// the chambers lie this far below the ground
	private static final int DEPTH = 26;
	private static final float PASSAGE = 3.0F;
	private static final Chamber NONE = new Chamber(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, false, false);

	private final int seed;
	private final float size;
	private final SiteCache<Chamber> chambers = SiteCache.make();

	/**
	 * @param ground the top of the ground over it
	 */
	record Chamber(float x, float y, float z, float radius, float height, int ground, boolean pool, boolean pothole) {
	}

	KarstCaves(int seed, float size) {
		this.seed = seed;
		this.size = size;
	}

	@Override
	public void carve(CaveCarving carving) {
		int gridX = NoiseUtil.floor(carving.minX / GRID);
		int gridZ = NoiseUtil.floor(carving.minZ / GRID);
		for (int gx = gridX - 1; gx <= gridX + 2; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 2; gz++) {
				Chamber chamber = this.chamber(gx, gz, carving);
				if (chamber == NONE) {
					continue;
				}
				float reach = chamber.radius() + 1.0F;
				if (carving.reaches(chamber.x() - reach, chamber.z() - reach, chamber.x() + reach, chamber.z() + reach)) {
					float pool = chamber.pool() ? chamber.y() - chamber.height() * 0.55F : CaveCarving.DRY;
					carving.hollow(chamber.x(), chamber.y(), chamber.z(), chamber.radius(), chamber.height(), chamber.radius() * 0.85F, (int) pool, 5);
					if (chamber.pothole()) {
						// a shaft straight up to the surface
						for (float y = chamber.y(); y <= chamber.ground() + 2; y += 1.5F) {
							carving.hollow(chamber.x(), y, chamber.z(), 2.5F, 1.5F, 2.5F, CaveCarving.DRY, CaveCarving.OPEN);
						}
					}
				}
				// passages to the chambers east and south
				for (int[] next : new int[][] { { 1, 0 }, { 0, 1 } }) {
					Chamber other = this.chamber(gx + next[0], gz + next[1], carving);
					if (other == NONE) {
						continue;
					}
					float minX = Math.min(chamber.x(), other.x()) - PASSAGE;
					float maxX = Math.max(chamber.x(), other.x()) + PASSAGE;
					float minZ = Math.min(chamber.z(), other.z()) - PASSAGE;
					float maxZ = Math.max(chamber.z(), other.z()) + PASSAGE;
					if (!carving.reaches(minX, minZ, maxX, maxZ)) {
						continue;
					}
					CaveFeatures.along(new float[] { chamber.x(), other.x() }, new float[] { chamber.y() - chamber.height() * 0.4F, other.y() - other.height() * 0.4F }, new float[] { chamber.z(), other.z() }, 1.5F,
						(x, y, z, index) -> carving.hollow(x, y + PASSAGE * 0.5F, z, PASSAGE, PASSAGE * 0.8F, PASSAGE, CaveCarving.DRY, 5));
				}
			}
		}
	}

	private Chamber chamber(int gridX, int gridZ, CaveCarving carving) {
		return this.chambers.get(gridX, gridZ, (cx, cz) -> this.find(cx, cz, carving));
	}

	Chamber find(int gridX, int gridZ, CaveCarving carving) {
		float x = (gridX + 0.5F) * GRID + (CaveFeatures.random(this.seed, gridX, gridZ, 1) - 0.5F) * GRID * 0.5F;
		float z = (gridZ + 0.5F) * GRID + (CaveFeatures.random(this.seed, gridX, gridZ, 2) - 0.5F) * GRID * 0.5F;
		Cell cell = carving.cell(x, z);
		if (cell.terrain != TerrainType.KARST && cell.terrain != TerrainType.SINKHOLE) {
			return NONE;
		}
		int ground = carving.levels.scale(cell.height);
		float radius = NoiseUtil.lerp(10.0F, 18.0F, CaveFeatures.random(this.seed, gridX, gridZ, 3)) * this.size;
		float height = NoiseUtil.lerp(6.0F, 10.0F, CaveFeatures.random(this.seed, gridX, gridZ, 4)) * this.size;
		float y = ground - DEPTH - height * 0.5F - CaveFeatures.random(this.seed, gridX, gridZ, 5) * 10.0F;
		if (y - height < carving.bottom + 8) {
			return NONE;
		}
		boolean pool = CaveFeatures.random(this.seed, gridX, gridZ, 6) < 0.4F;
		boolean pothole = CaveFeatures.random(this.seed, gridX, gridZ, 7) < 0.3F;
		return new Chamber(x, y, z, radius, height, ground, pool, pothole);
	}
}
