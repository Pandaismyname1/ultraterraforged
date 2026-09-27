package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.tile.Tile;

/**
 * Tiles hand their arrays back to a pool when closed, and the next tile takes them. Nothing done through a closed
 * tile may reach the tile that took its arrays: once, a late read left a chunk behind that made the next tile fail
 * with an ArrayIndexOutOfBoundsException while generating.
 */
public class TilePoolTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void readsFromAClosedTileDontReachTheNext() {
		GeneratorContext context = GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(), PresetRenderer.SEED, 3, 1, false);
		Tile first = context.generator.generate(10, 10).join();
		int chunkX = (10 << 3) + 3;
		int chunkZ = (10 << 3) + 5;
		first.close();
		// a late reader, after the tile's arrays went back to the pool
		first.getChunkReader(chunkX, chunkZ);

		Tile second = context.generator.generate(-7, 4).join();
		Tile fresh = GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(), PresetRenderer.SEED, 3, 1, false).generator.generate(-7, 4).join();
		int x = (-7 << 3) + 2;
		int z = (4 << 3) + 6;
		Tile.Chunk chunk = second.getChunkReader(x, z);
		Tile.Chunk expected = fresh.getChunkReader(x, z);
		assertEquals(x, chunk.getChunkX());
		assertEquals(z, chunk.getChunkZ());
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				Cell cell = chunk.getCell(dx, dz);
				assertEquals(expected.getCell(dx, dz).height, cell.height, "the reused tile differs from a fresh one at " + dx + " " + dz);
			}
		}
	}
}
