package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import raccoonman.reterraforged.data.preset.settings.CaveSettings;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.data.preset.settings.PresetFormat;
import raccoonman.reterraforged.preset.option.PresetOptions;

public class PresetFormatTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	// fixtures were written by the ReTerraForged 0.0.6 code (1.20.1 branch before the 1.20.2 merge)
	@ParameterizedTest
	@ValueSource(strings = { "0.0.6_default", "0.0.6_beautiful" })
	void readsPresetsSavedBy006(String name) throws Exception {
		JsonObject json = fixture(name);
		Preset preset = parse(json);
		assertEquals(json.getAsJsonObject("world").getAsJsonObject("continent").get("continentScale").getAsInt(), PresetOptions.CONTINENT_SCALE.get(preset));
		assertEquals(json.getAsJsonObject("rivers").get("riverCount").getAsInt(), preset.rivers().riverCount);
		assertEquals(json.getAsJsonObject("caves").get("caveCarverProbability").getAsFloat(), preset.caves().caveCarverProbability);
		assertEquals(json.getAsJsonObject("caves").get("entranceCaveProbability").getAsFloat(), preset.caves().entranceCaveProbability);
		// settings that no longer exist are dropped, everything else survives a round trip
		assertEquals(PresetOptionsTest.encode(preset), PresetOptionsTest.encode(parse(PresetOptionsTest.encode(preset))));
	}

	@Test
	void migratesMushroomIslandPoints() throws Exception {
		// 0.0.6 only wrote these when they differed from the default
		JsonObject json = fixture("0.0.6_default");
		JsonObject controlPoints = json.getAsJsonObject("world").getAsJsonObject("controlPoints");
		controlPoints.addProperty("mushroomFieldsInland", 0.05F);
		controlPoints.addProperty("mushroomFieldsCoast", 0.2F);

		Preset preset = parse(json);
		assertEquals(0.05F, PresetOptions.ISLAND_INLAND.get(preset));
		assertEquals(0.2F, PresetOptions.ISLAND_COAST.get(preset));
	}

	@Test
	void presetsFrom007GetVanillaCaves() {
		// 0.0.7 ignored cave settings and wrote an empty caves section
		JsonObject json = PresetOptionsTest.encode(BuiltinPresetRenderTest.presets().get("default").get());
		json.remove(PresetFormat.VERSION_KEY);
		json.add("caves", new JsonObject());
		assertEquals(PresetOptionsTest.encode(presetWithCaves(CaveSettings.makeVanilla())).get("caves"), PresetOptionsTest.encode(parse(json)).get("caves"));
	}

	private static Preset presetWithCaves(CaveSettings caves) {
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		return new Preset(preset.world(), preset.surface(), caves, preset.climate(), preset.terrain(), preset.rivers(), preset.filters(), preset.structures(), preset.miscellaneous());
	}

	@Test
	void writesCurrentVersionFirst() {
		JsonObject json = PresetOptionsTest.encode(BuiltinPresetRenderTest.presets().get("default").get());
		Map.Entry<String, JsonElement> first = json.entrySet().iterator().next();
		assertEquals(PresetFormat.VERSION_KEY, first.getKey());
		assertEquals(PresetFormat.CURRENT_VERSION, first.getValue().getAsInt());
	}

	@Test
	void everyBuiltinPresetRoundTrips() {
		BuiltinPresetRenderTest.presets().forEach((name, supplier) -> {
			JsonObject encoded = PresetOptionsTest.encode(supplier.get());
			assertEquals(encoded, PresetOptionsTest.encode(parse(encoded)), name);
		});
	}

	@Test
	void rejectsPresetsFromNewerVersions() {
		JsonObject json = PresetOptionsTest.encode(BuiltinPresetRenderTest.presets().get("default").get());
		json.addProperty(PresetFormat.VERSION_KEY, PresetFormat.CURRENT_VERSION + 1);
		DataResult<Preset> result = Preset.CODEC.parse(JsonOps.INSTANCE, json);
		assertTrue(result.error().isPresent());
		assertTrue(result.error().get().message().contains("newer ReTerraForged"), result.error().get().message());
	}

	private static Preset parse(JsonObject json) {
		return Preset.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, (error) -> {
			throw new AssertionError(error);
		});
	}

	private static JsonObject fixture(String name) throws Exception {
		try (Reader reader = new InputStreamReader(PresetFormatTest.class.getResourceAsStream("/fixtures/" + name + ".json"))) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}
}
