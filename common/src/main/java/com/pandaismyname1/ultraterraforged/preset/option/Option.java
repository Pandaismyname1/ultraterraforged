package com.pandaismyname1.ultraterraforged.preset.option;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Suppliers;

import net.minecraft.network.chat.Component;
import com.pandaismyname1.ultraterraforged.client.gui.Tooltips;
import com.pandaismyname1.ultraterraforged.data.preset.settings.BuiltinPresets;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

/**
 * A single user-facing preset setting, declared once and bound to a field of the typed {@link Preset}.
 * UIs, validation, reset-to-default and search all work from these declarations instead of hand-written pages.
 *
 * @param <T> the value type
 */
public abstract class Option<T> {
	private static final com.google.common.base.Supplier<Preset> DEFAULTS = Suppliers.memoize(BuiltinPresets::makeDefault);

	private final String path;
	private final String translationKey;
	private final Function<Preset, T> getter;
	private final BiConsumer<Preset, T> setter;
	private final List<Predicate<Preset>> conditions;
	private final Set<OptionTag> tags;
	private final boolean hidden;

	protected Option(Builder<T, ?> builder) {
		this.path = Objects.requireNonNull(builder.path, "path");
		this.translationKey = Objects.requireNonNull(builder.translationKey, "translationKey");
		this.getter = Objects.requireNonNull(builder.getter, "getter");
		this.setter = Objects.requireNonNull(builder.setter, "setter");
		this.conditions = List.copyOf(builder.conditions);
		this.tags = builder.tags.isEmpty() ? Set.of() : Set.copyOf(builder.tags);
		this.hidden = builder.hidden;
	}

	/**
	 * Where this value lives in the preset file, e.g. {@code world.continent.continentScale}.
	 * Stable across versions, so it doubles as the option's id.
	 */
	public String path() {
		return this.path;
	}

	public String translationKey() {
		return this.translationKey;
	}

	public String tooltipKey() {
		return Tooltips.translationKey(this.translationKey);
	}

	public Component displayName() {
		return Component.translatable(this.translationKey);
	}

	public Set<OptionTag> tags() {
		return this.tags;
	}

	/**
	 * Hidden options are still read and written with the preset but aren't shown, e.g. because the current
	 * generator ignores them.
	 */
	public boolean isHidden() {
		return this.hidden;
	}

	public T get(Preset preset) {
		return this.getter.apply(preset);
	}

	/**
	 * Validates the value against this option's constraints and stores it.
	 *
	 * @return the value that was actually stored
	 */
	public T set(Preset preset, T value) {
		T sanitized = this.sanitize(preset, value);
		this.setter.accept(preset, sanitized);
		return sanitized;
	}

	public T defaultValue() {
		return this.getter.apply(DEFAULTS.get());
	}

	public boolean isDefault(Preset preset) {
		return Objects.equals(this.get(preset), this.defaultValue());
	}

	public void reset(Preset preset) {
		this.set(preset, this.defaultValue());
	}

	/**
	 * Whether the value differs from the one in {@code baseline}, e.g. the preset the player started editing from.
	 */
	public boolean isModified(Preset preset, Preset baseline) {
		return !Objects.equals(this.get(preset), this.get(baseline));
	}

	public void resetTo(Preset preset, Preset baseline) {
		this.set(preset, this.get(baseline));
	}

	/**
	 * Whether the option has any effect given the rest of the preset; inactive options are shown disabled.
	 */
	public boolean isActive(Preset preset) {
		for (Predicate<Preset> condition : this.conditions) {
			if (!condition.test(preset)) {
				return false;
			}
		}
		return true;
	}

	protected T sanitize(Preset preset, T value) {
		return value;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + this.path + "]";
	}

	public abstract static class Builder<T, B extends Builder<T, B>> {
		private final String path;
		private String translationKey;
		private Function<Preset, T> getter;
		private BiConsumer<Preset, T> setter;
		private final List<Predicate<Preset>> conditions = new ArrayList<>();
		private final Set<OptionTag> tags = EnumSet.noneOf(OptionTag.class);
		private boolean hidden;

		protected Builder(String path) {
			this.path = path;
		}

		protected String path() {
			return this.path;
		}

		public B translation(String translationKey) {
			this.translationKey = translationKey;
			return this.self();
		}

		public B bind(Function<Preset, T> getter, BiConsumer<Preset, T> setter) {
			this.getter = getter;
			this.setter = setter;
			return this.self();
		}

		public B activeWhen(Predicate<Preset> condition) {
			this.conditions.add(condition);
			return this.self();
		}

		public B tag(OptionTag tag) {
			this.tags.add(tag);
			return this.self();
		}

		public B hidden() {
			this.hidden = true;
			return this.self();
		}

		@SuppressWarnings("unchecked")
		protected B self() {
			return (B) this;
		}

		public abstract Option<T> build();
	}
}
