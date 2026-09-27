package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.StrataStack;

public class StrataStackTest {
	private static List<BlockState> rocks;

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
		rocks = List.of(Blocks.STONE.defaultBlockState(), Blocks.GRANITE.defaultBlockState(), Blocks.DIORITE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState());
	}

	@Test
	void layersCoverTheHeightWithVisibleBoundaries() {
		StrataStack stack = StrataStack.generate(RandomSource.create(42L), Blocks.STONE.defaultBlockState(), 0.25F, rocks, 600, 3, 14);
		int previousTop = 0;
		for (int i = 0; i < stack.layerCount(); i++) {
			int thickness = stack.layerTop(i) - previousTop;
			assertTrue(thickness >= 3, "layer " + i + " is " + thickness + " thick");
			if (i > 0) {
				// the same rock twice in a row would be one layer without a boundary
				assertNotEquals(stack.layerState(i - 1), stack.layerState(i), "layer " + i);
			}
			previousTop = stack.layerTop(i);
		}
		assertTrue(previousTop >= 600);
	}

	@Test
	void sameSeedSameLayers() {
		StrataStack a = StrataStack.generate(RandomSource.create(7L), Blocks.STONE.defaultBlockState(), 0.25F, rocks, 300, 3, 14);
		StrataStack b = StrataStack.generate(RandomSource.create(7L), Blocks.STONE.defaultBlockState(), 0.25F, rocks, 300, 3, 14);
		assertEquals(a.layerCount(), b.layerCount());
		for (int i = 0; i < a.layerCount(); i++) {
			assertEquals(a.layerTop(i), b.layerTop(i));
			assertEquals(a.layerState(i), b.layerState(i));
		}
	}

	@Test
	void lookupFindsTheLayerAtEachHeight() {
		StrataStack stack = StrataStack.generate(RandomSource.create(3L), Blocks.STONE.defaultBlockState(), 0.25F, rocks, 200, 3, 14);
		for (int i = 0; i < stack.layerCount(); i++) {
			int bottom = i == 0 ? 0 : stack.layerTop(i - 1);
			assertEquals(stack.layerState(i), stack.at(bottom), "bottom of layer " + i);
			assertEquals(stack.layerState(i), stack.at(stack.layerTop(i) - 0.5F), "top of layer " + i);
		}
		// past the ends the outermost layers continue
		assertEquals(stack.layerState(0), stack.at(-50.0F));
		assertEquals(stack.layerState(stack.layerCount() - 1), stack.at(100000.0F));
	}

	@Test
	void baseRockKeepsItsShare() {
		// however many rocks there are, the base rock fills about the share it's given
		for (int extra : new int[] { 0, 10 }) {
			List<BlockState> materials = new java.util.ArrayList<>(rocks);
			for (int i = 0; i < extra; i++) {
				materials.add(List.of(Blocks.TUFF, Blocks.CALCITE, Blocks.SMOOTH_BASALT, Blocks.DRIPSTONE_BLOCK, Blocks.SANDSTONE, Blocks.TERRACOTTA, Blocks.MUD_BRICKS, Blocks.PRISMARINE, Blocks.BLACKSTONE, Blocks.END_STONE).get(i).defaultBlockState());
			}
			int stone = 0;
			int all = 0;
			for (int seed = 0; seed < 50; seed++) {
				StrataStack stack = StrataStack.generate(RandomSource.create(seed), Blocks.STONE.defaultBlockState(), 0.25F, materials, 600, 1, 6);
				int previousTop = 0;
				for (int i = 0; i < stack.layerCount(); i++) {
					int thickness = stack.layerTop(i) - previousTop;
					all += thickness;
					if (stack.layerState(i).is(Blocks.STONE)) {
						stone += thickness;
					}
					previousTop = stack.layerTop(i);
				}
			}
			float share = stone / (float) all;
			assertTrue(share > 0.15F && share < 0.35F, "stone share " + share + " with " + extra + " extra rocks");
		}
	}

	@Test
	void thinBedsMakeManyLayers() {
		// a 60 block cliff should cut through a lasagna of layers, not a few thick bands
		StrataStack stack = StrataStack.generate(RandomSource.create(5L), Blocks.STONE.defaultBlockState(), 0.25F, rocks, 600, 1, 6);
		int layers = 0;
		for (int i = 0; i < stack.layerCount() && stack.layerTop(i) < 300; i++) {
			if (stack.layerTop(i) >= 240) {
				layers++;
			}
		}
		assertTrue(layers >= 12, layers + " layers in 60 blocks");
	}

	@Test
	void manyModdedRocksStayCoherentPerRegion() {
		// like a modpack with several rock mods
		List<BlockState> many = new java.util.ArrayList<>(rocks);
		for (net.minecraft.world.level.block.Block block : List.of(Blocks.TUFF, Blocks.CALCITE, Blocks.SMOOTH_BASALT, Blocks.DRIPSTONE_BLOCK, Blocks.SANDSTONE, Blocks.TERRACOTTA, Blocks.MUD_BRICKS, Blocks.PRISMARINE, Blocks.BLACKSTONE, Blocks.END_STONE)) {
			many.add(block.defaultBlockState());
		}
		java.util.Set<BlockState> seen = new java.util.HashSet<>();
		RandomSource random = RandomSource.create(99L);
		for (int i = 0; i < 100; i++) {
			StrataStack stack = StrataStack.generate(random, Blocks.STONE.defaultBlockState(), 0.25F, many, 600, 3, 14);
			java.util.Set<BlockState> used = new java.util.HashSet<>();
			for (int layer = 0; layer < stack.layerCount(); layer++) {
				used.add(stack.layerState(layer));
			}
			// stone and at most four others in any one region
			assertTrue(used.size() <= 5, "a region uses " + used);
			assertTrue(used.contains(Blocks.STONE.defaultBlockState()));
			seen.addAll(used);
		}
		// but across the world every rock shows up
		assertEquals(new java.util.HashSet<>(many), seen);
	}

	@Test
	void aSingleRockMakesOneLayer() {
		StrataStack stack = StrataStack.generate(RandomSource.create(1L), Blocks.STONE.defaultBlockState(), 0.25F, List.of(Blocks.STONE.defaultBlockState()), 100, 3, 14);
		assertEquals(1, stack.layerCount());
		assertTrue(stack.layerTop(0) >= 100);
	}
}
