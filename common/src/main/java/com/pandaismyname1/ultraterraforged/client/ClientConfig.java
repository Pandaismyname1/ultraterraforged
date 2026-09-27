package com.pandaismyname1.ultraterraforged.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.pandaismyname1.ultraterraforged.RTFCommon;
import com.pandaismyname1.ultraterraforged.config.ModpackConfig;
import com.pandaismyname1.ultraterraforged.platform.ConfigUtil;

// config/ultraterraforged/client.json; a player's own choices, which take precedence over the modpack's in modpack.json
public record ClientConfig(boolean defaultWorldType) {
	private static final String DEFAULT_WORLD_TYPE = "useAsDefaultWorldType";

	public static ClientConfig load() {
		Path file = ConfigUtil.rtf("client.json");
		Boolean modpackWorldType = ModpackConfig.load().defaultWorldType();
		ClientConfig defaults = new ClientConfig(modpackWorldType == null || modpackWorldType);
		if (!Files.exists(file)) {
			defaults.save(file);
			return defaults;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			return new ClientConfig(json.has(DEFAULT_WORLD_TYPE) ? json.get(DEFAULT_WORLD_TYPE).getAsBoolean() : defaults.defaultWorldType);
		} catch (IOException | RuntimeException e) {
			RTFCommon.LOGGER.error("Couldn't read {}, using defaults", file, e);
			return defaults;
		}
	}

	private void save(Path file) {
		JsonObject json = new JsonObject();
		json.addProperty(DEFAULT_WORLD_TYPE, this.defaultWorldType);
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
			}
		} catch (IOException e) {
			RTFCommon.LOGGER.error("Couldn't write {}", file, e);
		}
	}
}
