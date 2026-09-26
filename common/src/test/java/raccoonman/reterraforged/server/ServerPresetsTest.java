package raccoonman.reterraforged.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
		Preset created = ServerPresets.loadOrCreatePreset(file);
		assertTrue(Files.exists(file));

		Files.writeString(file, Files.readString(file).replace("\"riverCount\": " + created.rivers().riverCount, "\"riverCount\": 3"));
		assertEquals(3, ServerPresets.loadOrCreatePreset(file).rivers().riverCount);
	}
}
