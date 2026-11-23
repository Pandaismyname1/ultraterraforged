package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.StrataRule;

/**
 * Ores are left out of rock layers (#18): mods tag their ores as stone so ore features can replace them, and a mod's
 * ore in the layers filled the world with it.
 */
public class StrataOreTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void oresAreNotRocks() {
		for (Block block : new Block[] { Blocks.IRON_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.COPPER_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE, Blocks.RAW_IRON_BLOCK }) {
			boolean ore = StrataRule.isOre(block, block.defaultBlockState());
			// raw ore blocks are storage blocks, not ores
			if (block == Blocks.RAW_IRON_BLOCK) {
				assertFalse(ore, "raw_iron_block isn't an ore");
			} else {
				assertTrue(ore, BuiltInRegistries.BLOCK.getKey(block) + " should count as an ore");
			}
		}
	}

	@Test
	void rocksAreNotOres() {
		for (Block block : new Block[] { Blocks.STONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.TUFF, Blocks.DEEPSLATE, Blocks.CALCITE, Blocks.SMOOTH_BASALT, Blocks.DRIPSTONE_BLOCK }) {
			assertFalse(StrataRule.isOre(block, block.defaultBlockState()), BuiltInRegistries.BLOCK.getKey(block) + " is a rock");
		}
	}
}
