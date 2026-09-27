package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import com.pandaismyname1.ultraterraforged.config.ModpackConfig;
import com.pandaismyname1.ultraterraforged.data.preset.PresetLibrary;
import com.pandaismyname1.ultraterraforged.data.preset.settings.BuiltinPresets;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

public class ModpackPresetsTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	// a modpack preset file: the settings plus an optional name and description
	private static void writePreset(Path file, Preset preset, String name, String description) throws Exception {
		JsonObject json = Preset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow().getAsJsonObject();
		if (name != null) {
			json.addProperty("name", name);
		}
		if (description != null) {
			json.addProperty("description", description);
		}
		Files.createDirectories(file.getParent());
		Files.writeString(file, json.toString());
	}

	private static Preset skyIslands() {
		Preset preset = BuiltinPresets.makeDefault();
		preset.rivers().riverCount = 3;
		return preset;
	}

	@Test
	void noModpackFileMeansNoChanges(@TempDir Path dir) {
		ModpackConfig config = ModpackConfig.load(dir.resolve(ModpackConfig.FILE));
		assertSame(ModpackConfig.NONE, config);

		List<PresetLibrary.Entry> shipped = PresetLibrary.shipped(config, dir.resolve(ModpackConfig.PRESET_FOLDER));
		assertEquals(PresetLibrary.builtins(), shipped);
		assertEquals(PresetLibrary.DEFAULT_ID, PresetLibrary.defaultPreset(config, shipped).id());
	}

	@Test
	void readsTheModpackFile(@TempDir Path dir) throws Exception {
		Path file = dir.resolve(ModpackConfig.FILE);
		Files.writeString(file, "{\"defaultPreset\": \"Sky Islands\", \"showBuiltinPresets\": false, \"useAsDefaultWorldType\": false}");
		ModpackConfig config = ModpackConfig.load(file);
		assertEquals("Sky Islands", config.defaultPreset());
		assertFalse(config.showBuiltinPresets());
		assertEquals(Boolean.FALSE, config.defaultWorldType());

		Files.writeString(file, "{}");
		config = ModpackConfig.load(file);
		assertNull(config.defaultPreset());
		assertTrue(config.showBuiltinPresets());
		assertNull(config.defaultWorldType());

		// a broken file mustn't stop the game, it's just ignored
		Files.writeString(file, "{ not json");
		assertSame(ModpackConfig.NONE, ModpackConfig.load(file));
	}

	@Test
	void modpackPresetsComeFirstAndStartNewWorlds(@TempDir Path dir) throws Exception {
		Path folder = dir.resolve(ModpackConfig.PRESET_FOLDER);
		writePreset(folder.resolve("Sky Islands.json"), skyIslands(), null, null);
		writePreset(folder.resolve("tuned.json"), BuiltinPresets.makeHighlands(), "Tuned Highlands", "Taller mountains for this pack");
		Files.writeString(folder.resolve("broken.json"), "{}");

		ModpackConfig config = new ModpackConfig("sky islands", true, null);
		List<PresetLibrary.Entry> shipped = PresetLibrary.shipped(config, folder);
		assertEquals(2 + PresetLibrary.builtins().size(), shipped.size(), "the unreadable file is skipped");
		assertEquals("modpack/Sky Islands.json", shipped.get(0).id());
		assertEquals("Sky Islands", shipped.get(0).name().getString());
		assertNull(shipped.get(0).description());
		assertEquals("Tuned Highlands", shipped.get(1).name().getString());
		assertEquals("Taller mountains for this pack", shipped.get(1).description().getString());
		assertEquals(PresetLibrary.Source.MODPACK, shipped.get(1).source());

		PresetLibrary.Entry chosen = PresetLibrary.defaultPreset(config, shipped);
		assertSame(shipped.get(0), chosen);
		Preset first = chosen.create();
		assertEquals(3, first.rivers().riverCount);
		// players edit what they start from, never the modpack's file
		first.rivers().riverCount = 20;
		assertNotSame(first, chosen.create());
		assertEquals(3, chosen.create().rivers().riverCount);
	}

	@Test
	void theDefaultCanBeAnyShippedPreset(@TempDir Path dir) throws Exception {
		Path folder = dir.resolve(ModpackConfig.PRESET_FOLDER);
		writePreset(folder.resolve("highlands.json"), skyIslands(), null, null);
		List<PresetLibrary.Entry> shipped = PresetLibrary.shipped(ModpackConfig.NONE, folder);

		assertEquals("builtin/tropics", PresetLibrary.defaultPreset(new ModpackConfig("tropics", true, null), shipped).id());
		assertEquals("builtin/tropics", PresetLibrary.defaultPreset(new ModpackConfig("builtin/tropics", true, null), shipped).id());
		// a modpack preset shadows the built-in one with the same name, unless the full id is used
		assertEquals("modpack/highlands.json", PresetLibrary.defaultPreset(new ModpackConfig("Highlands", true, null), shipped).id());
		assertEquals("builtin/highlands", PresetLibrary.defaultPreset(new ModpackConfig("builtin/highlands", true, null), shipped).id());
		// a typo falls back to Default rather than failing
		assertEquals(PresetLibrary.DEFAULT_ID, PresetLibrary.defaultPreset(new ModpackConfig("hihglands", true, null), shipped).id());
	}

	@Test
	void builtinPresetsCanBeHidden(@TempDir Path dir) throws Exception {
		Path folder = dir.resolve(ModpackConfig.PRESET_FOLDER);
		ModpackConfig hidden = new ModpackConfig(null, false, null);
		// with nothing to replace them, the built-in presets stay
		assertEquals(PresetLibrary.builtins(), PresetLibrary.shipped(hidden, folder));

		writePreset(folder.resolve("a.json"), skyIslands(), null, null);
		writePreset(folder.resolve("b.json"), BuiltinPresets.makeTropics(), null, null);
		List<PresetLibrary.Entry> shipped = PresetLibrary.shipped(hidden, folder);
		assertEquals(List.of("modpack/a.json", "modpack/b.json"), shipped.stream().map(PresetLibrary.Entry::id).toList());
		assertEquals("modpack/a.json", PresetLibrary.defaultPreset(hidden, shipped).id());
		assertEquals("modpack/b.json", PresetLibrary.defaultPreset(new ModpackConfig("b", false, null), shipped).id());
	}
}
