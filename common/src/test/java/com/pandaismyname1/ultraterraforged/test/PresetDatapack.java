package com.pandaismyname1.ultraterraforged.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifiers;

/**
 * Builds the registry contents of a preset's datapack against vanilla's built-in worldgen and serializes it the
 * way the Fabric registry provider does, keyed by the file path it would have in the pack.
 *
 * Biome modifiers are skipped: their implementations are loader specific and have no codec in common.
 */
public final class PresetDatapack {

	public record Result(Map<String, JsonElement> files, List<String> errors) {
	}

	public static Result generate(Preset preset) {
		TestBootstrap.init();
		HolderLookup.Provider patch;
		try (MockedStatic<BiomeModifiers> biomeModifiers = Mockito.mockStatic(BiomeModifiers.class, (invocation) -> {
			return invocation.getMethod().getReturnType() == BiomeModifier.class ? Mockito.mock(BiomeModifier.class) : null;
		})) {
			patch = preset.buildPatch(VanillaRegistries.createLookup());
		}

		RegistryOps<JsonElement> ops = patch.createSerializationContext(JsonOps.INSTANCE);
		Map<String, JsonElement> files = new TreeMap<>();
		List<String> errors = new ArrayList<>();
		for (RegistryDataLoader.RegistryData<?> data : RegistryDataLoader.WORLDGEN_REGISTRIES) {
			dump(patch, ops, data.key(), data.elementCodec(), files, errors);
		}
		TestBootstrap.DATA_REGISTRIES.forEach((key, codec) -> {
			if (!key.equals(UTFRegistries.BIOME_MODIFIER)) {
				dump(patch, ops, key, codec, files, errors);
			}
		});
		return new Result(files, errors);
	}

	@SuppressWarnings("unchecked")
	private static <T> void dump(HolderLookup.Provider provider, RegistryOps<JsonElement> ops, ResourceKey<? extends Registry<?>> registryKey, Codec<?> codec, Map<String, JsonElement> files, List<String> errors) {
		provider.lookup((ResourceKey<? extends Registry<T>>) registryKey).ifPresent((lookup) -> {
			lookup.listElements().forEach((Holder.Reference<T> holder) -> {
				String path = path(registryKey.identifier(), holder.key().identifier());
				DataResult<JsonElement> result = ((Codec<T>) codec).encodeStart(ops, holder.value());
				result.error().ifPresent((error) -> errors.add(path + ": " + error.message()));
				result.result().ifPresent((json) -> files.put(path, json));
			});
		});
	}

	// data/<namespace>/<registry>/<path>.json, with non-vanilla registries prefixed by their namespace
	public static String path(Identifier registry, Identifier element) {
		String registryPath = registry.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? registry.getPath() : registry.getNamespace() + "/" + registry.getPath();
		return "data/" + element.getNamespace() + "/" + registryPath + "/" + element.getPath() + ".json";
	}

	private PresetDatapack() {
	}
}
