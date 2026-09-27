package com.pandaismyname1.ultraterraforged.platform.neoforge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;

// NeoForge only accepts registry writes during its registration events, which come after UTF's bootstrap, so writes
// wait here until then
public final class RegistryUtilImpl {
	private static final Map<ResourceKey<? extends Registry<?>>, List<Entry<?>>> ENTRIES = Collections.synchronizedMap(new LinkedHashMap<>());
	private static final List<Registry<?>> REGISTRIES = Collections.synchronizedList(new ArrayList<>());
	private static final List<DataRegistry<?>> DATA_REGISTRIES = Collections.synchronizedList(new ArrayList<>());

	public static void register(IEventBus bus) {
		bus.addListener((NewRegistryEvent event) -> REGISTRIES.forEach(event::register));
		bus.addListener((RegisterEvent event) -> {
			List<Entry<?>> entries = ENTRIES.get(event.getRegistryKey());
			if (entries != null) {
				entries.forEach((entry) -> entry.register(event));
			}
		});
		bus.addListener((DataPackRegistryEvent.NewRegistry event) -> DATA_REGISTRIES.forEach((registry) -> registry.register(event)));
	}

	public static <T> void register(Registry<T> registry, String name, T value) {
		ResourceKey<? extends Registry<T>> key = registry.key();
		ENTRIES.computeIfAbsent(key, (k) -> Collections.synchronizedList(new ArrayList<>())).add(new Entry<>(key, UTFRegistries.createKey(key, name).location(), value));
	}

	public static <T> Registry<T> createRegistry(ResourceKey<? extends Registry<T>> key) {
		Registry<T> registry = new RegistryBuilder<>(key).sync(false).create();
		REGISTRIES.add(registry);
		return registry;
	}

	public static <T> void createDataRegistry(ResourceKey<? extends Registry<T>> key, Codec<T> codec) {
		DATA_REGISTRIES.add(new DataRegistry<>(key, codec));
	}

	private record Entry<T>(ResourceKey<? extends Registry<T>> registry, ResourceLocation name, T value) {

		void register(RegisterEvent event) {
			event.register(this.registry, this.name, this::value);
		}
	}

	private record DataRegistry<T>(ResourceKey<? extends Registry<T>> key, Codec<T> codec) {

		@SuppressWarnings("unchecked")
		void register(DataPackRegistryEvent.NewRegistry event) {
			event.dataPackRegistry((ResourceKey<Registry<T>>) this.key, this.codec);
		}
	}
}
