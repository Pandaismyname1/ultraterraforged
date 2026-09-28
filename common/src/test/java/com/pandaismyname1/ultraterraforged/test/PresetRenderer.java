package com.pandaismyname1.ultraterraforged.test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.imageio.ImageIO;

import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.RenderMode;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

// Generates terrain for a preset the same way the editor preview does, without Minecraft running
public final class PresetRenderer {
	public static final int SEED = 1234;
	public static final int TILE_SIZE = 4;
	public static final float ZOOM = 40.0F;

	public static Tile generate(Preset preset) {
		GeneratorContext context = GeneratorContext.makeUncached(preset, SEED, TILE_SIZE, 0, 6);
		return context.generator.generateZoomed(0.0F, 0.0F, ZOOM, false).join();
	}

	// Stable across runs as long as terrain generation is unchanged; heights are quantized to absorb float noise
	public static String fingerprint(Tile tile) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		ByteBuffer buffer = ByteBuffer.allocate(12);
		tile.iterate((cell, x, z) -> {
			buffer.clear();
			buffer.putInt(Math.round(cell.height * 4096.0F));
			buffer.putInt(cell.terrain.getName().hashCode());
			buffer.putInt(cell.biomeType.ordinal());
			digest.update(buffer.array());
		});
		return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
	}

	public static void writeImage(Preset preset, Tile tile, RenderMode mode, Path path) throws IOException {
		WorldSettings.Properties properties = preset.world().properties;
		Levels levels = new Levels(properties.terrainScaler(), properties.seaLevel);
		int size = tile.getBlockSize().size();
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		tile.iterate((cell, x, z) -> {
			// RenderMode packs colors as ARGB, as NativeImage takes them since 1.21.2
			image.setRGB(x, z, mode.getColor(cell, levels) & 0xFFFFFF);
		});
		Files.createDirectories(path.getParent());
		ImageIO.write(image, "png", path.toFile());
	}
}
