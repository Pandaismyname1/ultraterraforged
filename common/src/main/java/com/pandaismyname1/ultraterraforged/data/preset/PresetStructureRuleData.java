package com.pandaismyname1.ultraterraforged.data.preset;

import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.StructureTags;
import com.pandaismyname1.ultraterraforged.RTFCommon;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.registries.RTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRule;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRules;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

public class PresetStructureRuleData {
	public static final ResourceKey<StructureRule> CELL_TEST = createKey("cell_test");
	
	public static void bootstrap(Preset preset, BootstapContext<StructureRule> ctx) {
		ctx.register(CELL_TEST, StructureRules.cellTest(StructureTags.VILLAGE, 0.225F, TerrainType.MOUNTAIN_CHAIN, TerrainType.MOUNTAINS_1, TerrainType.MOUNTAINS_2, TerrainType.MOUNTAINS_3));
	}
	
	private static ResourceKey<StructureRule> createKey(String name) {
        return ResourceKey.create(RTFRegistries.STRUCTURE_RULE, RTFCommon.location(name));
	}
}
