package com.pandaismyname1.ultraterraforged.preset.option;

import java.util.List;
import java.util.function.Function;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

public class EnumOption<E extends Enum<E>> extends Option<E> {
	private final List<E> values;
	private final Function<E, String> nameGetter;

	private EnumOption(Builder<E> builder) {
		super(builder);
		if (builder.values == null || builder.values.isEmpty()) {
			throw new IllegalArgumentException(builder.path() + ": no values");
		}
		this.values = List.copyOf(builder.values);
		this.nameGetter = builder.nameGetter;
	}

	// the values offered to the user, in display order
	public List<E> values() {
		return this.values;
	}

	public String name(E value) {
		return this.nameGetter.apply(value);
	}

	@Override
	protected E sanitize(Preset preset, E value) {
		return this.values.contains(value) ? value : this.values.get(0);
	}

	public static <E extends Enum<E>> Builder<E> builder(String path) {
		return new Builder<>(path);
	}

	public static class Builder<E extends Enum<E>> extends Option.Builder<E, Builder<E>> {
		private List<E> values;
		private Function<E, String> nameGetter = Enum::name;

		private Builder(String path) {
			super(path);
		}

		public Builder<E> values(List<E> values) {
			this.values = values;
			return this;
		}

		public Builder<E> names(Function<E, String> nameGetter) {
			this.nameGetter = nameGetter;
			return this;
		}

		@Override
		public EnumOption<E> build() {
			return new EnumOption<>(this);
		}
	}
}
