package com.pandaismyname1.ultraterraforged.mixin.c2me;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.levelgen.RandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;

/**
 * C2ME OpenCL compiles a world's density functions for the graphics card when the world loads. UltraTerraForged's
 * terrain reads cells worked out in Java, which can't be, and unless openclAccel.allowIncompatibilityFallback is on
 * C2ME stops the game instead of generating the world the usual way. UltraTerraForged's worlds fall back; other worlds
 * keep the configured behaviour. C2ME reads its config before other mods' mixins apply, so this changes the read in
 * C2ME's own world loading handler, through MixinSquared, which C2ME bundles.
 */
@Mixin(value = ChunkMap.class, priority = 1500)
class MixinChunkMapOpenCL {
	@Shadow
	@Final
	private RandomState randomState;

	@TargetHandler(mixin = "com.ishland.c2me.opts.accel.opencl.mixin.MixinThreadedAnvilChunkStorage", name = "postInit")
	@ModifyExpressionValue(
		method = "@MixinSquared:Handler",
		at = @At(value = "FIELD", target = "Lcom/ishland/c2me/opts/accel/opencl/common/Config;allowIncompatibilityFallback:Z", remap = false),
		require = 0,
		remap = false
	)
	private boolean ultraterraforged$fallBack(boolean allowed) {
		return allowed || (Object) this.randomState instanceof UTFRandomState utfRandomState && utfRandomState.usesCells();
	}
}
