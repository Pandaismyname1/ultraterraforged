package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.data.preset.settings.PresetFormat;
import raccoonman.reterraforged.preset.option.BoolOption;
import raccoonman.reterraforged.preset.option.Category;
import raccoonman.reterraforged.preset.option.EnumOption;
import raccoonman.reterraforged.preset.option.FloatOption;
import raccoonman.reterraforged.preset.option.IntOption;
import raccoonman.reterraforged.preset.option.NumberOption;
import raccoonman.reterraforged.preset.option.Option;
import raccoonman.reterraforged.preset.option.OptionTag;
import raccoonman.reterraforged.preset.option.Page;
import raccoonman.reterraforged.preset.option.PresetOptions;

public class PresetOptionsTest {
	// settings present in the preset file that intentionally have no option
	private static final Set<String> NOT_EXPOSED = Set.of(
		PresetFormat.VERSION_KEY,
		"surface.erosion.snowHeight",
		// structure options are generated per structure set from the registry, see StructureOptionsTest
		"structures"
	);

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void pathsAreUnique() {
		Set<String> seen = new HashSet<>();
		PresetOptions.all().forEach((option) -> assertTrue(seen.add(option.path()), "duplicate option path " + option.path()));
	}

	// Setting an option must change exactly the value at its path in the encoded preset, which proves the
	// getter/setter pair is bound to the setting the path claims
	@Test
	void everyOptionIsBoundToTheValueAtItsPath() {
		Map<String, JsonElement> defaults = flatten(encode(BuiltinPresetRenderTest.presets().get("default").get()));
		List<String> failures = new ArrayList<>();
		PresetOptions.all().forEach((option) -> {
			Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
			Object before = option.get(preset);
			Object after = changeValue(option, preset);
			if (before.equals(after)) {
				failures.add(option.path() + ": couldn't pick a different valid value");
				return;
			}
			Map<String, JsonElement> changed = flatten(encode(preset));
			// fields equal to their codec default are left out of the file, so compare the union of keys
			Set<String> keys = new HashSet<>(defaults.keySet());
			keys.addAll(changed.keySet());
			Set<String> differences = keys.stream().filter((key) -> !java.util.Objects.equals(defaults.get(key), changed.get(key))).collect(Collectors.toSet());
			if (!differences.equals(Set.of(option.path()))) {
				failures.add(option.path() + ": changed " + differences);
			} else if (!sameValue(changed.get(option.path()), after)) {
				failures.add(option.path() + ": file has " + changed.get(option.path()) + " but option returned " + after);
			}
		});
		assertTrue(failures.isEmpty(), String.join("\n", failures));
	}

	// New settings added to the preset file must get an option, or be listed in NOT_EXPOSED
	@Test
	void everySettingHasAnOption() {
		Set<String> paths = PresetOptions.all().map(Option::path).collect(Collectors.toSet());
		Set<String> uncovered = new TreeMap<>(flatten(encode(BuiltinPresetRenderTest.presets().get("default").get()))).keySet().stream()
			.filter((key) -> !paths.contains(key) && !NOT_EXPOSED.contains(key))
			.collect(Collectors.toSet());
		assertTrue(uncovered.isEmpty(), "settings without an option: " + uncovered);
	}

	@Test
	void builtinPresetsAreValid() {
		List<String> failures = new ArrayList<>();
		BuiltinPresetRenderTest.presets().forEach((name, supplier) -> {
			Preset preset = supplier.get();
			PresetOptions.all().forEach((option) -> {
				if (!isValid(option, preset)) {
					failures.add(name + " " + option.path() + " = " + option.get(preset));
				}
			});
		});
		assertTrue(failures.isEmpty(), "built-in presets outside option constraints:\n" + String.join("\n", failures));
	}

	@Test
	void numberOptionsClampAndSnap() {
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		assertEquals(1024, PresetOptions.WORLD_HEIGHT.set(preset, 5000));
		assertEquals(16, PresetOptions.WORLD_HEIGHT.set(preset, 3));
		assertEquals(320, PresetOptions.WORLD_HEIGHT.set(preset, 330));

		// deep ocean can't pass shallow ocean, and moving shallow ocean below it is blocked too
		float shallow = PresetOptions.SHALLOW_OCEAN.get(preset);
		assertEquals(shallow, PresetOptions.DEEP_OCEAN.set(preset, 0.9F));
		assertEquals(shallow, PresetOptions.SHALLOW_OCEAN.set(preset, 0.0F));
	}

	@Test
	void resetRestoresTheDefault() {
		Preset preset = BuiltinPresetRenderTest.presets().get("legacy_huge_biomes").get();
		PresetOptions.all().forEach((option) -> option.reset(preset));
		// resetting in page order can be blocked by a neighbour that hasn't been reset yet, so reset twice
		PresetOptions.all().forEach((option) -> option.reset(preset));
		assertEquals(encode(BuiltinPresetRenderTest.presets().get("default").get()), encode(preset));
		PresetOptions.all().forEach((option) -> assertTrue(option.isDefault(preset), option.path()));
	}

	@Test
	void conditionsFollowContinentType() {
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		PresetOptions.CONTINENT_TYPE.set(preset, raccoonman.reterraforged.world.worldgen.continent.ContinentType.MULTI);
		assertTrue(PresetOptions.CONTINENT_SHAPE.isActive(preset));
		assertFalse(PresetOptions.CONTINENT_SKIPPING.isActive(preset));
		PresetOptions.CONTINENT_TYPE.set(preset, raccoonman.reterraforged.world.worldgen.continent.ContinentType.MULTI_IMPROVED);
		assertFalse(PresetOptions.CONTINENT_SHAPE.isActive(preset));
		assertTrue(PresetOptions.CONTINENT_SKIPPING.isActive(preset));
	}

	@Test
	void everyShownTextIsTranslated() throws Exception {
		JsonObject lang;
		try (Reader reader = new InputStreamReader(PresetOptionsTest.class.getResourceAsStream("/assets/reterraforged/lang/en_us.json"))) {
			lang = JsonParser.parseReader(reader).getAsJsonObject();
		}
		List<String> missing = new ArrayList<>();
		for (Page page : PresetOptions.PAGES) {
			requireKey(lang, page.titleKey(), missing);
			for (Category category : page.categories()) {
				if (category.labelKey() != null) {
					requireKey(lang, category.labelKey(), missing);
				}
				for (Option<?> option : category.options()) {
					if (!option.isHidden()) {
						requireKey(lang, option.translationKey(), missing);
						requireKey(lang, option.tooltipKey(), missing);
					}
				}
			}
		}
		for (OptionTag tag : OptionTag.values()) {
			requireKey(lang, tag.translationKey(), missing);
		}
		assertTrue(missing.isEmpty(), "missing translations: " + missing);
	}

	@Test
	void unrelatedPresetsDontShareSettings() throws Exception {
		// older files have no surface section; each decoded preset must get its own copy of the default
		JsonObject json = encode(BuiltinPresetRenderTest.presets().get("default").get());
		json.remove("surface");
		Preset a = Preset.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, Assertions::fail);
		Preset b = Preset.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, Assertions::fail);
		assertTrue(a.surface() != b.surface());
	}

	private static boolean isValid(Option<?> option, Preset preset) {
		Object value = option.get(preset);
		if (option instanceof NumberOption<?> number) {
			Comparable<Object> comparable = cast(value);
			if (comparable.compareTo(number.min()) < 0 || comparable.compareTo(number.max()) > 0) {
				return false;
			}
		}
		// setting the current value must be a no-op, i.e. it already satisfies steps and cross-option bounds
		Preset copy = preset.copy();
		return value.equals(setUnchecked(option, copy, value));
	}

	private static Object changeValue(Option<?> option, Preset preset) {
		Object current = option.get(preset);
		if (option instanceof BoolOption bool) {
			return bool.set(preset, !(Boolean) current);
		}
		if (option instanceof EnumOption<?> enumOption) {
			for (Object value : enumOption.values()) {
				if (!value.equals(current)) {
					return setUnchecked(option, preset, value);
				}
			}
			return current;
		}
		if (option instanceof IntOption intOption) {
			if (intOption.isSeed()) {
				return intOption.set(preset, (Integer) current + 1);
			}
			for (int candidate : new int[] { intOption.max(), intOption.min() }) {
				Integer stored = intOption.set(preset, candidate);
				if (!stored.equals(current)) {
					return stored;
				}
			}
			return current;
		}
		if (option instanceof FloatOption floatOption) {
			for (float candidate : new float[] { floatOption.max(), floatOption.min() }) {
				Float stored = floatOption.set(preset, candidate);
				if (!stored.equals(current)) {
					return stored;
				}
			}
			return current;
		}
		fail("unhandled option type " + option);
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T> T setUnchecked(Option<T> option, Preset preset, Object value) {
		return option.set(preset, (T) value);
	}

	@SuppressWarnings("unchecked")
	private static Comparable<Object> cast(Object value) {
		return (Comparable<Object>) value;
	}

	private static boolean sameValue(JsonElement json, Object value) {
		if (value instanceof Float f) {
			return json.getAsFloat() == f;
		}
		if (value instanceof Integer i) {
			return json.getAsInt() == i;
		}
		if (value instanceof Boolean b) {
			return json.getAsBoolean() == b;
		}
		return true;
	}

	private static void requireKey(JsonObject lang, String key, List<String> missing) {
		if (!lang.has(key)) {
			missing.add(key);
		}
	}

	static JsonObject encode(Preset preset) {
		return Preset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow(false, Assertions::fail).getAsJsonObject();
	}

	static Map<String, JsonElement> flatten(JsonObject json) {
		Map<String, JsonElement> flat = new TreeMap<>();
		flatten("", json, flat);
		return flat;
	}

	private static void flatten(String prefix, JsonObject json, Map<String, JsonElement> flat) {
		json.entrySet().forEach((entry) -> {
			String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
			if (entry.getValue().isJsonObject() && !entry.getValue().getAsJsonObject().entrySet().isEmpty()) {
				flatten(key, entry.getValue().getAsJsonObject(), flat);
			} else {
				flat.put(key, entry.getValue());
			}
		});
	}

	// adapts JUnit's fail() to DFU's error callbacks
	private static final class Assertions {
		static void fail(String message) {
			org.junit.jupiter.api.Assertions.fail(message);
		}
	}
}
