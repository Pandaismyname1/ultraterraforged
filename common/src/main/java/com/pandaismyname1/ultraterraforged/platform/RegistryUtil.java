package com.pandaismyname1.ultraterraforged.platform;

import com.mojang.serialization.Codec;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

@Deprecated
public final class RegistryUtil {

	// adds a value to a static registry, vanilla's or UTF's; on NeoForge the value is only in the registry once the
	// loader's registration events have run
	@ExpectPlatform
	public static <T> void register(Registry<T> registry, String name, T value) {
		throw new IllegalStateException();
	}

	@ExpectPlatform
	public static <T> Registry<T> createRegistry(ResourceKey<? extends Registry<T>> key) {
		throw new IllegalStateException();
	}

	@ExpectPlatform
	public static <T> void createDataRegistry(ResourceKey<? extends Registry<T>> key, Codec<T> codec) {
		throw new IllegalStateException();
	}
}
