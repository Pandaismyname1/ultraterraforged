package com.pandaismyname1.ultraterraforged.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.platform.ConfigUtil;

/**
 * Defaults a modpack ships in {@code config/ultraterraforged/modpack.json}, next to its own presets in
 * {@code config/ultraterraforged/modpack-presets}. Players can still pick and edit any preset; this only decides what
 * they start from. Nothing here is written by the mod, so a modpack update can replace these files without touching
 * anything the player saved.
 *
 * @param defaultPreset the preset new worlds start from: a modpack preset's file name (without .json), a built-in
 *                      preset's id such as "highlands", or a full id such as "modpack/My Preset.json"
 * @param showBuiltinPresets whether the built-in presets are offered alongside the modpack's
 * @param defaultWorldType whether UltraTerraForged is selected by default in Create World, for players who haven't chosen
 */
public record ModpackConfig(@Nullable String defaultPreset, boolean showBuiltinPresets, @Nullable Boolean defaultWorldType) {
	public static final String FILE = "modpack.json";
	public static final String PRESET_FOLDER = "modpack-presets";
	public static final ModpackConfig NONE = new ModpackConfig(null, true, null);

	private static final String DEFAULT_PRESET = "defaultPreset";
	private static final String SHOW_BUILTIN_PRESETS = "showBuiltinPresets";
	private static final String DEFAULT_WORLD_TYPE = "useAsDefaultWorldType";

	public static ModpackConfig load() {
		return load(ConfigUtil.utf(FILE));
	}

	public static Path presetFolder() {
		return ConfigUtil.utf(PRESET_FOLDER);
	}

	public static ModpackConfig load(Path file) {
		if (!Files.exists(file)) {
			return NONE;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			return new ModpackConfig(
				json.has(DEFAULT_PRESET) ? json.get(DEFAULT_PRESET).getAsString() : null,
				!json.has(SHOW_BUILTIN_PRESETS) || json.get(SHOW_BUILTIN_PRESETS).getAsBoolean(),
				json.has(DEFAULT_WORLD_TYPE) ? json.get(DEFAULT_WORLD_TYPE).getAsBoolean() : null
			);
		} catch (IOException | RuntimeException e) {
			UTFCommon.LOGGER.error("Couldn't read {}, ignoring the modpack defaults", file, e);
			return NONE;
		}
	}
}
