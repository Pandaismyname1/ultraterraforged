package com.pandaismyname1.ultraterraforged.mixin.streams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import com.pandaismyname1.ultraterraforged.compat.streams.StreamsCompat;

// Streams Reflowing's rivers don't carve away UltraTerraForged's water or the ground under it (StreamsCompat.keeps)
@Mixin(WorldGenRegion.class)
abstract class MixinWorldGenRegion {

	@Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
	private void setBlock(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> callback) {
		if (StreamsCompat.carving() && StreamsCompat.keeps((WorldGenRegion) (Object) this, pos, state)) {
			callback.setReturnValue(false);
		}
	}
}
