package com.pandaismyname1.ultraterraforged.world.worldgen.surface;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public interface UTFSurfaceContext {
	@Nullable
	Set<ResourceKey<Biome>> getSurroundingBiomes();
}
