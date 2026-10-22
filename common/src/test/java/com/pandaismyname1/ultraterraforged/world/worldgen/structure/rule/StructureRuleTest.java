package com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;

/**
 * Structure rules have their say only over the structures they're for (issue #7: every structure of every mod was kept
 * away from rivers). A rule saved in a preset before it named its structures was meant for villages.
 */
public class StructureRuleTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void savedRulesWithoutTargetsAreForVillages() {
		JsonElement saved = JsonParser.parseString("{\"cutoff\": 0.225, \"terrain_type_blacklist\": []}");
		CellTest rule = CellTest.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
		assertEquals(StructureTags.VILLAGE, rule.targets());
	}

	@Test
	void targetsAreKept() {
		TagKey<net.minecraft.world.level.levelgen.structure.Structure> mineshafts = TagKey.create(Registries.STRUCTURE, ResourceLocation.parse("minecraft:mineshaft"));
		CellTest rule = StructureRules.cellTest(mineshafts, 0.1F);
		JsonElement written = CellTest.CODEC.encodeStart(JsonOps.INSTANCE, rule).getOrThrow();
		assertEquals("#minecraft:mineshaft", written.getAsJsonObject().get("targets").getAsString());
		CellTest read = CellTest.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow();
		assertEquals(rule, read);
	}
}
