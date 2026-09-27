package com.pandaismyname1.ultraterraforged.test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

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

		// under the water of a river, lake or wetland above the sea
		public boolean raisedWater(int x, int z) {
			Cell cell = this.cell(x, z);
			return cell.waterLevel > 0.0F && this.levels.scale(cell.height) < this.levels.scale(cell.waterLevel);
		}

		// the top water block of a river, lake or wetland above the sea, or the sea's
		public int waterY(int x, int z) {
			Cell cell = this.cell(x, z);
			return cell.waterLevel > 0.0F ? Math.max(this.levels.waterY, this.levels.scale(cell.waterLevel)) : this.levels.waterY;
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
				if (y < water || cell.terrain.isRiver() || cell.terrain.isLake() || view.raisedWater(x, z)) {
					float depth = clamp((Math.max(water, view.waterY(x, z)) - y) / 40.0F, 0.0F, 1.0F);
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

	// the quarter turns that put the most water at the camera's end of the map, to look from the sea at a coast
	public static int turnsFacingLand(View view) {
		int size = view.size();
		int water = view.levels().waterLevel;
		int[] wet = new int[4];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size / 4; j++) {
				// south, west, north, east strips
				wet[0] += view.blockY(i, size - 1 - j) < water ? 1 : 0;
				wet[1] += view.blockY(j, i) < water ? 1 : 0;
				wet[2] += view.blockY(i, j) < water ? 1 : 0;
				wet[3] += view.blockY(size - 1 - j, i) < water ? 1 : 0;
			}
		}
		int best = 0;
		for (int i = 1; i < 4; i++) {
			if (wet[i] > wet[best]) {
				best = i;
			}
		}
		// the camera stands at the south end; turning the map by best puts that side there
		return new int[] { 0, 3, 2, 1 }[best];
	}

	/**
	 * Where two views of the same place differ most, e.g. with and without a landform, as a block position.
	 */
	public static long mostChanged(View a, View b) {
		int best = -1;
		long found = Long.MIN_VALUE;
		for (int x = 0; x < a.size(); x++) {
			for (int z = 0; z < a.size(); z++) {
				int change = Math.abs(a.blockY(x, z) - b.blockY(x, z));
				if (change > best) {
					best = change;
					found = com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil.pack((int) a.blockX(x), (int) a.blockZ(z));
				}
			}
		}
		return found;
	}

	// a side view along the middle column, north to south
	public static void writeProfileZ(View view, String name) throws IOException {
		int size = view.size();
		int x = size / 2;
		int maxY = 0;
		for (int z = 0; z < size; z++) {
			maxY = Math.max(maxY, view.blockY(x, z));
		}
		int height = maxY + 20;
		int water = view.levels().waterLevel;
		BufferedImage image = new BufferedImage(size, height, BufferedImage.TYPE_INT_RGB);
		for (int z = 0; z < size; z++) {
			int ground = view.blockY(x, z);
			for (int y = 0; y < height; y++) {
				int color = y <= ground ? 0x7A6A55 : (y <= water ? 0x4070D0 : 0xD8E8F8);
				image.setRGB(z, height - 1 - y, color);
			}
		}
		Files.createDirectories(OUT);
		ImageIO.write(image, "png", OUT.resolve(name + ".png").toFile());
	}

	/**
	 * A 3D look at the terrain from above the south edge, facing north, drawn like an old voxel landscape: every screen
	 * column walks away from the camera and paints the ground that rises above what's already drawn.
	 */
	public static void writePerspective(View view, String name) throws IOException {
		writePerspective(view, name, 0);
	}

	/**
	 * @param turns quarter turns of the map, clockwise, before looking north; 1 looks from the east, i.e. faces west
	 */
	public static void writePerspective(View original, String name, int turns) throws IOException {
		View view = original;
		int size = view.size();
		int width = 640;
		int height = 400;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int sky = 0xBFD8F0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				image.setRGB(x, y, sky);
			}
		}
		int water = view.levels().waterLevel;
		// above the high ground, so mountains don't swallow the camera
		float[] heights = new float[size * size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				heights[i * size + j] = view.exactY(i, j);
			}
		}
		java.util.Arrays.sort(heights);
		float cameraHeight = Math.max(water + 70.0F, heights[heights.length * 95 / 100] + 45.0F);
		float horizon = height * 0.3F;
		float scale = 220.0F;
		for (int column = 0; column < width; column++) {
			int drawnTo = height;
			for (float distance = 4.0F; distance < size * 1.1F; distance += Math.max(0.5F, distance * 0.01F)) {
				// the camera stands half the map south of the south edge
				float mapZ = size + size * 0.1F - distance;
				float spread = distance * 1.1F;
				float mapX = size / 2.0F + (column - width / 2.0F) / width * spread;
				if (mapZ < 0 || mapZ >= size || mapX < 0 || mapX >= size) {
					continue;
				}
				int rx = (int) mapX;
				int rz = (int) mapZ;
				// turn the map under the camera
				int cx;
				int cz;
				switch (Math.floorMod(turns, 4)) {
				case 1 -> { cx = size - 1 - rz; cz = rx; }
				case 2 -> { cx = size - 1 - rx; cz = size - 1 - rz; }
				case 3 -> { cx = rz; cz = size - 1 - rx; }
				default -> { cx = rx; cz = rz; }
				}
				float ground = Math.max(view.exactY(cx, cz), water);
				int screenY = (int) ((cameraHeight - ground) / distance * scale + horizon);
				if (screenY < drawnTo) {
					int color = colorAt(view, cx, cz);
					for (int y = Math.max(0, screenY); y < drawnTo; y++) {
						image.setRGB(column, y, color);
					}
					drawnTo = Math.max(0, screenY);
				}
			}
		}
		Files.createDirectories(OUT);
		ImageIO.write(image, "png", OUT.resolve(name + ".png").toFile());
	}

	private static int colorAt(View view, int x, int z) {
		int water = view.levels().waterLevel;
		Cell cell = view.cell(x, z);
		float exact = view.exactY(x, z);
		if (exact < water || cell.terrain.isRiver() || cell.terrain.isLake()) {
			return rgb(0.25F, 0.45F, 0.8F);
		}
		float west = view.exactY(Math.max(0, x - 1), z);
		float south = view.exactY(x, Math.min(view.size() - 1, z + 1));
		// lit from the south west, towards the camera
		float light = ((exact - west) + (exact - south)) / (2.0F * view.zoom());
		float shade = clamp(0.8F + light * 0.5F, 0.3F, 1.3F);
		float slope = Math.abs(exact - west) + Math.abs(exact - south);
		// steep ground is bare rock
		if (slope > 3.0F * view.zoom()) {
			return rgb(0.55F * shade, 0.52F * shade, 0.5F * shade);
		}
		float h = clamp((exact - water) / 180.0F, 0.0F, 1.0F);
		return rgb(lerp(0.35F, 0.7F, h) * shade, lerp(0.55F, 0.6F, h) * shade, lerp(0.3F, 0.5F, h) * shade);
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
