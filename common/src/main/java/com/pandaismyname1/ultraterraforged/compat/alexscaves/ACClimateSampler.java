package com.pandaismyname1.ultraterraforged.compat.alexscaves;

// a climate sampler that knows whether it samples a world UltraTerraForged makes (mixin.alexscaves.MixinClimateSampler)
public interface ACClimateSampler {

	boolean ultraterraforged$isTerrain();

	void ultraterraforged$setTerrain(boolean terrain);
}
