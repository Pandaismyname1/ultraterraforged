package raccoonman.reterraforged.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.platform.ConfigUtil;

// config/reterraforged/client.json
public record ClientConfig(boolean defaultWorldType) {
	private static final String DEFAULT_WORLD_TYPE = "useAsDefaultWorldType";

	public static ClientConfig load() {
		Path file = ConfigUtil.rtf("client.json");
		ClientConfig defaults = new ClientConfig(true);
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
