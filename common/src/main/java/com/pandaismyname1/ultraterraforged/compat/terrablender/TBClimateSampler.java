package com.pandaismyname1.ultraterraforged.compat.terrablender;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

// a climate sampler that also samples UltraTerraForged's biome regions, which pick TerraBlender's region
public interface TBClimateSampler {

	void setUniqueness(DensitySampler.Bound uniqueness);

	@Nullable
	DensitySampler.Bound getUniqueness();
}
