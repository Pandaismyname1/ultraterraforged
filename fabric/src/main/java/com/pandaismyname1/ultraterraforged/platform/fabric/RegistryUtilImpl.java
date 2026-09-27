package com.pandaismyname1.ultraterraforged.platform.fabric;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;

public class RegistryUtilImpl {

	public static <T> void register(Registry<T> registry, String name, T value) {
		Registry.register(registry, UTFRegistries.createKey(registry.key(), name), value);
	}

	@SuppressWarnings("unchecked")
	public static <T> Registry<T> createRegistry(ResourceKey<? extends Registry<T>> key) {
		return FabricRegistryBuilder.createSimple((ResourceKey<Registry<T>>) key).buildAndRegister();
	}

	public static <T> void createDataRegistry(ResourceKey<? extends Registry<T>> key, Codec<T> codec) {
		DynamicRegistries.register(key, codec);
	}
}
