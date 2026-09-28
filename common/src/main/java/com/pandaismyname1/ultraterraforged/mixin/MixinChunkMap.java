package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.RandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;

@Mixin(ChunkMap.class)
public class MixinChunkMap {
	@Shadow
	@Final
	private ServerLevel level;
	@Shadow
    private RandomState randomState;

	// no arguments captured, so the constructor's signature can change between versions
	@Inject(
		at = @At("TAIL"),
		method = "<init>"
	)
	private void ultraterraforged$init(CallbackInfo callback) {
		if((Object) this.randomState instanceof UTFRandomState utfRandomState) {
			WorldGenFlags.setCullNoiseSections(true);

			utfRandomState.initialize(this.level.registryAccess());
		}
	}
}
