package com.pandaismyname1.ultraterraforged.mixin.byepregen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import com.pandaismyname1.ultraterraforged.compat.byepregen.ByePregenCompat;

// Bye Pregen's arena fills no more cells than UltraTerraForged's noise chunk has (issue #13, ByePregenCompat)
@Pseudo
@Mixin(targets = "com.moepus.byepregen.worldgen.arena.ArenaNoiseFiller", remap = false)
class MixinArenaNoiseFiller {

	@Inject(method = "fill", at = @At("HEAD"), cancellable = true, require = 0)
	private static void fill(NoiseChunk noiseChunk, @Coerce Object request, @Coerce Object targets, CallbackInfoReturnable<ChunkAccess> callback) {
		ChunkAccess chunk = ByePregenCompat.fillCulled(noiseChunk, request, targets);
		if (chunk != null) {
			callback.setReturnValue(chunk);
		}
	}
}
