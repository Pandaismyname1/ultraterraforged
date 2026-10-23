package com.pandaismyname1.ultraterraforged.mixin.alexscaves;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.compat.alexscaves.ACClimateSampler;

// marked by MixinRandomState when it makes a sampler for a world UltraTerraForged makes
@Mixin(Climate.Sampler.class)
class MixinClimateSampler implements ACClimateSampler {
	@Unique
	private boolean terrain;

	@Override
	public boolean ultraterraforged$isTerrain() {
		return this.terrain;
	}

	@Override
	public void ultraterraforged$setTerrain(boolean terrain) {
		this.terrain = terrain;
	}
}
