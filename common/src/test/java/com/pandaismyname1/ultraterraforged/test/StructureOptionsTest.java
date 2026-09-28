package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.preset.option.BoolOption;
import com.pandaismyname1.ultraterraforged.preset.option.Category;
import com.pandaismyname1.ultraterraforged.preset.option.IntOption;
import com.pandaismyname1.ultraterraforged.preset.option.Option;
import com.pandaismyname1.ultraterraforged.preset.option.Page;
import com.pandaismyname1.ultraterraforged.preset.option.StructureOptions;

public class StructureOptionsTest {
	private static final Identifier VILLAGES = Identifier.parse("villages");
	private static Map<String, Option<?>> options;

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
		Page page = StructureOptions.page(VanillaRegistries.createLookup().lookupOrThrow(Registries.STRUCTURE_SET), (holder) -> true);
		options = page.options().collect(Collectors.toMap(Option::path, Function.identity()));
		assertTrue(page.categories().stream().map(Category::id).anyMatch("minecraft:strongholds"::equals));
	}

	@Test
	void defaultsComeFromTheRegisteredStructureSet() {
		Preset preset = preset();
		// vanilla villages: spacing 34, separation 8
		assertEquals(34, option("structures.minecraft:villages.spacing").get(preset));
		assertEquals(8, option("structures.minecraft:villages.separation").get(preset));
		assertEquals(128, option("structures.minecraft:strongholds.count").get(preset));
		assertTrue(preset.structures().entries.isEmpty());
	}

	@Test
	void overridesAreStoredAndRemovedWhenReset() {
		Preset preset = preset();
		IntOption spacing = intOption("structures.minecraft:villages.spacing");
		spacing.set(preset, 20);
		assertEquals(20, preset.structures().get(VILLAGES).orElseThrow().spacing);
		spacing.reset(preset);
		assertTrue(preset.structures().entries.isEmpty(), "resetting should drop the override");
	}

	@Test
	void separationStaysBelowSpacing() {
		Preset preset = preset();
		intOption("structures.minecraft:villages.spacing").set(preset, 10);
		assertEquals(9, intOption("structures.minecraft:villages.separation").set(preset, 30));
		// and spacing can't be pulled down onto the separation
		assertEquals(10, intOption("structures.minecraft:villages.spacing").set(preset, 2));
	}

	@Test
	void disablingDeactivatesTheOtherOptions() {
		Preset preset = preset();
		BoolOption enabled = (BoolOption) option("structures.minecraft:villages.enabled");
		enabled.set(preset, false);
		assertEquals(Boolean.FALSE, preset.structures().get(VILLAGES).orElseThrow().enabled);
		assertFalse(option("structures.minecraft:villages.spacing").isActive(preset));
		enabled.set(preset, true);
		assertTrue(preset.structures().entries.isEmpty());
	}

	@Test
	void displayNamesAreReadable() {
		assertEquals("Woodland Mansions", StructureOptions.displayName(Identifier.parse("woodland_mansions")).getString());
		assertEquals("Big Towers (somemod)", StructureOptions.displayName(Identifier.fromNamespaceAndPath("somemod", "big_towers")).getString());
	}

	@Test
	void migratesStructureSettingsFrom006() {
		// layout written by 0.0.6, whose structure settings were never applied
		JsonObject json = PresetOptionsTest.encode(preset());
		json.remove("version");
		json.add("structures", JsonParser.parseString("{\"structures\": {\"minecraft:villages\": {\"spacing\": 40, \"separation\": 10, \"salt\": 10387312, \"disabled\": false}, \"minecraft:igloos\": {\"spacing\": 32, \"separation\": 8, \"salt\": 14357618, \"disabled\": true}}}"));
		Preset preset = Preset.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow((error) -> new AssertionError(error));
		assertEquals(40, preset.structures().get(VILLAGES).orElseThrow().spacing);
		assertEquals(null, preset.structures().get(VILLAGES).orElseThrow().enabled);
		assertEquals(Boolean.FALSE, preset.structures().get(Identifier.parse("igloos")).orElseThrow().enabled);
	}

	private static Preset preset() {
		return BuiltinPresetRenderTest.presets().get("default").get();
	}

	private static Option<?> option(String path) {
		Option<?> option = options.get(path);
		if (option == null) {
			throw new AssertionError("no option " + path + " in " + options.keySet());
		}
		return option;
	}

	private static IntOption intOption(String path) {
		return (IntOption) option(path);
	}
}
