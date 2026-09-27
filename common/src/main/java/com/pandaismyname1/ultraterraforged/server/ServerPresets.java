package com.pandaismyname1.ultraterraforged.server;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Supplier;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.registries.VanillaRegistries;
import com.pandaismyname1.ultraterraforged.RTFCommon;
import com.pandaismyname1.ultraterraforged.data.PresetPacks;
import com.pandaismyname1.ultraterraforged.data.preset.PresetLibrary;
import com.pandaismyname1.ultraterraforged.data.preset.RTFWorldPresets;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.platform.ConfigUtil;

/**
 * Dedicated server support: a new world whose server.properties has {@code level-type=ultraterraforged:ultraterraforged}
 * gets the preset from {@code config/ultraterraforged/server-preset.json} installed as a datapack before the world's
 * datapacks load. The file is created from the modpack's default preset if it doesn't exist yet. Vanilla enables new
 * packs in a world's datapacks folder automatically, and the pack then stays with the world like any other.
 */
public final class ServerPresets {
	public static final String PRESET_FILE = "server-preset.json";

	/**
	 * Called with a world's datapacks folder while the server sets up its pack repository.
	 */
	public static void installIfNewWorld(Path datapackDir) {
		Path worldDir = datapackDir.toAbsolutePath().getParent();
		if (worldDir == null || Files.exists(worldDir.resolve("level.dat"))) {
			return;
		}
		if (!isUltraTerraForgedLevelType(Path.of("server.properties"))) {
			return;
		}
		try {
			Preset preset = loadOrCreatePreset(ConfigUtil.rtf(PRESET_FILE), PresetLibrary::defaultPreset);
			// the server hasn't loaded its registries yet, so generate against vanilla's built-in worldgen
			PresetPacks.export(preset, VanillaRegistries.createLookup(), datapackDir.resolve(PresetPacks.WORLD_PACK_NAME));
			RTFCommon.LOGGER.info("Creating a UltraTerraForged world with the preset from {}", ConfigUtil.rtf(PRESET_FILE));
		} catch (IOException | RuntimeException e) {
			// failing here would silently produce vanilla terrain, so stop instead
			throw new IllegalStateException("Couldn't set up the UltraTerraForged preset for the new world", e);
		}
	}

	static boolean isUltraTerraForgedLevelType(Path serverProperties) {
		if (!Files.exists(serverProperties)) {
			return false;
		}
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(serverProperties)) {
			properties.load(reader);
		} catch (IOException e) {
			RTFCommon.LOGGER.warn("Couldn't read {}", serverProperties, e);
			return false;
		}
		String levelType = properties.getProperty("level-type", "").trim().toLowerCase(Locale.ROOT);
		return levelType.equals(RTFWorldPresets.ULTRATERRAFORGED.location().toString());
	}

	static Preset loadOrCreatePreset(Path file, Supplier<PresetLibrary.Entry> defaultPreset) throws IOException {
		if (!Files.exists(file)) {
			PresetLibrary.Entry defaults = defaultPreset.get();
			Preset preset = defaults.create();
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				JsonElement json = Preset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow(false, RTFCommon.LOGGER::error);
				new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
			}
			RTFCommon.LOGGER.info("Wrote the {} preset to {}; edit it to change the terrain of new worlds", defaults.id(), file);
			return preset;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			return Preset.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow(false, (error) -> {
				throw new IllegalStateException("Invalid preset in " + file + ": " + error);
			});
		}
	}

	private ServerPresets() {
	}
}
