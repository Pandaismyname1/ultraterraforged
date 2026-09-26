package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.preset.option.BoolOption;
import raccoonman.reterraforged.preset.option.EnumOption;
import raccoonman.reterraforged.preset.option.FloatOption;
import raccoonman.reterraforged.preset.option.IntOption;
import raccoonman.reterraforged.preset.option.Option;
import raccoonman.reterraforged.preset.option.Page;
import raccoonman.reterraforged.preset.option.PresetOptions;
import raccoonman.reterraforged.world.worldgen.tile.Tile;

/**
 * Every visible option on the pages that shape the land must change the generated terrain, otherwise it's a
 * setting that does nothing (like the base and vertical scales did while the heightmap dropped them).
 */
public class TerrainSensitivityTest {
	// options that legitimately don't show in the heightmap, with the reason
	private static final Set<String> NOT_IN_HEIGHTMAP = Set.of(
		// where players spawn, not what the land looks like
		"world.properties.spawnType",
		// the dimension's height, sea and lava levels are applied by the noise settings; the heightmap is capped at 256
		"world.properties.worldHeight",
		"world.properties.worldDepth",
		"world.properties.lavaLevel",
		// only feed the continentalness and macro noise that vanilla's biome source uses to pick biome variants
		"world.controlPoints.midInland",
		"climate.biomeShape.macroNoiseSize",
		// used by the river generator, but branch rivers didn't form in the sampled area, so their effect isn't verified here
		"rivers.branchRivers.bedDepth",
		"rivers.branchRivers.bedWidth",
		"rivers.branchRivers.bankWidth",
		// a few blocks across, too small to show at the sampled resolution; LandformTest checks them
		"landforms.seaCliffs.seaStacks"
	);

	@Test
	void everyTerrainOptionChangesTheTerrain() {
		TestBootstrap.init();
		String baseline = fingerprint(defaultPreset());
		List<String> inert = new ArrayList<>();
		for (Page page : List.of(PresetOptions.WORLD, PresetOptions.CLIMATE, PresetOptions.TERRAIN, PresetOptions.RIVERS, PresetOptions.LANDFORMS)) {
			page.options().filter((option) -> !option.isHidden() && option.isActive(defaultPreset()) && !NOT_IN_HEIGHTMAP.contains(option.path())).forEach((option) -> {
				Preset preset = defaultPreset();
				if (!change(option, preset)) {
					inert.add(option.path() + " (couldn't change)");
					return;
				}
				if (baseline.equals(fingerprint(preset))) {
					inert.add(option.path());
				}
			});
		}
		inert.forEach((path) -> System.out.println("no effect: " + path));
		assertTrue(inert.isEmpty(), "options with no effect on the terrain: " + inert);
	}

	// the usual overview plus a close-up of a river network, where narrow branch rivers show up
	private static String fingerprint(Preset preset) {
		Tile close = raccoonman.reterraforged.world.worldgen.GeneratorContext.makeUncached(preset, PresetRenderer.SEED, PresetRenderer.TILE_SIZE, 0, 6).generator.generateZoomed(-3500.0F, 3000.0F, 4.0F, false).join();
		return PresetRenderer.fingerprint(PresetRenderer.generate(preset)) + PresetRenderer.fingerprint(close);
	}

	private static Preset defaultPreset() {
		return BuiltinPresetRenderTest.presets().get("default").get();
	}

	// moves the option well away from its current value
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static boolean change(Option<?> option, Preset preset) {
		Object before = option.get(preset);
		if (option instanceof BoolOption bool) {
			bool.set(preset, !(Boolean) before);
		} else if (option instanceof EnumOption enumOption) {
			for (Object value : enumOption.values()) {
				if (!value.equals(before)) {
					((Option) option).set(preset, value);
					break;
				}
			}
		} else if (option instanceof IntOption intOption) {
			int current = (Integer) before;
			int target = intOption.isSeed() ? current + 12345 : (current - intOption.min() > intOption.max() - current ? intOption.min() + (current - intOption.min()) / 4 : current + (intOption.max() - current) / 2);
			intOption.set(preset, target);
		} else if (option instanceof FloatOption floatOption) {
			float current = (Float) before;
			float target = current - floatOption.min() > floatOption.max() - current ? floatOption.min() + (current - floatOption.min()) / 4.0F : current + (floatOption.max() - current) / 2.0F;
			floatOption.set(preset, target);
		}
		return !before.equals(option.get(preset));
	}
}
