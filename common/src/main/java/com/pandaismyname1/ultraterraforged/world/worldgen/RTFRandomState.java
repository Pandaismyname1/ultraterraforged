package com.pandaismyname1.ultraterraforged.world.worldgen;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public interface RTFRandomState {
	void initialize(RegistryAccess registryAccess);
	
	@Nullable
	RegistryAccess registryAccess();
	
	@Nullable
	Preset preset();

	@Nullable
	GeneratorContext generatorContext();
	
	DensityFunction wrap(DensityFunction function);

	Noise wrap(Noise noise);
}
