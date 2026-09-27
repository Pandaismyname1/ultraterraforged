package com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;

public class StructureRules {

	public static void bootstrap() {
		register("cell_test", CellTest.CODEC);
	}
	
	public static CellTest cellTest(TagKey<Structure> targets, float cutoff, Terrain... terrainTypeBlacklist) {
		return new CellTest(cutoff, ImmutableSet.copyOf(terrainTypeBlacklist));
	}

	private static void register(String name, Codec<? extends StructureRule> value) {
		RegistryUtil.register(UTFBuiltInRegistries.STRUCTURE_RULE_TYPE, name, value);
	}
}
