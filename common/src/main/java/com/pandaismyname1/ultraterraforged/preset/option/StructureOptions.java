package com.pandaismyname1.ultraterraforged.preset.option;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.StructureSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.StructureSettings.StructureSetEntry;

/**
 * Options for the structure sets that exist in a given world, including modded ones. Unlike {@link PresetOptions}
 * these can't be declared up front: they are built from the registry when an editor opens.
 *
 * Each option reads the preset's override and falls back to the registered value; setting the registered value
 * removes the override again.
 */
public final class StructureOptions {

	public static Page page(HolderLookup.RegistryLookup<StructureSet> structureSets, Predicate<Holder.Reference<StructureSet>> filter) {
		List<Category> categories = new ArrayList<>();
		structureSets.listElements()
			.filter(filter)
			.sorted(Comparator.comparing((Holder.Reference<StructureSet> holder) -> holder.key().identifier()))
			.forEach((holder) -> categories.add(category(holder.key().identifier(), holder.value())));
		return new Page("structures", UTFTranslationKeys.GUI_STRUCTURE_SETTINGS_TITLE, categories);
	}

	public static Category category(Identifier id, StructureSet set) {
		String path = "structures." + id + ".";
		StructurePlacement placement = set.placement();
		List<Option<?>> options = new ArrayList<>();

		BoolOption enabled = BoolOption.builder(path + "enabled")
			.translation(UTFTranslationKeys.GUI_BUTTON_STRUCTURE_ENABLED)
			.bind((p) -> entry(p, id, (e) -> e.enabled, true), (p, v) -> override(p, id, (e, value) -> e.enabled = value, v, true))
			.build();
		options.add(enabled);
		Predicate<Preset> isEnabled = enabled::get;

		if (placement instanceof RandomSpreadStructurePlacement randomSpread) {
			int spacing = randomSpread.spacing();
			int separation = randomSpread.separation();
			options.add(IntOption.builder(path + "spacing")
				.translation(UTFTranslationKeys.GUI_SLIDER_SPACING)
				.range(1, Math.max(512, spacing))
				.atLeast((p) -> entry(p, id, (e) -> e.separation, separation) + 1)
				.activeWhen(isEnabled)
				.bind((p) -> entry(p, id, (e) -> e.spacing, spacing), (p, v) -> override(p, id, (e, value) -> e.spacing = value, v, spacing))
				.build());
			options.add(IntOption.builder(path + "separation")
				.translation(UTFTranslationKeys.GUI_SLIDER_SEPARATION)
				.range(0, Math.max(511, separation))
				.atMost((p) -> entry(p, id, (e) -> e.spacing, spacing) - 1)
				.activeWhen(isEnabled)
				.bind((p) -> entry(p, id, (e) -> e.separation, separation), (p, v) -> override(p, id, (e, value) -> e.separation = value, v, separation))
				.build());
		} else if (placement instanceof ConcentricRingsStructurePlacement rings) {
			options.add(ringOption(id, path + "distance", UTFTranslationKeys.GUI_SLIDER_STRUCTURE_DISTANCE, 0, 1023, rings.distance(), isEnabled, (e) -> e.distance, (e, v) -> e.distance = v));
			options.add(ringOption(id, path + "spread", UTFTranslationKeys.GUI_SLIDER_STRUCTURE_SPREAD, 0, 1023, rings.spread(), isEnabled, (e) -> e.spread, (e, v) -> e.spread = v));
			options.add(ringOption(id, path + "count", UTFTranslationKeys.GUI_SLIDER_STRUCTURE_COUNT, 1, Math.max(512, rings.count()), rings.count(), isEnabled, (e) -> e.count, (e, v) -> e.count = v));
		}

		// other placement types from mods can only be switched on and off
		// (since 26.3 the frequency and salt live on AbstractSpreadingStructurePlacement, which both extend)
		if ((placement instanceof RandomSpreadStructurePlacement || placement instanceof ConcentricRingsStructurePlacement) && placement instanceof AbstractSpreadingStructurePlacement spreading) {
			float frequency = spreading.frequency();
			int salt = spreading.salt();
			options.add(FloatOption.builder(path + "frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_STRUCTURE_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(isEnabled)
				.bind((p) -> entry(p, id, (e) -> e.frequency, frequency), (p, v) -> override(p, id, (e, value) -> e.frequency = value, v, frequency))
				.build());
			options.add(IntOption.builder(path + "salt")
				.translation(UTFTranslationKeys.GUI_BUTTON_SALT)
				.seed()
				.activeWhen(isEnabled)
				.bind((p) -> entry(p, id, (e) -> e.salt, salt), (p, v) -> override(p, id, (e, value) -> e.salt = value, v, salt))
				.build());
		}
		return Category.withText(id.toString(), displayName(id), options);
	}

	// "minecraft:woodland_mansions" becomes "Woodland Mansions", modded sets keep their namespace
	public static Component displayName(Identifier id) {
		StringBuilder name = new StringBuilder();
		for (String word : id.getPath().replace('/', ' ').replace('_', ' ').split(" ")) {
			if (word.isEmpty()) {
				continue;
			}
			if (name.length() > 0) {
				name.append(' ');
			}
			name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		MutableComponent component = Component.literal(name.toString());
		if (!id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
			component.append(Component.literal(" (" + id.getNamespace() + ")").withStyle(ChatFormatting.GRAY));
		}
		return component;
	}

	private static IntOption ringOption(Identifier id, String path, String translationKey, int min, int max, int registered, Predicate<Preset> isEnabled, Function<StructureSetEntry, Integer> getter, BiConsumer<StructureSetEntry, Integer> setter) {
		return IntOption.builder(path)
			.translation(translationKey)
			.range(min, Math.max(max, registered))
			.activeWhen(isEnabled)
			.bind((p) -> entry(p, id, getter, registered), (p, v) -> override(p, id, setter, v, registered))
			.build();
	}

	private static <T> T entry(Preset preset, Identifier id, Function<StructureSetEntry, T> getter, T registered) {
		return preset.structures().get(id).map(getter).orElse(registered);
	}

	private static <T> void override(Preset preset, Identifier id, BiConsumer<StructureSetEntry, T> setter, T value, T registered) {
		StructureSettings structures = preset.structures();
		setter.accept(structures.getOrCreate(id), Objects.equals(value, registered) ? null : value);
		structures.clean(id);
	}

	private StructureOptions() {
	}
}
