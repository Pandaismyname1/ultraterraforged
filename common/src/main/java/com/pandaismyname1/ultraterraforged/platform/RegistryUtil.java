package com.pandaismyname1.ultraterraforged.platform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier;

@Deprecated
public final class RegistryUtil {
	
	public static <T> void register(Registry<T> registry, String name, T value) {
		getWritable(registry).register(UTFRegistries.createKey(registry.key(), name), value, Lifecycle.stable());
	}
	
	@ExpectPlatform
	public static Registry<BiomeModifier> getBiomeModifierRegistry() {
		throw new IllegalStateException();
	}
	
	@ExpectPlatform
	public static <T> WritableRegistry<T> getWritable(Registry<T> registry) {
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
