package com.pandaismyname1.ultraterraforged.mixin.streams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pandaismyname1.ultraterraforged.compat.streams.StreamsCompat;

/**
 * Marks the time Streams Reflowing spends carving one of its own rivers, for MixinWorldGenRegion. The method overrides
 * vanilla's Feature.place, so it's named as the game is mapped: by name in the dev game and on Mojang's mappings,
 * method_13151 on Fabric's, m_142674_ on Forge's.
 */
@Pseudo
@Mixin(targets = "dev.streamsreflowing.worldgen.RiverCarverFeature", remap = false)
abstract class MixinRiverCarverFeature {

	@Inject(method = { "place", "method_13151", "m_142674_" }, at = @At("HEAD"), require = 0)
	private void place$HEAD(@Coerce Object context, CallbackInfoReturnable<Boolean> callback) {
		StreamsCompat.setCarving(true);
	}

	@Inject(method = { "place", "method_13151", "m_142674_" }, at = @At("RETURN"), require = 0)
	private void place$RETURN(@Coerce Object context, CallbackInfoReturnable<Boolean> callback) {
		StreamsCompat.setCarving(false);
	}
}
