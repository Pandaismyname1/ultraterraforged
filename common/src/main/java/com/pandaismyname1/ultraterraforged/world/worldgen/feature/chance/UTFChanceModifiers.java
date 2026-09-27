package com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance;

import com.mojang.serialization.Codec;

import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;

public class UTFChanceModifiers {

	public static void bootstrap() {
		register("elevation", ElevationChanceModifier.CODEC);
		register("biome_edge", BiomeEdgeChanceModifier.CODEC);
	}
	
	public static ElevationChanceModifier elevation(float from, float to) {
		return elevation(from, to, false);
	}
	
	public static ElevationChanceModifier elevation(float from, float to, boolean exclusive) {
		return new ElevationChanceModifier(from, to, exclusive);
	}
	
	public static BiomeEdgeChanceModifier biomeEdge(float from, float to) {
		return biomeEdge(from, to, false);
	}
	
	public static BiomeEdgeChanceModifier biomeEdge(float from, float to, boolean exclusive) {
		return new BiomeEdgeChanceModifier(from, to, exclusive);
	}
	
	private static void register(String name, Codec<? extends ChanceModifier> placement) {
		RegistryUtil.register(UTFBuiltInRegistries.CHANCE_MODIFIER_TYPE, name, placement);
	}
}
