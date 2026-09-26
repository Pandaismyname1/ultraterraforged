package raccoonman.reterraforged.test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.data.preset.settings.WorldSettings;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.tile.Tile;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

/**
 * Shaded relief maps of generated terrain, for looking at terrain changes without starting the game. Heights come
 * from the same heightmap the world is built from.
 */
public final class TerrainViews {
	public static final Path OUT = Path.of("build", "terrain-views");

	public record View(Tile tile, float centerX, float centerZ, float zoom, Levels levels) {

		public int size() {
			return this.tile.getBlockSize().size();
		}

		public Cell cell(int x, int z) {
			return this.tile.lookup(x, z);
		}

		public int blockY(int x, int z) {
			return this.levels.scale(this.cell(x, z).height);
		}

		public float exactY(int x, int z) {
			return this.cell(x, z).height * this.levels.worldHeight;
		}

		public float blockX(int x) {
			return this.centerX + (x - this.size() / 2.0F) * this.zoom;
		}

		public float blockZ(int z) {
			return this.centerZ + (z - this.size() / 2.0F) * this.zoom;
		}
	}

	public static Levels levels(Preset preset) {
		WorldSettings.Properties properties = preset.world().properties;
		return new Levels(properties.terrainScaler(), properties.seaLevel);
	}

	public static View view(Preset preset, float centerX, float centerZ, float zoom) {
		GeneratorContext context = GeneratorContext.makeUncached(preset, PresetRenderer.SEED, PresetRenderer.TILE_SIZE, 0, 6);
		Tile tile = context.generator.generateZoomed(centerX, centerZ, zoom, false).join();
		return new View(tile, centerX, centerZ, zoom, levels(preset));
	}

	/**
	 * Searches outward from the origin at a coarse zoom for a cell matching the test, e.g. a river between high
	 * ground, and returns its block position, or null.
	 */
	public static long find(Preset preset, float searchZoom, Predicate<Cell> test) {
		View view = view(preset, 0.0F, 0.0F, searchZoom);
		int size = view.size();
		int best = Integer.MAX_VALUE;
		long found = Long.MIN_VALUE;
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				if (test.test(view.cell(x, z))) {
					int dx = x - size / 2;
					int dz = z - size / 2;
					int distance = dx * dx + dz * dz;
					if (distance < best) {
						best = distance;
						found = PosUtil.pack((int) view.blockX(x), (int) view.blockZ(z));
					}
				}
			}
		}
		return found;
	}

	// hillshade lit from the north west, tinted by height, with water in blue
	public static void write(View view, String name) throws IOException {
		int size = view.size();
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		int water = view.levels().waterLevel;
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				Cell cell = view.cell(x, z);
				int y = view.blockY(x, z);
				// exact heights, rounded ones would draw every block step as a contour line
				float exact = view.exactY(x, z);
				float west = view.exactY(Math.max(0, x - 1), z);
				float north = view.exactY(x, Math.max(0, z - 1));
				// slope per block towards the light
				float light = ((exact - west) + (exact - north)) / (2.0F * view.zoom());
				float shade = clamp(0.75F + light * 0.6F, 0.25F, 1.25F);
				int color;
				if (y < water || cell.terrain.isRiver() || cell.terrain.isLake()) {
					float depth = clamp((water - y) / 40.0F, 0.0F, 1.0F);
					color = rgb(0.25F - depth * 0.15F, 0.45F - depth * 0.2F, 0.85F - depth * 0.3F);
				} else {
					float h = clamp((y - water) / 180.0F, 0.0F, 1.0F);
					// green lowland to brown hills to grey peaks and white tops
					float r = h < 0.5F ? lerp(0.35F, 0.6F, h * 2) : lerp(0.6F, 0.92F, (h - 0.5F) * 2);
					float g = h < 0.5F ? lerp(0.55F, 0.5F, h * 2) : lerp(0.5F, 0.92F, (h - 0.5F) * 2);
					float b = h < 0.5F ? lerp(0.3F, 0.35F, h * 2) : lerp(0.35F, 0.92F, (h - 0.5F) * 2);
					color = rgb(r * shade, g * shade, b * shade);
				}
				image.setRGB(x, z, color);
			}
		}
		Files.createDirectories(OUT);
		ImageIO.write(image, "png", OUT.resolve(name + ".png").toFile());
	}

	/**
	 * A side view of the terrain along the middle row of the view, one pixel per block of height.
	 */
	public static void writeProfile(View view, String name) throws IOException {
		int size = view.size();
		int z = size / 2;
		int maxY = 0;
		for (int x = 0; x < size; x++) {
			maxY = Math.max(maxY, view.blockY(x, z));
		}
		int height = maxY + 20;
		int water = view.levels().waterLevel;
		BufferedImage image = new BufferedImage(size, height, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < size; x++) {
			int ground = view.blockY(x, z);
			for (int y = 0; y < height; y++) {
				int color = y <= ground ? 0x7A6A55 : (y <= water ? 0x4070D0 : 0xD8E8F8);
				image.setRGB(x, height - 1 - y, color);
			}
		}
		Files.createDirectories(OUT);
		ImageIO.write(image, "png", OUT.resolve(name + ".png").toFile());
	}

	private static int rgb(float r, float g, float b) {
		return ((int) (clamp(r, 0, 1) * 255) << 16) | ((int) (clamp(g, 0, 1) * 255) << 8) | (int) (clamp(b, 0, 1) * 255);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private TerrainViews() {
	}
}
