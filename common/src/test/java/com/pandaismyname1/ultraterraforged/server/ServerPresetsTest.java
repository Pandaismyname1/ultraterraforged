package com.pandaismyname1.ultraterraforged.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pandaismyname1.ultraterraforged.data.preset.PresetLibrary;
import com.pandaismyname1.ultraterraforged.data.preset.settings.BuiltinPresets;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;

public class ServerPresetsTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void recognisesTheLevelTypeAsVanillaWritesIt(@TempDir Path dir) throws Exception {
		Path properties = dir.resolve("server.properties");
		// vanilla escapes the colon when it saves server.properties
		Files.writeString(properties, "level-name=world\nlevel-type=ultraterraforged\\:ultraterraforged\n");
		assertTrue(ServerPresets.isUltraTerraForgedLevelType(properties));
		Files.writeString(properties, "level-type=UltraTerraForged:UltraTerraForged\n");
		assertTrue(ServerPresets.isUltraTerraForgedLevelType(properties));
		Files.writeString(properties, "level-type=minecraft\\:normal\n");
		assertFalse(ServerPresets.isUltraTerraForgedLevelType(properties));
		assertFalse(ServerPresets.isUltraTerraForgedLevelType(dir.resolve("missing.properties")));
	}

	@Test
	void writesTheDefaultPresetOnceAndReadsEditsBack(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("ultraterraforged").resolve(ServerPresets.PRESET_FILE);
		// e.g. the modpack's default
		PresetLibrary.Entry highlands = PresetLibrary.find("highlands", PresetLibrary.builtins()).orElseThrow();
		Preset created = ServerPresets.loadOrCreatePreset(file, () -> highlands);
		assertTrue(Files.exists(file));
		assertEquals(BuiltinPresets.makeHighlands().terrain().mountains.weight, created.terrain().mountains.weight);

		Files.writeString(file, Files.readString(file).replace("\"riverCount\": " + created.rivers().riverCount, "\"riverCount\": 3"));
		assertEquals(3, ServerPresets.loadOrCreatePreset(file, () -> highlands).rivers().riverCount);
	}
}
