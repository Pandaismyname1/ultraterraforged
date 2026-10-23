package com.pandaismyname1.ultraterraforged.mixin.generatoraccelerator;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import com.pandaismyname1.ultraterraforged.compat.generatoraccelerator.GeneratorAcceleratorCodecs;

// vanilla's density function types keep their names when Generator Accelerator registered its own under them, so
// functions made in code can still be written (GeneratorAcceleratorCodecs)
@Mixin(MappedRegistry.class)
abstract class MixinMappedRegistry<T> {

	@Shadow
	public abstract ResourceKey<? extends Registry<T>> key();

	@SuppressWarnings("unchecked")
	@Inject(method = "getResourceKey", at = @At("RETURN"), cancellable = true)
	private void getResourceKey(T value, CallbackInfoReturnable<Optional<ResourceKey<T>>> callback) {
		if (callback.getReturnValue().isEmpty() && (Object) this.key() == Registries.DENSITY_FUNCTION_TYPE) {
			ResourceKey<?> key = GeneratorAcceleratorCodecs.vanillaTypeKey(value);
			if (key != null) {
				callback.setReturnValue(Optional.of((ResourceKey<T>) key));
			}
		}
	}
}
