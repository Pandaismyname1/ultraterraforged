package com.pandaismyname1.ultraterraforged.mixin.byepregen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.NoiseChunk;

@Mixin(NoiseChunk.class)
public interface AccessorNoiseChunk {
	// as tall as the terrain in UltraTerraForged's worlds (MixinNoiseChunk)
	@Accessor
	int getCellCountY();
}
