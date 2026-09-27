package com.pandaismyname1.ultraterraforged.preset.option;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

/**
 * A numeric option with a fixed range, an optional step, and optional bounds that depend on other values in the
 * preset (e.g. the deep ocean control point can't go above the shallow ocean one).
 */
public abstract class NumberOption<T extends Number & Comparable<T>> extends Option<T> {
	private final T min;
	private final T max;
	private final List<Function<Preset, T>> lowerBounds;
	private final List<Function<Preset, T>> upperBounds;

	protected NumberOption(Builder<T, ?> builder) {
		super(builder);
		if (builder.min.compareTo(builder.max) > 0) {
			throw new IllegalArgumentException(builder.path() + ": min " + builder.min + " > max " + builder.max);
		}
		this.min = builder.min;
		this.max = builder.max;
		this.lowerBounds = List.copyOf(builder.lowerBounds);
		this.upperBounds = List.copyOf(builder.upperBounds);
	}

	public T min() {
		return this.min;
	}

	public T max() {
		return this.max;
	}

	@Override
	protected T sanitize(Preset preset, T value) {
		T result = clamp(value, this.min, this.max);
		for (Function<Preset, T> bound : this.lowerBounds) {
			result = max(result, bound.apply(preset));
		}
		for (Function<Preset, T> bound : this.upperBounds) {
			result = min(result, bound.apply(preset));
		}
		return this.snap(result);
	}

	protected T snap(T value) {
		return value;
	}

	private static <T extends Comparable<T>> T clamp(T value, T min, T max) {
		return max(min(value, max), min);
	}

	private static <T extends Comparable<T>> T min(T a, T b) {
		return a.compareTo(b) <= 0 ? a : b;
	}

	private static <T extends Comparable<T>> T max(T a, T b) {
		return a.compareTo(b) >= 0 ? a : b;
	}

	public abstract static class Builder<T extends Number & Comparable<T>, B extends Builder<T, B>> extends Option.Builder<T, B> {
		private T min;
		private T max;
		private final List<Function<Preset, T>> lowerBounds = new ArrayList<>();
		private final List<Function<Preset, T>> upperBounds = new ArrayList<>();

		protected Builder(String path) {
			super(path);
		}

		public B range(T min, T max) {
			this.min = min;
			this.max = max;
			return this.self();
		}

		// the value can't go below another value in the preset
		public B atLeast(Function<Preset, T> bound) {
			this.lowerBounds.add(bound);
			return this.self();
		}

		// the value can't go above another value in the preset
		public B atMost(Function<Preset, T> bound) {
			this.upperBounds.add(bound);
			return this.self();
		}
	}
}
