package com.pandaismyname1.ultraterraforged.fabric.mixin;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.base.Suppliers;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.fabric.FabricBiomeModifier;

@Mixin(BiomeModificationImpl.class)
public class MixinBiomeModificationImpl {
	@Shadow(remap = false)
	@Final
	private static Comparator<Object> MODIFIER_ORDER_COMPARATOR;
	
	private static final Constructor<?> CTOR;
	
	@Redirect(
		method = "finalizeWorldGen",
		at = @At(
			value = "INVOKE",
			target = "getSortedModifiers"
		),
		remap = false
	)
	public List<Object> getSortedModifiers(BiomeModificationImpl self, RegistryAccess registries) {
		List<Object> modifiers = new ArrayList<>(this.getSortedModifiers());
		for(Holder.Reference<BiomeModifier> holder : registries.lookupOrThrow(UTFRegistries.BIOME_MODIFIER).listElements().toList()) {
			if(holder.value() instanceof FabricBiomeModifier modifier) {
				ResourceLocation id = holder.key().location();
				Object record = this.makeModifierRecord(id, ModificationPhase.POST_PROCESSING, (ctx) -> {
					return true;
				}, (selection, modification) -> {
					// one modifier failing on one biome shouldn't stop the world from loading
					try {
						modifier.apply(selection, modification);
					} catch (RuntimeException e) {
						UTFCommon.LOGGER.error("Biome modifier {} failed on biome {}", id, selection.getBiomeKey().location(), e);
					}
				});
				if (record != null) {
					modifiers.add(record);
				}
			}
		}
		modifiers.sort(MODIFIER_ORDER_COMPARATOR);
		return modifiers;
    }

	/**
	 * Vanilla works out the order of the biomes' features once, and in singleplayer the client already did, before the
	 * server got here. Fabric works it out again only when a modifier changed features through its own context, which
	 * ours don't: they change the biome. Without this the order lacks UTF's trees and features, and decorating a chunk
	 * that has one fails with "Index -1 out of bounds".
	 */
	@Inject(method = "finalizeWorldGen", at = @At("TAIL"), remap = false)
	private void finalizeWorldGen$TAIL(RegistryAccess registries, CallbackInfo callback) {
		boolean modified = registries.registryOrThrow(UTFRegistries.BIOME_MODIFIER).stream().anyMatch((modifier) -> modifier instanceof FabricBiomeModifier);
		if (!modified) {
			return;
		}
		registries.registryOrThrow(Registries.LEVEL_STEM).forEach((stem) -> {
			ChunkGenerator generator = stem.generator();
			ChunkGeneratorAccessor accessor = (ChunkGeneratorAccessor) generator;
			var settings = accessor.getGenerationSettingsGetter();
			accessor.setFeaturesPerStep(Suppliers.memoize(() -> {
				return FeatureSorter.buildFeaturesPerStep(List.copyOf(generator.getBiomeSource().possibleBiomes()), (biome) -> settings.apply(biome).features(), true);
			}));
		});
	}
	
	@Shadow(remap = false)
	private List<Object> getSortedModifiers() {
		throw new UnsupportedOperationException();
	}
	
	// Fabric API's record of a modifier, made through reflection as it's internal: null, with the reason logged, if
	// this Fabric API's record doesn't take what it used to
	private Object makeModifierRecord(ResourceLocation id, ModificationPhase phase, Predicate<BiomeSelectionContext> selector, BiConsumer<BiomeSelectionContext, BiomeModificationContext> modifier) {
		if (CTOR == null) {
			UTFCommon.LOGGER.error("Can't add biome modifier {}: this Fabric API's biome modification records aren't made as expected", id);
			return null;
		}
		Class<?>[] types = CTOR.getParameterTypes();
		Object[] args = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			for (Object arg : new Object[] { phase, id, selector, modifier }) {
				if (types[i].isInstance(arg)) {
					args[i] = arg;
				}
			}
		}
		try {
			return CTOR.newInstance(args);
		} catch (ReflectiveOperationException | RuntimeException e) {
			UTFCommon.LOGGER.error("Can't add biome modifier {}", id, e);
			return null;
		}
	}
	
	static {
		Constructor<?> ctor = null;
		try {
			// the record's fields have been reordered before: take the constructor that takes these four, in any order
			for (Constructor<?> candidate : Class.forName("net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl$ModifierRecord").getDeclaredConstructors()) {
				List<Class<?>> types = List.of(candidate.getParameterTypes());
				if (types.size() == 4 && types.containsAll(List.of(ModificationPhase.class, ResourceLocation.class, Predicate.class, BiConsumer.class))) {
					candidate.setAccessible(true);
					ctor = candidate;
				}
			}
		} catch (ClassNotFoundException | SecurityException e) {
			UTFCommon.LOGGER.error("Fabric API's biome modification records weren't found", e);
		}
		CTOR = ctor;
	}
}
