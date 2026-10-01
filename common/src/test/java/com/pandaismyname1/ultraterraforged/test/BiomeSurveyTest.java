package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.Continentalness;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

/**
 * The vanilla overworld biomes the Default preset's climate reaches at the surface: its cells run through vanilla's
 * biome table, as the biome source does. Variant biomes, swamps and rivers used to be out of reach.
 *
 * Chunks read their climate from tiles and /locate from columns it looks up one by one: the two have to agree, or a
 * chunk gets another biome than /locate finds there.
 *
 * Land has a land climate: islands and peninsulas in the continent's sea used to get ocean biomes, and with them
 * shipwrecks and ocean ruins on dry ground.
 */
public class BiomeSurveyTest {

	@Test
	@SuppressWarnings("unchecked")
	void vanillaBiomesAreReachable() throws ReflectiveOperationException {
		TestBootstrap.init();
		// the variants of vanilla's slices, its most eroded lowlands and its valleys
		List<ResourceKey<Biome>> reachable = List.of(
			Biomes.SUNFLOWER_PLAINS, Biomes.ICE_SPIKES, Biomes.CHERRY_GROVE,
			Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.OLD_GROWTH_PINE_TAIGA,
			Biomes.FROZEN_PEAKS, Biomes.ERODED_BADLANDS, Biomes.WINDSWEPT_SAVANNA,
			Biomes.SWAMP, Biomes.MANGROVE_SWAMP, Biomes.RIVER, Biomes.FROZEN_RIVER,
			Biomes.PLAINS, Biomes.MEADOW, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS
		);
		// vanilla's overworld biome table; addBiomes isn't public
		List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> points = new ArrayList<>();
		Method addBiomes = OverworldBiomeBuilder.class.getDeclaredMethod("addBiomes", Consumer.class);
		addBiomes.setAccessible(true);
		addBiomes.invoke(new OverworldBiomeBuilder(), (Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>) points::add);
		Climate.ParameterList<ResourceKey<Biome>> biomes = new Climate.ParameterList<>(points);

		Map<ResourceKey<Biome>, Integer> counts = new TreeMap<>((a, b) -> a.location().compareTo(b.location()));
		int total = 0;
		GeneratorContext context = GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(), 1111, 3, 6, false);
		// the columns the world reads, every 64 blocks over 25,600 blocks square
		Cell cell = new Cell();
		for (int x = -12800; x < 12800; x += 64) {
			for (int z = -12800; z < 12800; z += 64) {
				context.lookup.apply(cell.reset(), x, z);
				// the sea is in vanilla's hands; rivers, lakes and wetlands are counted
				if (cell.terrain.isDeepOcean() || cell.terrain.isShallowOcean()) {
					continue;
				}
				Climate.TargetPoint target = Climate.target(cell.temperature, cell.moisture, cell.continentalness, cell.erosion, 0.0F, cell.weirdness);
				counts.merge(biomes.findValue(target), 1, Integer::sum);
				total++;
			}
		}
		int land = total;
		counts.forEach((biome, count) -> System.out.printf("%-28s %6.2f%%%n", biome.location().getPath(), 100.0F * count / land));
		for (ResourceKey<Biome> biome : reachable) {
			assertTrue(counts.containsKey(biome), biome.location() + " never generates");
		}
	}

	@Test
	void tilesAgreeWithColumnLookups() {
		TestBootstrap.init();
		GeneratorContext context = GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(), 1111, 3, 6, false);
		Cell computed = new Cell();
		int total = 0;
		int differ = 0;
		for (int x = -4000; x < 4000; x += 80) {
			for (int z = -4000; z < 4000; z += 80) {
				Tile tile = context.cache.provide(context.cache.chunkToTile(x >> 4), context.cache.chunkToTile(z >> 4));
				Cell tiled = tile.lookup(x, z);
				context.lookup.compute(computed.reset(), x, z);
				total++;
				// only the coast may differ, where erosion has moved the shore
				if (Math.abs(computed.temperature - tiled.temperature) > 1.0E-4F || Math.abs(computed.moisture - tiled.moisture) > 1.0E-4F
					|| Math.abs(computed.continentalness - tiled.continentalness) > 1.0E-4F || Math.abs(computed.erosion - tiled.erosion) > 1.0E-4F
					|| Math.abs(computed.weirdness - tiled.weirdness) > 1.0E-4F) {
					differ++;
				}
			}
		}
		assertTrue(differ <= total / 100, differ + " of " + total + " columns have another climate in their tile");
	}

	@Test
	void landHasALandClimate() {
		TestBootstrap.init();
		GeneratorContext context = GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(), 1111, 3, 6, false);
		Cell cell = new Cell();
		int land = 0;
		int sea = 0;
		for (int x = -12800; x < 12800; x += 32) {
			for (int z = -12800; z < 12800; z += 32) {
				context.lookup.apply(cell.reset(), x, z);
				if (cell.height < context.levels.ground) {
					continue;
				}
				land++;
				if (cell.continentalness < Continentalness.COAST.min()) {
					sea++;
				}
			}
		}
		assertTrue(land > 0);
		assertTrue(sea == 0, sea + " of " + land + " land columns have the sea's climate");
	}
}
