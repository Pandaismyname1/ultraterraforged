package com.pandaismyname1.ultraterraforged.fabric.mixin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
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
				modifiers.add(this.makeModifierRecord(holder.key().identifier(), ModificationPhase.POST_PROCESSING, (ctx) -> {
					return true;
				}, modifier::apply));
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
		boolean modified = registries.lookupOrThrow(UTFRegistries.BIOME_MODIFIER).stream().anyMatch((modifier) -> modifier instanceof FabricBiomeModifier);
		if (!modified) {
			return;
		}
		registries.lookupOrThrow(Registries.LEVEL_STEM).forEach((stem) -> {
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
	
	private Object makeModifierRecord(Identifier id, ModificationPhase phase, Predicate<BiomeSelectionContext> selector, BiConsumer<BiomeSelectionContext, BiomeModificationContext> modifier) {
		try {
			return CTOR.newInstance(phase, id, selector, modifier);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static {
		Constructor<?> ctor;
		try {
			ctor = Class.forName("net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl$ModifierRecord").getDeclaredConstructor(ModificationPhase.class, Identifier.class, Predicate.class, BiConsumer.class);
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			e.printStackTrace();
			ctor = null;
		}
		CTOR = ctor;
	}
}
