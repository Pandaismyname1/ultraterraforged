package com.pandaismyname1.ultraterraforged.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;

import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.server.commands.UTFCommands;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifiers;

/**
 * Boots vanilla and runs UTF's common bootstrap without a mod loader.
 *
 * The @ExpectPlatform hooks have no implementation in common, so they are stubbed with what the Fabric
 * implementations do. Vanilla freezes its registries during bootstrap, before a loader would normally let mods
 * register, so they are briefly unfrozen while UTF registers its types.
 */
public final class TestBootstrap {
	// data registries UTF adds, in registration order, as the loaders would be told about them
	public static final Map<ResourceKey<? extends Registry<?>>, Codec<?>> DATA_REGISTRIES = Collections.synchronizedMap(new LinkedHashMap<>());

	private static boolean initialized;

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();

		List<MappedRegistry<?>> unfrozen = unfreezeBuiltInRegistries();
		try (
			MockedStatic<RegistryUtil> registryUtil = Mockito.mockStatic(RegistryUtil.class);
			MockedStatic<ModLoaderUtil> modLoaderUtil = Mockito.mockStatic(ModLoaderUtil.class);
			MockedStatic<UTFCommands> commands = Mockito.mockStatic(UTFCommands.class);
			MockedStatic<BiomeModifiers> biomeModifiers = Mockito.mockStatic(BiomeModifiers.class)
		) {
			registryUtil.when(() -> RegistryUtil.createRegistry(any())).thenAnswer((invocation) -> newRegistry(invocation.getArgument(0)));
			registryUtil.when(() -> RegistryUtil.getWritable(any())).thenAnswer((invocation) -> invocation.getArgument(0));
			registryUtil.when(() -> RegistryUtil.register(any(), anyString(), any())).thenAnswer((invocation) -> {
				register(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
				return null;
			});
			registryUtil.when(() -> RegistryUtil.createDataRegistry(any(), any())).thenAnswer((invocation) -> {
				DATA_REGISTRIES.put(invocation.getArgument(0), invocation.getArgument(1));
				return null;
			});
			modLoaderUtil.when(() -> ModLoaderUtil.isLoaded(anyString())).thenReturn(false);

			UTFCommon.bootstrap();
		} finally {
			unfrozen.forEach(MappedRegistry::freeze);
		}
		initialized = true;
	}

	@SuppressWarnings("unchecked")
	private static <T> void register(Registry<T> registry, String name, Object value) {
		((WritableRegistry<T>) registry).register(UTFRegistries.createKey(registry.key(), name), (T) value, Lifecycle.stable());
	}

	private static List<MappedRegistry<?>> unfreezeBuiltInRegistries() {
		List<MappedRegistry<?>> unfrozen = new ArrayList<>();
		try {
			Field frozen = MappedRegistry.class.getDeclaredField("frozen");
			frozen.setAccessible(true);
			for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
				if (registry instanceof MappedRegistry<?> mapped && frozen.getBoolean(mapped)) {
					frozen.setBoolean(mapped, false);
					unfrozen.add(mapped);
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Couldn't unfreeze built-in registries", e);
		}
		return unfrozen;
	}

	@SuppressWarnings("unchecked")
	private static <T> MappedRegistry<T> newRegistry(ResourceKey<?> key) {
		return new MappedRegistry<>((ResourceKey<? extends Registry<T>>) key, Lifecycle.stable());
	}
}
