package com.pandaismyname1.ultraterraforged.data.preset.settings;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

final class PresetCodecs {

	/**
	 * A field that falls back to a default when missing but, unlike {@link Codec#optionalFieldOf(String, Object)},
	 * is always written. Preset files then state every value explicitly, so changing a default later can't change
	 * what an existing preset or world generates.
	 */
	public static <T> MapCodec<T> defaulted(Codec<T> codec, String name, T defaultValue) {
		return codec.optionalFieldOf(name).xmap((value) -> value.orElse(defaultValue), Optional::of);
	}

	private PresetCodecs() {
	}
}
