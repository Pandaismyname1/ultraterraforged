package com.pandaismyname1.ultraterraforged.world.worldgen;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public interface UTFRandomState {
	void initialize(RegistryAccess registryAccess);

	@Nullable
	RegistryAccess registryAccess();

	@Nullable
	Preset preset();

	@Nullable
	GeneratorContext generatorContext();

	// whether the world's terrain reads UltraTerraForged's cells: known from the start, before the preset is loaded
	boolean usesCells();

	Noise wrap(Noise noise);
}
