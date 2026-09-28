package com.pandaismyname1.ultraterraforged.data;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;

// Since 1.20.5 a type dispatched on a "type" field needs a MapCodec. Before, any codec would do: a codec that isn't a map
// was written under a "value" key next to the type. These keep that exact format, so presets saved on older versions
// read the same.
public final class UTFCodecs {

	private UTFCodecs() {
	}

	public static <A> MapCodec<A> asMap(Codec<A> codec) {
		return codec instanceof MapCodec.MapCodecCodec<A> map ? map.codec() : codec.fieldOf("value");
	}

	// asMap for a registry's wildcard-typed entries
	public static <E> MapCodec<? extends E> entry(Codec<? extends E> codec) {
		return asMap(codec);
	}

	// a codec for values of a registry of codecs, dispatched on "type"
	public static <E> Codec<E> dispatch(Registry<Codec<? extends E>> registry, Function<? super E, ? extends Codec<? extends E>> type) {
		return registry.byNameCodec().dispatch(type, UTFCodecs::entry);
	}
}
