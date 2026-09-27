package raccoonman.reterraforged.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.test.BuiltinPresetRenderTest;
import raccoonman.reterraforged.test.PresetRenderer;
import raccoonman.reterraforged.test.TestBootstrap;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

/**
 * The caves shaped by the land, carved into a stand-in world chunk by chunk as the game would: each is found near the
 * middle of the world and measured. With -Drtf.render=true slices through them are drawn to build/terrain-views.
 */
public class CavesTest {
	private static final int SEED = 4321;

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	private static CaveWorld world(String preset) {
		Preset settings = BuiltinPresetRenderTest.presets().get(preset).get();
		return new CaveWorld(GeneratorContext.makeCached(settings, PresetRenderer.SEED, 3, 6, false));
	}

	private static boolean render() {
		return Boolean.getBoolean("rtf.render");
	}

	@Test
	void undergroundRiversHoldTheirWater() throws IOException {
		CaveWorld world = world("default");
		UndergroundRivers rivers = new UndergroundRivers(SEED, 1.0F);
		CaveCarving probe = world.probe();
		UndergroundRivers.Path path = null;
		search:
		for (int r = 0; r < 12; r++) {
			for (int gx = -r; gx <= r; gx++) {
				for (int gz = -r; gz <= r; gz++) {
					// as the game rolls for them
					if (CaveFeatures.random(SEED, gx, gz, 0) >= 0.5F) {
						continue;
					}
					UndergroundRivers.Path found = rivers.find(gx, gz, probe);
					if (found.xs().length > 0) {
						path = found;
						break search;
					}
				}
			}
		}
		assertTrue(path != null, "no underground river found");
		world.carve(path.minX(), path.minZ(), path.maxX(), path.maxZ(), rivers);
		int water = world.count((state) -> state.is(Blocks.WATER));
		int air = world.count(BlockState::isAir);
		// water beside or over air would pour out
		int leaks = 0;
		for (long key : world.carvedPositions()) {
			BlockPos pos = BlockPos.of(key);
			if (!world.get(pos.getX(), pos.getY(), pos.getZ()).is(Blocks.WATER)) {
				continue;
			}
			if (world.get(pos.getX(), pos.getY() - 1, pos.getZ()).isAir()) {
				leaks++;
				continue;
			}
			for (int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
				if (world.get(pos.getX() + step[0], pos.getY(), pos.getZ() + step[1]).isAir()) {
					leaks++;
					break;
				}
			}
		}
		System.out.printf("underground river at %.0f %.0f, water at y=%d: %d blocks of water, %d of air, %d leaking%n", path.xs()[0], path.zs()[0], path.water(), water, air, leaks);
		if (render()) {
			world.writeMap("underground_river_map", (int) path.xs()[path.xs().length / 2], (int) path.zs()[path.zs().length / 2], 400);
			world.writeSlice("underground_river_slice", (int) path.xs()[path.xs().length / 2], (int) path.zs()[path.zs().length / 2], true, 80, path.water() - 20, world.ground((int) path.xs()[0], (int) path.zs()[0]) + 10);
		}
		assertTrue(water > 500, "only " + water + " blocks of water");
		assertTrue(air > 500, "only " + air + " blocks of air over the water");
		assertTrue(leaks < water / 100, leaks + " blocks of water beside or over air");
	}

	@Test
	void lavaTubesAreLinedWithBasalt() throws IOException {
		CaveWorld world = world("volcanic_isles");
		LavaTubes tubes = new LavaTubes(SEED, 1.0F);
		CaveCarving probe = world.probe();
		LavaTubes.Tube tube = null;
		search:
		for (int r = 0; r < 20; r++) {
			for (int gx = -r; gx <= r; gx++) {
				for (int gz = -r; gz <= r; gz++) {
					if (Math.max(Math.abs(gx), Math.abs(gz)) != r) {
						continue;
					}
					LavaTubes.Tube found = tubes.find(gx, gz, probe);
					if (found.xs().length > 0) {
						tube = found;
						break search;
					}
				}
			}
		}
		assertTrue(tube != null, "no lava tube found");
		world.carve(tube.minX(), tube.minZ(), tube.maxX(), tube.maxZ(), tubes);
		int lining = world.count((state) -> state.is(Blocks.BASALT) || state.is(Blocks.BLACKSTONE) || state.is(Blocks.SMOOTH_BASALT));
		int air = world.count(BlockState::isAir);
		int skylights = 0;
		for (int i = 0; i < tube.xs().length; i++) {
			if (tube.skylights()[i]) {
				int x = (int) tube.xs()[i];
				int z = (int) tube.zs()[i];
				if (world.get(x, world.ground(x, z), z).isAir()) {
					skylights++;
				}
			}
		}
		System.out.printf("lava tube from %.0f %.0f, %d steps: %d blocks of lining, %d of air, %d skylights open%n", tube.xs()[0], tube.zs()[0], tube.xs().length, lining, air, skylights);
		if (render()) {
			world.writeMap("lava_tube_map", (int) tube.xs()[tube.xs().length / 2], (int) tube.zs()[tube.zs().length / 2], 300);
			int i = tube.xs().length / 2;
			world.writeSlice("lava_tube_slice", (int) tube.xs()[i], (int) tube.zs()[i], true, 40, (int) tube.ys()[i] - 12, world.ground((int) tube.xs()[i], (int) tube.zs()[i]) + 8);
		}
		assertTrue(lining > 300, "only " + lining + " blocks of lining");
		assertTrue(air > 300, "only " + air + " blocks of air");
	}

	@Test
	void karstCavesOpenUnderKarst() throws IOException {
		CaveWorld world = world("tropics");
		KarstCaves caves = new KarstCaves(SEED, 1.0F);
		CaveCarving probe = world.probe();
		KarstCaves.Chamber chamber = null;
		int foundX = 0;
		int foundZ = 0;
		search:
		for (int r = 0; r < 60; r++) {
			for (int gx = -r; gx <= r; gx++) {
				for (int gz = -r; gz <= r; gz++) {
					if (Math.max(Math.abs(gx), Math.abs(gz)) != r) {
						continue;
					}
					KarstCaves.Chamber found = caves.find(gx, gz, probe);
					if (found.radius() > 0.0F) {
						chamber = found;
						foundX = gx;
						foundZ = gz;
						break search;
					}
				}
			}
		}
		assertTrue(chamber != null, "no karst cave found");
		world.carve(chamber.x() - 200, chamber.z() - 200, chamber.x() + 200, chamber.z() + 200, caves);
		int air = world.count(BlockState::isAir);
		int water = world.count((state) -> state.is(Blocks.WATER));
		System.out.printf("karst chamber at %.0f %.0f %.0f (grid %d %d): %d blocks of air, %d of water%n", chamber.x(), chamber.y(), chamber.z(), foundX, foundZ, air, water);
		if (render()) {
			world.writeMap("karst_caves_map", (int) chamber.x(), (int) chamber.z(), 400);
			world.writeSlice("karst_caves_slice", (int) chamber.x(), (int) chamber.z(), true, 120, (int) chamber.y() - 20, chamber.ground() + 8);
		}
		assertTrue(air > 2000, "only " + air + " blocks of air");
	}

	@Test
	void seaCavesAreFloodedAtTheirMouth() throws IOException {
		CaveWorld world = world("default");
		SeaCaves caves = new SeaCaves(SEED, 1.0F);
		CaveCarving probe = world.probe();
		int found = 0;
		SeaCaves.Cave first = null;
		for (int gx = -40; gx <= 40 && found < 12; gx++) {
			for (int gz = -40; gz <= 40 && found < 12; gz++) {
				SeaCaves.Cave cave = caves.find(gx, gz, probe);
				if (cave.radius() > 0.0F) {
					found++;
					first = first == null ? cave : first;
					world.carve(Math.min(cave.fromX(), cave.toX()) - 6, Math.min(cave.fromZ(), cave.toZ()) - 6, Math.max(cave.fromX(), cave.toX()) + 6, Math.max(cave.fromZ(), cave.toZ()) + 6, caves);
				}
			}
		}
		assertTrue(first != null, "no sea cave found");
		int sea = world.context.levels.waterY;
		int water = world.count((state) -> state.is(Blocks.WATER));
		int air = world.count(BlockState::isAir);
		// air under the sea's level would be a hole the sea should fill
		int dryUnderSea = 0;
		for (long key : world.carvedPositions()) {
			BlockPos pos = BlockPos.of(key);
			if (pos.getY() <= sea && world.get(pos.getX(), pos.getY(), pos.getZ()).isAir()) {
				dryUnderSea++;
			}
		}
		System.out.printf("%d sea caves found, carving %d blocks of water and %d of air, %d dry below the sea's level%n", found, water, air, dryUnderSea);
		if (render()) {
			world.writeMap("sea_caves_map", (int) first.toX(), (int) first.toZ(), 200);
			boolean alongX = Math.abs(first.toX() - first.fromX()) > Math.abs(first.toZ() - first.fromZ());
			world.writeSlice("sea_cave_slice", (int) ((first.fromX() + first.toX()) / 2), (int) ((first.fromZ() + first.toZ()) / 2), alongX, 60, sea - 10, sea + 30);
		}
		assertTrue(found >= 3, "only " + found + " sea caves");
		assertTrue(water > 100 && air > 100, "sea caves carve " + water + " blocks of water and " + air + " of air");
		assertTrue(dryUnderSea == 0, dryUnderSea + " blocks of air below the sea's level");
	}

	@Test
	void layerCavesRunLevel() throws IOException {
		CaveWorld world = world("default");
		LayerCaves caves = new LayerCaves(SEED, 1.0F);
		world.carve(-128, -128, 127, 127, caves);
		int air = world.count(BlockState::isAir);
		java.util.Map<Integer, Integer> levels = new java.util.TreeMap<>();
		for (long key : world.carvedPositions()) {
			levels.merge(BlockPos.of(key).getY(), 1, Integer::sum);
		}
		System.out.printf("layer caves: %d blocks of air, by height %s%n", air, levels);
		if (render()) {
			world.writeSlice("layer_caves_slice", 0, 0, true, 256, -64, 160);
		}
		assertTrue(air > 2000, "only " + air + " blocks of air");
	}

	@Test
	void giantCavernsHaveLakesAndPillars() throws IOException {
		CaveWorld world = world("default");
		GiantCaverns caverns = new GiantCaverns(SEED, 1.0F);
		CaveCarving probe = world.probe();
		GiantCaverns.Cavern cavern = null;
		search:
		for (int r = 0; r < 12; r++) {
			for (int gx = -r; gx <= r; gx++) {
				for (int gz = -r; gz <= r; gz++) {
					if (Math.max(Math.abs(gx), Math.abs(gz)) != r || CaveFeatures.random(SEED, gx, gz, 0) >= 0.35F) {
						continue;
					}
					GiantCaverns.Cavern found = caverns.find(gx, gz, probe);
					if (found.radiusX() > 0.0F) {
						cavern = found;
						break search;
					}
				}
			}
		}
		assertTrue(cavern != null, "no giant cavern found");
		float reach = Math.max(cavern.radiusX(), cavern.radiusZ()) * 1.3F;
		world.carve(cavern.x() - reach, cavern.z() - reach, cavern.x() + reach, cavern.z() + reach, caverns);
		int air = world.count(BlockState::isAir);
		int water = world.count((state) -> state.is(Blocks.WATER));
		// columns of stone left standing inside it
		int pillars = 0;
		int cy = (int) cavern.y();
		for (int dx = (int) -cavern.radiusX() / 2; dx < cavern.radiusX() / 2; dx++) {
			for (int dz = (int) -cavern.radiusZ() / 2; dz < cavern.radiusZ() / 2; dz++) {
				if (world.get((int) cavern.x() + dx, cy, (int) cavern.z() + dz).is(Blocks.STONE)) {
					pillars++;
				}
			}
		}
		System.out.printf("giant cavern at %.0f %.0f %.0f, %.0f by %.0f by %.0f: %d blocks of air, %d of water, %d stone columns at its middle height%n", cavern.x(), cavern.y(), cavern.z(), cavern.radiusX(), cavern.radiusY(), cavern.radiusZ(), air, water, pillars);
		if (render()) {
			world.writeSlice("giant_cavern_slice", (int) cavern.x(), (int) cavern.z(), true, (int) (cavern.radiusX() * 2.8F), (int) (cavern.y() - cavern.radiusY() * 1.5F), (int) (cavern.y() + cavern.radiusY() * 1.5F));
		}
		assertTrue(air > 100000, "only " + air + " blocks of air");
		assertTrue(water > 3000, "only " + water + " blocks of lake");
		assertTrue(pillars > 10, "only " + pillars + " stone columns in the middle");
	}

	@Test
	void glacierCavesAreWalledWithIce() throws IOException {
		CaveWorld world = world("frozen_north");
		GlacierCaves caves = new GlacierCaves(SEED, 1.0F);
		CaveCarving probe = world.probe();
		GlacierCaves.Cave cave = null;
		search:
		for (int r = 0; r < 40; r++) {
			for (int gx = -r; gx <= r; gx++) {
				for (int gz = -r; gz <= r; gz++) {
					if (Math.max(Math.abs(gx), Math.abs(gz)) != r) {
						continue;
					}
					GlacierCaves.Cave found = caves.find(gx, gz, probe);
					if (found.xs().length > 0) {
						cave = found;
						break search;
					}
				}
			}
		}
		assertTrue(cave != null, "no glacier cave found");
		world.carve(cave.minX(), cave.minZ(), cave.maxX(), cave.maxZ(), caves);
		int ice = world.count((state) -> state.is(Blocks.BLUE_ICE) || state.is(Blocks.PACKED_ICE));
		int air = world.count(BlockState::isAir);
		System.out.printf("glacier cave at %.0f %.0f: %d blocks of ice, %d of air%n", cave.xs()[0], cave.zs()[0], ice, air);
		if (render()) {
			world.writeSlice("glacier_cave_slice", (int) cave.xs()[2], (int) cave.zs()[2], true, 40, (int) cave.ys()[0] - 10, (int) cave.ys()[0] + 30);
		}
		assertTrue(ice > 150, "only " + ice + " blocks of ice");
		assertTrue(air > 150, "only " + air + " blocks of air");
	}

	@Test
	void rockSheltersUndercutCliffs() throws IOException {
		CaveWorld world = world("badlands");
		RockShelters shelters = new RockShelters(SEED, 1.0F);
		world.carve(-384, -256, 127, 255, shelters);
		int air = world.count(BlockState::isAir);
		System.out.printf("rock shelters: %d blocks of air%n", air);
		assertTrue(air > 200, "only " + air + " blocks of air");
	}

	@Test
	void springsWellOutOfHillsides() {
		CaveWorld world = world("highlands");
		Springs springs = new Springs(SEED, 1.0F);
		world.carve(-256, -256, 255, 255, springs);
		int sources = 0;
		for (BlockPos pos : world.postprocessed) {
			if (world.get(pos.getX(), pos.getY(), pos.getZ()).is(Blocks.WATER)) {
				sources++;
			}
		}
		System.out.printf("%d springs%n", sources);
		assertTrue(sources > 20, "only " + sources + " springs");
	}

	@Test
	void hydrothermalVentsStandInTheRifts() {
		CaveWorld world = world("default");
		raccoonman.reterraforged.world.worldgen.heightmap.TerrainLocator.Found found = raccoonman.reterraforged.world.worldgen.heightmap.TerrainLocator.locate(world.context.lookup, raccoonman.reterraforged.world.worldgen.terrain.TerrainType.OCEAN_RIDGE, 0, 0, 12000, 60_000L);
		assertTrue(found != null, "no mid-ocean ridge found");
		HydrothermalVents vents = new HydrothermalVents(SEED);
		world.carve(found.x() - 160, found.z() - 160, found.x() + 160, found.z() + 160, vents);
		int magma = world.count((state) -> state.is(Blocks.MAGMA_BLOCK));
		int chimneys = world.count((state) -> state.is(Blocks.BASALT) || state.is(Blocks.BLACKSTONE));
		System.out.printf("hydrothermal vents by the ridge at %d %d: %d blocks of magma, %d of chimney%n", found.x(), found.z(), magma, chimneys);
		assertTrue(magma > 5, "only " + magma + " blocks of magma");
		assertTrue(chimneys > 10, "only " + chimneys + " blocks of chimney");
	}

	@Test
	void whaleFallsLieOnTheDeepFloor() {
		CaveWorld world = world("default");
		WhaleFalls falls = new WhaleFalls(SEED, 1.0F);
		// the deep sea off the default preset's first coast
		world.carve(1600, -1600, 2600, -600, falls);
		int bones = world.count((state) -> state.is(Blocks.BONE_BLOCK));
		System.out.printf("whale falls: %d blocks of bone%n", bones);
		assertTrue(bones > 40, "only " + bones + " blocks of bone");
	}
}
