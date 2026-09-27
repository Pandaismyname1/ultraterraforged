package com.pandaismyname1.ultraterraforged.preset.option;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;

/**
 * A group of options within a page. The label is either a translation key or, for groups generated from registry
 * contents such as structure sets, ready-made text; a category may also have no label.
 */
public record Category(String id, @Nullable String labelKey, @Nullable Component labelText, List<Option<?>> options) {

	public Category {
		options = List.copyOf(options);
	}

	public Optional<Component> label() {
		if (this.labelText != null) {
			return Optional.of(this.labelText);
		}
		return Optional.ofNullable(this.labelKey).map(Component::translatable);
	}

	public static Category of(String id, @Nullable String labelKey, Option<?>... options) {
		return new Category(id, labelKey, null, List.of(options));
	}

	public static Category withText(String id, Component label, List<Option<?>> options) {
		return new Category(id, null, label, options);
	}
}
