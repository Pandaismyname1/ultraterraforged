package com.pandaismyname1.ultraterraforged.registries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;

import net.minecraft.core.Cloner;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance.ChanceModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.decorator.TemplateDecorator;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.placement.TemplatePlacement;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domain;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.CurveFunction;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRule;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.LayeredSurfaceRule;

public class UTFRegistries {
	public static final ResourceKey<Registry<Codec<? extends Noise>>> NOISE_TYPE = createKey("worldgen/noise_type");
	public static final ResourceKey<Registry<Codec<? extends Domain>>> DOMAIN_TYPE = createKey("worldgen/domain_type");
	public static final ResourceKey<Registry<Codec<? extends CurveFunction>>> CURVE_FUNCTION_TYPE = createKey("worldgen/curve_function_type");
	public static final ResourceKey<Registry<Codec<? extends ChanceModifier>>> CHANCE_MODIFIER_TYPE = createKey("worldgen/chance_modifier_type");
	public static final ResourceKey<Registry<Codec<? extends TemplatePlacement<?>>>> TEMPLATE_PLACEMENT_TYPE = createKey("worldgen/template_placement_type");
	public static final ResourceKey<Registry<Codec<? extends TemplateDecorator<?>>>> TEMPLATE_DECORATOR_TYPE = createKey("worldgen/template_decorator_type");
	public static final ResourceKey<Registry<Codec<? extends BiomeModifier>>> BIOME_MODIFIER_TYPE = createKey("worldgen/biome_modifier_type");
	public static final ResourceKey<Registry<Codec<? extends StructureRule>>> STRUCTURE_RULE_TYPE = createKey("worldgen/structure_rule_type");
	public static final ResourceKey<Registry<Noise>> NOISE = createKey("worldgen/noise");
	public static final ResourceKey<Registry<BiomeModifier>> BIOME_MODIFIER = createKey("worldgen/biome_modifier");
	public static final ResourceKey<Registry<StructureRule>> STRUCTURE_RULE = createKey("worldgen/structure_rule");
	public static final ResourceKey<Registry<LayeredSurfaceRule.Layer>> SURFACE_LAYERS = createKey("worldgen/surface_layers");

	public static final ResourceKey<Registry<Preset>> PRESET = createKey("worldgen/preset");

	private static final List<Consumer<Cloner.Factory>> DATA_REGISTRY_CODECS = new ArrayList<>();
	private static final List<ResourceKey<? extends Registry<?>>> DATA_REGISTRIES = new ArrayList<>();

	// a datapack registry, made by the loader; its codec is also kept for cloner()
	public static <T> void createDataRegistry(ResourceKey<? extends Registry<T>> key, Codec<T> codec) {
		DATA_REGISTRY_CODECS.add((factory) -> factory.addCodec(key, codec));
		DATA_REGISTRIES.add(key);
		RegistryUtil.createDataRegistry(key, codec);
	}

	// copies entries between registry lookups, as building a registry patch does: knows the codecs of vanilla's
	// datapack registries and of UTF's
	public static Cloner.Factory cloner() {
		Cloner.Factory factory = new Cloner.Factory();
		RegistryDataLoader.WORLD_REGISTRIES.forEach((data) -> data.runWithArguments(factory::addCodec));
		RegistryDataLoader.DIMENSION_REGISTRIES.forEach((data) -> data.runWithArguments(factory::addCodec));
		DATA_REGISTRY_CODECS.forEach((codec) -> codec.accept(factory));
		return factory;
	}

	// the registries cloner() can copy
	private static Set<ResourceKey<? extends Registry<?>>> clonable() {
		Set<ResourceKey<? extends Registry<?>>> keys = new HashSet<>(DATA_REGISTRIES);
		RegistryDataLoader.WORLD_REGISTRIES.forEach((data) -> keys.add(data.key()));
		RegistryDataLoader.DIMENSION_REGISTRIES.forEach((data) -> keys.add(data.key()));
		return keys;
	}

	// The lookup a preset's registry patch starts from: the given one without the registries cloner() can't copy (the
	// loaders' and other mods', such as neoforge:structure_modifier, which a preset never patches, but which building a
	// patch would otherwise try to copy), plus an empty registry for each of UTF's datapack registries it lacks, as
	// building a patch looks up every registry it patches in the lookup it starts from.
	public static HolderLookup.Provider patchBase(HolderLookup.Provider registries) {
		Set<ResourceKey<? extends Registry<?>>> clonable = clonable();
		List<ResourceKey<? extends Registry<?>>> missing = DATA_REGISTRIES.stream().filter((key) -> registries.lookup(key).isEmpty()).toList();
		List<HolderLookup.RegistryLookup<?>> empty = missing.stream().<HolderLookup.RegistryLookup<?>>map((key) -> emptyLookup(key)).toList();
		return new HolderLookup.Provider() {

			@Override
			public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
				return Stream.concat(registries.listRegistryKeys().filter(clonable::contains), missing.stream());
			}

			@SuppressWarnings("unchecked")
			@Override
			public <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
				Optional<? extends HolderLookup.RegistryLookup<T>> lookup = registries.lookup(key);
				if (lookup.isPresent()) {
					return lookup;
				}
				return empty.stream().filter((registry) -> registry.key().equals(key)).findFirst().map((registry) -> (HolderLookup.RegistryLookup<T>) registry);
			}
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static HolderLookup.RegistryLookup<?> emptyLookup(ResourceKey<? extends Registry<?>> key) {
		return new MappedRegistry(key, Lifecycle.stable());
	}
	
	public static <T> ResourceKey<T> createKey(ResourceKey<? extends Registry<T>> registryKey, String valueKey) {
		return ResourceKey.create(registryKey, UTFCommon.location(valueKey));
	}

	private static <T> ResourceKey<Registry<T>> createKey(String key) {
		return ResourceKey.createRegistryKey(UTFCommon.location(key));
	}
}
