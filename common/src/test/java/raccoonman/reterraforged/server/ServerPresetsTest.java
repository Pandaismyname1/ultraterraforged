package raccoonman.reterraforged.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import raccoonman.reterraforged.data.preset.PresetLibrary;
import raccoonman.reterraforged.data.preset.settings.BuiltinPresets;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.test.TestBootstrap;

public class ServerPresetsTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void recognisesTheLevelTypeAsVanillaWritesIt(@TempDir Path dir) throws Exception {
		Path properties = dir.resolve("server.properties");
		// vanilla escapes the colon when it saves server.properties
		Files.writeString(properties, "level-name=world\nlevel-type=reterraforged\\:reterraforged\n");
		assertTrue(ServerPresets.isReTerraForgedLevelType(properties));
		Files.writeString(properties, "level-type=ReTerraForged:ReTerraForged\n");
		assertTrue(ServerPresets.isReTerraForgedLevelType(properties));
		Files.writeString(properties, "level-type=minecraft\\:normal\n");
		assertFalse(ServerPresets.isReTerraForgedLevelType(properties));
		assertFalse(ServerPresets.isReTerraForgedLevelType(dir.resolve("missing.properties")));
	}

	@Test
	void writesTheDefaultPresetOnceAndReadsEditsBack(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("reterraforged").resolve(ServerPresets.PRESET_FILE);
		// e.g. the modpack's default
		PresetLibrary.Entry highlands = PresetLibrary.find("highlands", PresetLibrary.builtins()).orElseThrow();
		Preset created = ServerPresets.loadOrCreatePreset(file, () -> highlands);
		assertTrue(Files.exists(file));
		assertEquals(BuiltinPresets.makeHighlands().terrain().mountains.weight, created.terrain().mountains.weight);

		Files.writeString(file, Files.readString(file).replace("\"riverCount\": " + created.rivers().riverCount, "\"riverCount\": 3"));
		assertEquals(3, ServerPresets.loadOrCreatePreset(file, () -> highlands).rivers().riverCount);
	}
}
