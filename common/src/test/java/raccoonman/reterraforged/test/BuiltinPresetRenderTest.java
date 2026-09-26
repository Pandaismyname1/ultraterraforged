package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import raccoonman.reterraforged.client.gui.screen.presetconfig.RenderMode;
import raccoonman.reterraforged.data.preset.settings.BuiltinPresets;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.tile.Tile;

/**
 * Renders every built-in preset and compares a fingerprint of the generated terrain against
 * golden values, so refactors of the preset model can't silently change worldgen.
 *
 * Images are written to common/build/preset-renders for eyeballing. After an intentional
 * terrain change, regenerate the golden file with: ./gradlew :common:test -Drtf.updateGolden=true
 */
public class BuiltinPresetRenderTest {
	private static final String GOLDEN_RESOURCE = "/golden/preset-fingerprints.properties";
	private static final Path GOLDEN_SOURCE = Path.of("src/test/resources/golden/preset-fingerprints.properties");
	private static final Path RENDER_DIR = Path.of("build/preset-renders");
	private static final boolean UPDATE = Boolean.getBoolean("rtf.updateGolden");

	private static final Map<String, String> ACTUAL = new TreeMap<>();

	public static Map<String, Supplier<Preset>> presets() {
		Map<String, Supplier<Preset>> presets = new LinkedHashMap<>();
		presets.put("default", BuiltinPresets::makeDefault);
		presets.put("legacy_default", BuiltinPresets::makeLegacyDefault);
		presets.put("legacy_vanillaish", BuiltinPresets::makeLegacyVanillaish);
		presets.put("legacy_beautiful", BuiltinPresets::makeLegacyBeautiful);
		presets.put("legacy_lite", BuiltinPresets::makeLegacyLite);
		presets.put("legacy_huge_biomes", BuiltinPresets::makeLegacyHugeBiomes);
		return presets;
	}

	@TestFactory
	Stream<DynamicTest> builtinPresetsGenerateExpectedTerrain() throws IOException {
		TestBootstrap.init();
		Properties golden = loadGolden();
		return presets().entrySet().stream().map((entry) -> DynamicTest.dynamicTest(entry.getKey(), () -> {
			Preset preset = entry.getValue().get();
			Tile tile = PresetRenderer.generate(preset);
			String fingerprint = PresetRenderer.fingerprint(tile);
			ACTUAL.put(entry.getKey(), fingerprint);

			PresetRenderer.writeImage(preset, tile, RenderMode.BIOME_TYPE, RENDER_DIR.resolve(entry.getKey() + ".png"));
			PresetRenderer.writeImage(preset, tile, RenderMode.TERRAIN_REGION, RENDER_DIR.resolve(entry.getKey() + "_terrain.png"));

			// generating twice must give the same result, otherwise the golden check below is meaningless
			assertEquals(fingerprint, PresetRenderer.fingerprint(PresetRenderer.generate(entry.getValue().get())), "terrain generation is not deterministic");

			if (!UPDATE) {
				String expected = golden.getProperty(entry.getKey());
				assertNotNull(expected, "no golden fingerprint for " + entry.getKey() + "; run with -Drtf.updateGolden=true");
				assertEquals(expected, fingerprint, "terrain for preset '" + entry.getKey() + "' changed (see " + RENDER_DIR + ")");
			}
		}));
	}

	@AfterAll
	static void writeGolden() throws IOException {
		if (UPDATE && !ACTUAL.isEmpty()) {
			Properties properties = new Properties();
			properties.putAll(ACTUAL);
			Files.createDirectories(GOLDEN_SOURCE.getParent());
			try (Writer writer = Files.newBufferedWriter(GOLDEN_SOURCE)) {
				properties.store(writer, "Terrain fingerprints of the built-in presets, see BuiltinPresetRenderTest");
			}
		}
	}

	private static Properties loadGolden() throws IOException {
		Properties properties = new Properties();
		try (InputStream stream = BuiltinPresetRenderTest.class.getResourceAsStream(GOLDEN_RESOURCE)) {
			if (stream != null) {
				try (Reader reader = new java.io.InputStreamReader(stream)) {
					properties.load(reader);
				}
			}
		}
		return properties;
	}
}
