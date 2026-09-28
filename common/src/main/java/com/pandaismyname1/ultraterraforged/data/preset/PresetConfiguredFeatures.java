package com.pandaismyname1.ultraterraforged.data.preset;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.RandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.AlterGroundDecorator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.data.preset.settings.MiscellaneousSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.SurfaceSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.TerrainSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.BushFeature;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.DiskFeature;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.ErodeSnowFeature;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.SwampSurfaceFeature;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance.ChanceFeature;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance.ChanceModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance.UTFChanceModifiers;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.TemplateFeature;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.decorator.DecoratorConfig;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.decorator.TemplateDecorator;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.decorator.TemplateDecorators;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.decorator.TreeContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.paste.PasteConfig;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.placement.TemplatePlacement;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.placement.TemplatePlacements;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.template.TemplateContext;

public class PresetConfiguredFeatures {
	public static final ResourceKey<Feature> ERODE_SNOW = createKey("erode_snow");
	public static final ResourceKey<Feature> SWAMP_SURFACE = createKey("swamp_surface");

	public static final ResourceKey<Feature> OAK_SMALL = createKey("oak/small");
	public static final ResourceKey<Feature> OAK_FOREST = createKey("oak/forest");
	public static final ResourceKey<Feature> OAK_LARGE = createKey("oak/large");
	public static final ResourceKey<Feature> BIRCH_SMALL = createKey("birch/small");
	public static final ResourceKey<Feature> BIRCH_FOREST = createKey("birch/forest");
	public static final ResourceKey<Feature> BIRCH_LARGE = createKey("birch/large");
	public static final ResourceKey<Feature> ACACIA_SMALL = createKey("acacia/small");
	public static final ResourceKey<Feature> ACACIA_LARGE = createKey("acacia/large");
	public static final ResourceKey<Feature> DARK_OAK_SMALL = createKey("dark_oak/small");
	public static final ResourceKey<Feature> DARK_OAK_LARGE = createKey("dark_oak/large");
	public static final ResourceKey<Feature> HUGE_BROWN_MUSHROOM = createKey("mushrooms/huge_brown_mushroom");
	public static final ResourceKey<Feature> HUGE_RED_MUSHROOM = createKey("mushrooms/huge_red_mushroom");
	public static final ResourceKey<Feature> WILLOW_SMALL = createKey("willow/small");
	public static final ResourceKey<Feature> WILLOW_LARGE = createKey("willow/large");
	public static final ResourceKey<Feature> MEADOW_NORMAL = createKey("meadow/normal");
	public static final ResourceKey<Feature> MEADOW_VARIANT = createKey("meadow/variant");
	public static final ResourceKey<Feature> PINE = createKey("pine/pine");
	public static final ResourceKey<Feature> SPRUCE_SMALL = createKey("spruce/small");
	public static final ResourceKey<Feature> SPRUCE_LARGE = createKey("spruce/large");
	public static final ResourceKey<Feature> SPRUCE_SMALL_ON_SNOW = createKey("spruce/small_on_snow");
	public static final ResourceKey<Feature> SPRUCE_LARGE_ON_SNOW = createKey("spruce/large_on_snow");
	public static final ResourceKey<Feature> REDWOOD_LARGE = createKey("redwood/large");
	public static final ResourceKey<Feature> REDWOOD_HUGE = createKey("redwood/huge");
	public static final ResourceKey<Feature> JUNGLE_SMALL = createKey("jungle/small");
	public static final ResourceKey<Feature> JUNGLE_LARGE = createKey("jungle/large");
	public static final ResourceKey<Feature> JUNGLE_HUGE = createKey("jungle/huge");

	public static final ResourceKey<Feature> ACACIA_BUSH = createKey("acacia/bush");
	public static final ResourceKey<Feature> MARSH_BUSH = createKey("shrubs/marsh_bush");
	public static final ResourceKey<Feature> PLAINS_BUSH = createKey("shrubs/plains_bush");
	public static final ResourceKey<Feature> STEPPE_BUSH = createKey("shrubs/steppe_bush");
	public static final ResourceKey<Feature> COLD_STEPPE_BUSH = createKey("shrubs/cold_steppe_bush");
	public static final ResourceKey<Feature> TAIGA_SCRUB_BUSH = createKey("shrubs/taiga_scrub_bush");

	public static final ResourceKey<Feature> FOREST_GRASS = createKey("forest_grass");
	public static final ResourceKey<Feature> MEADOW_GRASS = createKey("meadow_grass");
	public static final ResourceKey<Feature> FERN_GRASS = createKey("fern_grass");
	public static final ResourceKey<Feature> BIRCH_GRASS = createKey("birch_grass");
	
	public static final ResourceKey<Feature> PLAINS_TREES = createKey("plains_trees");
	public static final ResourceKey<Feature> FOREST_TREES = createKey("forest_trees");
	public static final ResourceKey<Feature> FLOWER_FOREST_TREES = createKey("flower_forest_trees");
	public static final ResourceKey<Feature> BIRCH_TREES = createKey("birch_trees");
	public static final ResourceKey<Feature> DARK_FOREST_TREES = createKey("dark_forest_trees");
	public static final ResourceKey<Feature> SAVANNA_TREES = createKey("savanna_trees");
	public static final ResourceKey<Feature> BADLANDS_TREES = createKey("badlands_trees");
	public static final ResourceKey<Feature> WOODED_BADLANDS_TREES = createKey("wooded_badlands_trees");
	public static final ResourceKey<Feature> SWAMP_TREES = createKey("swamp_trees");
	public static final ResourceKey<Feature> MEADOW_TREES = createKey("meadow_trees");
	public static final ResourceKey<Feature> FIR_TREES = createKey("fir_trees");
	public static final ResourceKey<Feature> GROVE_TREES = createKey("grove_trees");
	public static final ResourceKey<Feature> SPRUCE_TREES = createKey("spruce_trees");
	public static final ResourceKey<Feature> SPRUCE_TUNDRA_TREES = createKey("spruce_tundra_trees");
	public static final ResourceKey<Feature> REDWOOD_TREES = createKey("redwood_trees");
	public static final ResourceKey<Feature> JUNGLE_TREES = createKey("jungle_trees");
	public static final ResourceKey<Feature> JUNGLE_EDGE_TREES = createKey("jungle_edge_trees");
	
	public static void bootstrap(Preset preset, BootstrapContext<Feature> ctx) {
		MiscellaneousSettings miscellaneous = preset.miscellaneous();
		SurfaceSettings surface = preset.surface();
		SurfaceSettings.Erosion erosion = surface.erosion();

		TerrainSettings terrain = preset.terrain();
		TerrainSettings.General general = terrain.general;
		
		if(miscellaneous.naturalSnowDecorator || miscellaneous.smoothLayerDecorator || preset.surface().erosion().snowAspect > 0) {
//			ErodeFeature.Config erodeConfig = new ErodeFeature.Config(miscellaneous.rockTag(), erosion.rockVariance, erosion.rockMin, erosion.dirtVariance, erosion.dirtMin, erosion.rockSteepness, erosion.dirtSteepness, erosion.screeSteepness, erosion.heightModifier / 255F, erosion.slopeModifier / 255F, 256, 3F / 255F, 0.55F);
			ctx.register(ERODE_SNOW, new ErodeSnowFeature(erosion.snowSteepness, (float) erosion.snowHeight / 255.0F, miscellaneous.naturalSnowDecorator, miscellaneous.smoothLayerDecorator, erosion.heightModifier / 255F, erosion.slopeModifier / 255F, erosion.snowAspect));
		}
		
		ctx.register(SWAMP_SURFACE, new SwampSurfaceFeature(Blocks.CLAY.defaultBlockState(), Blocks.MUD.defaultBlockState(), Blocks.MUD.defaultBlockState()));
		
		if(miscellaneous.customBiomeFeatures) {
			HolderGetter<PlacedFeature> placedFeatures = ctx.lookup(Registries.PLACED_FEATURE);
			Holder<PlacedFeature> oakSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.OAK_SMALL);
			Holder<PlacedFeature> oakForest = placedFeatures.getOrThrow(PresetPlacedFeatures.OAK_FOREST);
			Holder<PlacedFeature> oakLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.OAK_LARGE);
			Holder<PlacedFeature> birchSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.BIRCH_SMALL);
			Holder<PlacedFeature> birchForest = placedFeatures.getOrThrow(PresetPlacedFeatures.BIRCH_FOREST);
			Holder<PlacedFeature> birchLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.BIRCH_LARGE);
			Holder<PlacedFeature> acaciaBush = placedFeatures.getOrThrow(PresetPlacedFeatures.ACACIA_BUSH);
			Holder<PlacedFeature> acaciaSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.ACACIA_SMALL);
			Holder<PlacedFeature> acaciaLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.ACACIA_LARGE);
			Holder<PlacedFeature> darkOakSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.DARK_OAK_SMALL);
			Holder<PlacedFeature> darkOakLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.DARK_OAK_LARGE);
			Holder<PlacedFeature> hugeBrownMushroom = placedFeatures.getOrThrow(PresetPlacedFeatures.HUGE_BROWN_MUSHROOM);
			Holder<PlacedFeature> hugeRedMushroom = placedFeatures.getOrThrow(PresetPlacedFeatures.HUGE_RED_MUSHROOM);
			Holder<PlacedFeature> willowSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.WILLOW_SMALL);
			Holder<PlacedFeature> willowLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.WILLOW_LARGE);
			Holder<PlacedFeature> meadowNormal = placedFeatures.getOrThrow(PresetPlacedFeatures.MEADOW_NORMAL);
			Holder<PlacedFeature> meadowVariant = placedFeatures.getOrThrow(PresetPlacedFeatures.MEADOW_VARIANT);
			Holder<PlacedFeature> spruceSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.SPRUCE_SMALL);
			Holder<PlacedFeature> spruceLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.SPRUCE_LARGE);
			Holder<PlacedFeature> spruceSmallOnSnow = placedFeatures.getOrThrow(PresetPlacedFeatures.SPRUCE_SMALL_ON_SNOW);
			Holder<PlacedFeature> spruceLargeOnSnow = placedFeatures.getOrThrow(PresetPlacedFeatures.SPRUCE_LARGE_ON_SNOW);
			Holder<PlacedFeature> redwoodLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.REDWOOD_LARGE);
			Holder<PlacedFeature> redwoodHuge = placedFeatures.getOrThrow(PresetPlacedFeatures.REDWOOD_HUGE);
			Holder<PlacedFeature> jungleSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.JUNGLE_SMALL);
			Holder<PlacedFeature> jungleLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.JUNGLE_LARGE);
			Holder<PlacedFeature> jungleHuge = placedFeatures.getOrThrow(PresetPlacedFeatures.JUNGLE_HUGE);
			Holder<PlacedFeature> jungleBush = placedFeatures.getOrThrow(TreePlacements.JUNGLE_BUSH);

			ctx.register(OAK_SMALL, makeTree(PresetTemplatePaths.OAK_SMALL));
			ctx.register(OAK_FOREST, makeTreeWithBehives(PresetTemplatePaths.OAK_FOREST, PresetTemplateDecoratorLists.BEEHIVE_RARITY_005, ImmutableMap.of(
				Biomes.PLAINS, PresetTemplateDecoratorLists.BEEHIVE_RARITY_005,
				Biomes.SUNFLOWER_PLAINS, PresetTemplateDecoratorLists.BEEHIVE_RARITY_005,
				Biomes.FLOWER_FOREST, PresetTemplateDecoratorLists.BEEHIVE_RARITY_002_AND_005
			)));
			ctx.register(OAK_LARGE, makeTreeWithBehives(PresetTemplatePaths.OAK_LARGE, PresetTemplateDecoratorLists.BEEHIVE_RARITY_0002_AND_005, ImmutableMap.of(
				Biomes.PLAINS, PresetTemplateDecoratorLists.BEEHIVE_RARITY_005,
				Biomes.SUNFLOWER_PLAINS, PresetTemplateDecoratorLists.BEEHIVE_RARITY_005,
				Biomes.FLOWER_FOREST, PresetTemplateDecoratorLists.BEEHIVE_RARITY_002_AND_005
			)));
			ctx.register(BIRCH_SMALL, makeTree(PresetTemplatePaths.BIRCH_SMALL));
			ctx.register(BIRCH_FOREST, makeTreeWithBehives(PresetTemplatePaths.BIRCH_FOREST, PresetTemplateDecoratorLists.BEEHIVE_RARITY_0002_AND_005, ImmutableMap.of(
				Biomes.PLAINS, PresetTemplateDecoratorLists.BEEHIVE_RARITY_005,
				Biomes.SUNFLOWER_PLAINS, PresetTemplateDecoratorLists.BEEHIVE_RARITY_005,
				Biomes.FLOWER_FOREST, PresetTemplateDecoratorLists.BEEHIVE_RARITY_002_AND_005
			)));
			ctx.register(BIRCH_LARGE, makeTreeWithBehives(PresetTemplatePaths.BIRCH_LARGE, PresetTemplateDecoratorLists.BEEHIVE_RARITY_0002_AND_005, ImmutableMap.of(
				Biomes.PLAINS, PresetTemplateDecoratorLists.BEEHIVE_RARITY_005,
				Biomes.SUNFLOWER_PLAINS, PresetTemplateDecoratorLists.BEEHIVE_RARITY_005,
				Biomes.FLOWER_FOREST, PresetTemplateDecoratorLists.BEEHIVE_RARITY_002_AND_005
			)));
			ctx.register(ACACIA_SMALL, makeTree(PresetTemplatePaths.ACACIA_SMALL));
			ctx.register(ACACIA_LARGE, makeTree(PresetTemplatePaths.ACACIA_LARGE));
			ctx.register(DARK_OAK_SMALL, makeTree(PresetTemplatePaths.DARK_OAK_SMALL));
			ctx.register(DARK_OAK_LARGE, makeTree(PresetTemplatePaths.DARK_OAK_LARGE));
			ctx.register(HUGE_BROWN_MUSHROOM, makeTree(PresetTemplatePaths.BROWN_MUSHROOM, 0));
			ctx.register(HUGE_RED_MUSHROOM, makeTree(PresetTemplatePaths.RED_MUSHROOM));
			ctx.register(WILLOW_SMALL, makeTree(PresetTemplatePaths.WILLOW_SMALL));
			ctx.register(WILLOW_LARGE, makeTree(PresetTemplatePaths.WILLOW_LARGE));
			ctx.register(MEADOW_NORMAL, makeTreeWithBehives(PresetTemplatePaths.MEADOW_NORMAL, PresetTemplateDecoratorLists.BEEHIVE_RARITY_0075));
			ctx.register(MEADOW_VARIANT, makeTreeWithBehives(PresetTemplatePaths.MEADOW_VARIANT, PresetTemplateDecoratorLists.BEEHIVE_RARITY_0075));
			TemplateFeature<?> pineConfig = makeTree(PresetTemplatePaths.PINE);
			ctx.register(PINE, pineConfig);
			ctx.register(SPRUCE_SMALL, makeTree(PresetTemplatePaths.SPRUCE_SMALL));
			ctx.register(SPRUCE_LARGE, makeTree(PresetTemplatePaths.SPRUCE_LARGE));
			ctx.register(SPRUCE_SMALL_ON_SNOW, makeTree(PresetTemplatePaths.SPRUCE_SMALL, TemplatePlacements.any(), 3));
			ctx.register(SPRUCE_LARGE_ON_SNOW, makeTree(PresetTemplatePaths.SPRUCE_LARGE, TemplatePlacements.any(), 3));
			ctx.register(REDWOOD_LARGE, makeTree(PresetTemplatePaths.REDWOOD_LARGE));
			TemplateFeature<?> redwoodHugeConfig = makeTree(PresetTemplatePaths.REDWOOD_HUGE, ImmutableList.of(TemplateDecorators.tree(new AlterGroundDecorator(BlockStateProvider.holderOf(Blocks.PODZOL)))), TemplatePlacements.tree(), 3);
			ctx.register(REDWOOD_HUGE, redwoodHugeConfig);
			TemplateFeature<?> jungleSmallConfig = makeTree(PresetTemplatePaths.JUNGLE_SMALL);
			ctx.register(JUNGLE_SMALL, jungleSmallConfig);
			ctx.register(JUNGLE_LARGE, makeTree(PresetTemplatePaths.JUNGLE_LARGE));
			ctx.register(JUNGLE_HUGE, makeTree(PresetTemplatePaths.JUNGLE_HUGE));
			
			ctx.register(ACACIA_BUSH, makeTree(PresetTemplatePaths.ACACIA_BUSH, 2));
			ctx.register(MARSH_BUSH, makeSmallBush(Blocks.OAK_LOG, Blocks.BIRCH_LEAVES, 0.05F, 0.09F, 0.65F));
			ctx.register(PLAINS_BUSH, makeSmallBush(Blocks.OAK_LOG, Blocks.BIRCH_LEAVES, 0.05F, 0.09F, 0.65F));
			ctx.register(STEPPE_BUSH, makeSmallBush(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, 0.06F, 0.08F, 0.7F));
			ctx.register(COLD_STEPPE_BUSH, makeSmallBush(Blocks.SPRUCE_LOG, Blocks.OAK_LEAVES, 0.05F, 0.075F, 0.6F));
			ctx.register(TAIGA_SCRUB_BUSH, makeSmallBush(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, 0.05F, 0.075F, 0.6F));
			
			ctx.register(PLAINS_TREES, makeRandom(oakForest, List.of(
				makeWeighted(0.2F, oakForest), 
				makeWeighted(0.3F, oakLarge)
			)));
			ctx.register(FOREST_TREES, makeRandom(oakForest, List.of(
				makeWeighted(0.2F, oakForest), 
				makeWeighted(0.3F, oakLarge)
			)));
			ctx.register(FLOWER_FOREST_TREES, makeRandom(birchForest, List.of(
				makeWeighted(0.2F, birchForest), 
				makeWeighted(0.2F, birchLarge), 
				makeWeighted(0.2F, oakForest), 
				makeWeighted(0.2F, oakLarge)
			)));
			ctx.register(BIRCH_TREES, makeChance(
				makeChanceEntry(birchLarge, 0.2F, UTFChanceModifiers.elevation(0.25F, 0.0F), UTFChanceModifiers.biomeEdge(0.1F, 0.3F)),
				makeChanceEntry(birchForest, 0.2F, UTFChanceModifiers.elevation(0.3F, 0.0F), UTFChanceModifiers.biomeEdge(0.05F, 0.2F)),
				makeChanceEntry(birchSmall, 0.1F, UTFChanceModifiers.biomeEdge(0.25F, 0.0F)),
				makeChanceEntry(birchSmall, 0.1F, UTFChanceModifiers.elevation(0.25F, 0.65F))
			));
			ctx.register(DARK_FOREST_TREES, makeRandom(darkOakLarge, List.of(
				makeWeighted(0.025F, hugeBrownMushroom),
				makeWeighted(0.05F, hugeRedMushroom),
				makeWeighted(0.3F, darkOakLarge),
				makeWeighted(0.2F, darkOakSmall),
				makeWeighted(0.05F, birchForest),
				makeWeighted(0.025F, oakForest)
			)));
			ctx.register(SAVANNA_TREES, makeRandom(acaciaLarge, List.of(
				makeWeighted(0.4F, acaciaLarge), 
				makeWeighted(0.15F, acaciaSmall)
			)));
			ctx.register(BADLANDS_TREES, makeRandom(oakSmall, List.of(
				makeWeighted(0.2F, oakSmall), 
				makeWeighted(0.1F, acaciaBush)
			)));
			ctx.register(WOODED_BADLANDS_TREES, makeRandom(oakSmall, List.of(
				makeWeighted(0.3F, oakSmall), 
				makeWeighted(0.2F, acaciaBush)
			)));
			ctx.register(SWAMP_TREES, makeRandom(willowLarge, List.of(
				makeWeighted(0.2F, willowSmall), 
				makeWeighted(0.35F, willowLarge)
			)));
			ctx.register(MEADOW_TREES, makeRandom(meadowNormal, List.of(
				makeWeighted(0.2F, meadowNormal), 
				makeWeighted(0.35F, meadowVariant)
			)));
			ctx.register(FIR_TREES, makeChance(
				makeChanceEntry(spruceSmall, 0.1F, UTFChanceModifiers.elevation(0.55F, 0.2F)),
				makeChanceEntry(spruceLarge, 0.25F, UTFChanceModifiers.elevation(0.3F, 0.0F))
			));
			ctx.register(GROVE_TREES, makeChance(
				makeChanceEntry(spruceSmallOnSnow, 0.1F, UTFChanceModifiers.elevation(1.0F, 0.2F)),
				makeChanceEntry(spruceLargeOnSnow, 0.25F, UTFChanceModifiers.elevation(0.3F, 0.0F))
			));
			
			ctx.register(REDWOOD_TREES, makeChance(
				makeChanceEntry(redwoodHuge, 0.4F, UTFChanceModifiers.elevation(0.15F, 0.0F), UTFChanceModifiers.biomeEdge(0.1F, 0.3F)),
				makeChanceEntry(redwoodLarge, 0.2F, UTFChanceModifiers.elevation(0.25F, 0.0F), UTFChanceModifiers.biomeEdge(0.05F, 0.25F)),
				makeChanceEntry(spruceLarge, 0.4F, UTFChanceModifiers.elevation(0.35F, 0.15F)),
				makeChanceEntry(spruceSmall, 0.2F, UTFChanceModifiers.elevation(0.5F, 0.2F))
			));
			ctx.register(JUNGLE_TREES, makeRandom(jungleSmall, List.of(
				makeWeighted(0.2F, jungleSmall), 
				makeWeighted(0.3F, jungleLarge),
				makeWeighted(0.4F, jungleHuge),
				makeWeighted(0.5F, jungleBush)
			)));
			ctx.register(JUNGLE_EDGE_TREES, makeRandom(jungleSmall, List.of(
				makeWeighted(0.2F, jungleSmall), 
				makeWeighted(0.3F, jungleLarge),
				makeWeighted(0.4F, jungleBush)
			)));
			ctx.register(FOREST_GRASS, makeRandom(
				makePatch(Blocks.SHORT_GRASS, 48),
				List.of(
					makeWeighted(0.5F, makePatch(Blocks.SHORT_GRASS, 56)),
					makeWeighted(0.4F, makePatch(Blocks.TALL_GRASS, 56)),
					makeWeighted(0.2F, makePatch(Blocks.LARGE_FERN, 48)),
					makeWeighted(0.2F, makePatch(Blocks.FERN, 24))
				)
			));
			ctx.register(MEADOW_GRASS, makeRandom(
				makePatch(Blocks.FERN, 15),
				List.of(
					makeWeighted(0.5F, makePatch(Blocks.SHORT_GRASS, 15)),
					makeWeighted(0.5F, makePatch(Blocks.FERN, 36)),
					makeWeighted(0.2F, makePatch(Blocks.LARGE_FERN, 55)),
					makeWeighted(0.4F, makePatch(Blocks.TALL_GRASS, 45))
				)
			));
			ctx.register(FERN_GRASS, makeRandom(
				makePatch(Blocks.SHORT_GRASS, 48),
				List.of(
					makeWeighted(0.55F, makePatch(Blocks.SHORT_GRASS, 56)),
					makeWeighted(0.2F, makePatch(Blocks.TALL_GRASS, 24)),
					makeWeighted(0.3F, makePatch(Blocks.LARGE_FERN, 24)),
					makeWeighted(0.5F, makePatch(Blocks.FERN, 36))
				)
			));
			ctx.register(BIRCH_GRASS, makeRandom(
				makePatch(Blocks.TALL_GRASS, 48),
				List.of(
					makeWeighted(0.8F, makePatch(Blocks.TALL_GRASS, 56)),
					makeWeighted(0.5F, makePatch(Blocks.LILAC, 64)),
					makeWeighted(0.3F, makePatch(Blocks.LARGE_FERN, 48)),
					makeWeighted(0.2F, makePatch(Blocks.FERN, 24)),
					makeWeighted(0.1F, makePatch(Blocks.PEONY, 32))
				)
			));

	        ctx.register(MiscOverworldFeatures.DISK_CLAY, new DiskFeature(BlockStateProvider.holderOf(Blocks.CLAY), BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.CLAY), UniformInt.of(2, 3), 1));
	        ctx.register(MiscOverworldFeatures.DISK_GRAVEL, new DiskFeature(BlockStateProvider.holderOf(Blocks.GRAVEL), BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.GRASS_BLOCK), UniformInt.of(2, 5), 2));
	        ctx.register(MiscOverworldFeatures.DISK_SAND, new DiskFeature(Holder.direct(new RuleBasedStateProvider(BlockStateProvider.holderOf(Blocks.SAND), List.of(new RuleBasedStateProvider.Rule(BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.AIR), BlockStateProvider.holderOf(Blocks.SANDSTONE))))), BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.GRASS_BLOCK), UniformInt.of(2, 6), 2));

	        ctx.register(TreeFeatures.ACACIA, makeRandom(acaciaSmall, List.of(
	        	makeWeighted(1.0F, acaciaSmall), 
	        	makeWeighted(0.5F, acaciaLarge)
			)));
	        RandomSelectorFeature birchConfig = makeRandom(birchSmall, List.of(
	        	makeWeighted(1.0F, birchSmall), 
	        	makeWeighted(0.5F, birchLarge)
			));
	        ctx.register(TreeFeatures.BIRCH, birchConfig);
	        ctx.register(TreeFeatures.BIRCH_BEES_005, birchConfig);
	        ctx.register(TreeFeatures.DARK_OAK, makeRandom(darkOakSmall, List.of(
	        	makeWeighted(1.0F, darkOakSmall), 
	        	makeWeighted(0.5F, darkOakLarge)
			)));
	        ctx.register(TreeFeatures.JUNGLE_TREE_NO_VINE, jungleSmallConfig);
	        ctx.register(TreeFeatures.MEGA_JUNGLE_TREE, makeRandom(jungleLarge, List.of(
	        	makeWeighted(1.0F, jungleLarge), 
	        	makeWeighted(0.5F, jungleHuge)
			)));
	        RandomSelectorFeature oakConfig = makeRandom(oakSmall, List.of(
	        	makeWeighted(1.0F, oakSmall), 
	        	makeWeighted(0.5F, oakLarge)
			));
	        ctx.register(TreeFeatures.OAK, oakConfig);
	        ctx.register(TreeFeatures.OAK_BEES_005, oakConfig);
	        ctx.register(TreeFeatures.FANCY_OAK_BEES_005, oakConfig);
	        ctx.register(TreeFeatures.FANCY_OAK, oakConfig);
	        ctx.register(TreeFeatures.SPRUCE, makeRandom(spruceSmall, List.of(
	        	makeWeighted(1.0F, spruceSmall), 
	        	makeWeighted(0.5F, spruceLarge)
			)));
	        ctx.register(TreeFeatures.PINE, pineConfig);
	        ctx.register(TreeFeatures.MEGA_PINE, redwoodHugeConfig);
	        ctx.register(TreeFeatures.MEGA_SPRUCE, redwoodHugeConfig);
	    }
	}
	
	private static BushFeature makeSmallBush(Block log, Block leaves, float air, float leaf, float size) {
		return new BushFeature(log.defaultBlockState(), leaves.defaultBlockState(), air, leaf, size);
	}

	private static TemplateFeature<?> makeTree(List<Identifier> templates) {
		return makeTree(templates, 3);
	}
		
	private static TemplateFeature<?> makeTree(List<Identifier> templates, int baseExtension) {
		return makeTree(templates, TemplatePlacements.tree(), baseExtension);
	}
	
	private static TemplateFeature<?> makeTree(List<Identifier> templates, TemplatePlacement<?> placement, int baseExtension) {
		return makeTree(templates, ImmutableList.of(), placement, baseExtension);
	}
	
	private static <T extends TemplateContext> TemplateFeature<T> makeTree(List<Identifier> templates, List<TemplateDecorator<T>> decorators, TemplatePlacement<T> placement, int baseExtension) {
		return new TemplateFeature<>(templates, placement, new PasteConfig(baseExtension, false, true, false, false), new DecoratorConfig<>(decorators, ImmutableMap.of()));
	}

	private static TemplateFeature<TreeContext> makeTreeWithBehives(List<Identifier> templates, List<TemplateDecorator<TreeContext>> behives) {
		return makeTreeWithBehives(templates, behives, ImmutableMap.of());
	}
	
	private static TemplateFeature<TreeContext> makeTreeWithBehives(List<Identifier> templates, List<TemplateDecorator<TreeContext>> behives, Map<ResourceKey<Biome>, List<TemplateDecorator<TreeContext>>> biomeOverrides) {
		return new TemplateFeature<>(templates, TemplatePlacements.tree(), new PasteConfig(3, false, true, false, false), new DecoratorConfig<>(behives, biomeOverrides));
	}
	
	private static ChanceFeature makeChance(ChanceFeature.Entry... entries) {
		return new ChanceFeature(ImmutableList.copyOf(entries));
	}

	private static ChanceFeature.Entry makeChanceEntry(Holder<PlacedFeature> feature, float chance, ChanceModifier... modifiers) {
		return new ChanceFeature.Entry(feature, chance, ImmutableList.copyOf(modifiers));
	}
	
	// vanilla's random_selector: each entry tried in turn with its chance, else the default. Deprecated in 26.3 but still
	// registered, and vanilla's own tree features still use it; weighted_random_selector would pick differently
	private static RandomSelectorFeature makeRandom(Holder<PlacedFeature> defaultFeature, List<WeightedPlacedFeature> entries) {
		return new RandomSelectorFeature(entries, defaultFeature);
	}

    private static WeightedPlacedFeature makeWeighted(float weight, Holder<PlacedFeature> feature) {
    	return new WeightedPlacedFeature(feature, weight);
    }
    
    // what random_patch did before 26.1 removed it: the block, tried at this many spots around, only where there's air
    // (26.3 renamed random_offset to offset)
    private static Holder<PlacedFeature> makePatch(Block state, int tries) {
        return Holder.direct(new PlacedFeature(Holder.direct(new SimpleBlockFeature(BlockStateProvider.holderOf(state.defaultBlockState()))), List.of(
        	CountPlacement.of(tries),
        	OffsetPlacement.ofTriangle(7, 3),
        	BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
        )));
    }

	protected static ResourceKey<Feature> createKey(String name) {
        return ResourceKey.create(Registries.FEATURE, UTFCommon.location(name));
	}
}
