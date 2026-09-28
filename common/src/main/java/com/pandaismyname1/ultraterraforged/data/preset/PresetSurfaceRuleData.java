package com.pandaismyname1.ultraterraforged.data.preset;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.data.preset.settings.MiscellaneousSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.SurfaceSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.tags.UTFBlockTags;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition.UTFSurfaceConditions;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.UTFSurfaceRules;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.StrataRule;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.Scaling;

public class PresetSurfaceRuleData {
    private static final SurfaceRules.RuleSource AIR = PresetSurfaceRuleData.makeStateRule(Blocks.AIR);
    private static final SurfaceRules.RuleSource BEDROCK = PresetSurfaceRuleData.makeStateRule(Blocks.BEDROCK);
    private static final SurfaceRules.RuleSource WHITE_TERRACOTTA = PresetSurfaceRuleData.makeStateRule(Blocks.WHITE_TERRACOTTA);
    private static final SurfaceRules.RuleSource ORANGE_TERRACOTTA = PresetSurfaceRuleData.makeStateRule(Blocks.ORANGE_TERRACOTTA);
    private static final SurfaceRules.RuleSource BROWN_TERRACOTTA = PresetSurfaceRuleData.makeStateRule(Blocks.BROWN_TERRACOTTA);
    private static final SurfaceRules.RuleSource TERRACOTTA = PresetSurfaceRuleData.makeStateRule(Blocks.TERRACOTTA);
    private static final SurfaceRules.RuleSource RED_SAND = PresetSurfaceRuleData.makeStateRule(Blocks.RED_SAND);
    private static final SurfaceRules.RuleSource RED_SANDSTONE = PresetSurfaceRuleData.makeStateRule(Blocks.RED_SANDSTONE);
    private static final SurfaceRules.RuleSource STONE = PresetSurfaceRuleData.makeStateRule(Blocks.STONE);
    private static final SurfaceRules.RuleSource DEEPSLATE = PresetSurfaceRuleData.makeStateRule(Blocks.DEEPSLATE);
    private static final SurfaceRules.RuleSource DIRT = PresetSurfaceRuleData.makeStateRule(Blocks.DIRT);
    private static final SurfaceRules.RuleSource PODZOL = PresetSurfaceRuleData.makeStateRule(Blocks.PODZOL);
    private static final SurfaceRules.RuleSource COARSE_DIRT = PresetSurfaceRuleData.makeStateRule(Blocks.COARSE_DIRT);
    private static final SurfaceRules.RuleSource MYCELIUM = PresetSurfaceRuleData.makeStateRule(Blocks.MYCELIUM);
    private static final SurfaceRules.RuleSource GRASS_BLOCK = PresetSurfaceRuleData.makeStateRule(Blocks.GRASS_BLOCK);
    private static final SurfaceRules.RuleSource CALCITE = PresetSurfaceRuleData.makeStateRule(Blocks.CALCITE);
    private static final SurfaceRules.RuleSource GRAVEL = PresetSurfaceRuleData.makeStateRule(Blocks.GRAVEL);
    private static final SurfaceRules.RuleSource SAND = PresetSurfaceRuleData.makeStateRule(Blocks.SAND);
    private static final SurfaceRules.RuleSource SANDSTONE = PresetSurfaceRuleData.makeStateRule(Blocks.SANDSTONE);
    private static final SurfaceRules.RuleSource SMOOTH_SANDSTONE = PresetSurfaceRuleData.makeStateRule(Blocks.SMOOTH_SANDSTONE);
    private static final SurfaceRules.RuleSource PACKED_ICE = PresetSurfaceRuleData.makeStateRule(Blocks.PACKED_ICE);
    private static final SurfaceRules.RuleSource SNOW_BLOCK = PresetSurfaceRuleData.makeStateRule(Blocks.SNOW_BLOCK);
    private static final SurfaceRules.RuleSource MUD = PresetSurfaceRuleData.makeStateRule(Blocks.MUD);
    private static final SurfaceRules.RuleSource POWDER_SNOW = PresetSurfaceRuleData.makeStateRule(Blocks.POWDER_SNOW);
    private static final SurfaceRules.RuleSource ICE = PresetSurfaceRuleData.makeStateRule(Blocks.ICE);
    private static final SurfaceRules.RuleSource WATER = PresetSurfaceRuleData.makeStateRule(Blocks.WATER);
    private static final SurfaceRules.RuleSource LAVA = PresetSurfaceRuleData.makeStateRule(Blocks.LAVA);
    private static final SurfaceRules.RuleSource MAGMA_BLOCK = PresetSurfaceRuleData.makeStateRule(Blocks.MAGMA_BLOCK);
    private static final SurfaceRules.RuleSource BASALT = PresetSurfaceRuleData.makeStateRule(Blocks.BASALT);
    private static final SurfaceRules.RuleSource SMOOTH_BASALT = PresetSurfaceRuleData.makeStateRule(Blocks.SMOOTH_BASALT);
    private static final SurfaceRules.RuleSource BLACKSTONE = PresetSurfaceRuleData.makeStateRule(Blocks.BLACKSTONE);
    private static final SurfaceRules.RuleSource TUFF = PresetSurfaceRuleData.makeStateRule(Blocks.TUFF);
    private static final SurfaceRules.RuleSource CLAY = PresetSurfaceRuleData.makeStateRule(Blocks.CLAY);
    private static final SurfaceRules.RuleSource TUBE_CORAL = PresetSurfaceRuleData.makeStateRule(Blocks.TUBE_CORAL_BLOCK);
    private static final SurfaceRules.RuleSource BRAIN_CORAL = PresetSurfaceRuleData.makeStateRule(Blocks.BRAIN_CORAL_BLOCK);
    private static final SurfaceRules.RuleSource BUBBLE_CORAL = PresetSurfaceRuleData.makeStateRule(Blocks.BUBBLE_CORAL_BLOCK);
    private static final SurfaceRules.RuleSource FIRE_CORAL = PresetSurfaceRuleData.makeStateRule(Blocks.FIRE_CORAL_BLOCK);
    private static final SurfaceRules.RuleSource HORN_CORAL = PresetSurfaceRuleData.makeStateRule(Blocks.HORN_CORAL_BLOCK);
    private static final SurfaceRules.RuleSource COBBLESTONE = PresetSurfaceRuleData.makeStateRule(Blocks.COBBLESTONE);
    private static final SurfaceRules.RuleSource MOSSY_COBBLESTONE = PresetSurfaceRuleData.makeStateRule(Blocks.MOSSY_COBBLESTONE);
    // the sea floor is sandy down to this many blocks below the sea, and mixed down to MIDDLE_SEA
    private static final int SHALLOW_SEA = 14;
    private static final int MIDDLE_SEA = 30;
    // rivers this many blocks above the sea run over gravel and cobbles rather than sand
    private static final int MOUNTAIN_RIVER_HEIGHT = 45;
    // where the salt crust noise is above this, it's a crack between the crust's polygons
    private static final float SALT_CRACK = 0.983F;

    private static final Identifier STRATA_CACHE_ID = UTFCommon.location("default");
    private static final Identifier DEEP_STRATA_CACHE_ID = UTFCommon.location("deep");
    private static final int STRATA_VARIANTS = 100;
    // thin beds, so a cliff shows many alternating layers
    private static final int STRATA_MIN_THICKNESS = 1;
    private static final int STRATA_MAX_THICKNESS = 6;
    // share of plain stone, and of deepslate in the deep layers
    private static final float STRATA_STONE_SHARE = 0.25F;
    private static final float STRATA_DEEPSLATE_SHARE = 0.4F;
    // how deep the bare rock of steep slopes stays plain stone when strata are kept off the surface
    private static final int STRATA_PLAIN_DEPTH = 5;
    // the top of the vanilla stone to deepslate transition
    private static final int DEEPSLATE_TOP = 8;
    // every block is at or above the bottom of the world
    private static final SurfaceRules.ConditionSource NEVER = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.bottom(), 0));
    
    private static SurfaceRules.RuleSource makeStateRule(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
    
    public static SurfaceRules.RuleSource overworld(Preset preset, HolderGetter<Noise> noise) {
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		Scaling scaling = Scaling.make(properties.terrainScaler(), properties.seaLevel);
    	MiscellaneousSettings miscellaneousSettings = preset.miscellaneous();
    	
    	SurfaceSettings surfaceSettings = preset.surface();
    	SurfaceSettings.Erosion erosion = surfaceSettings.erosion();

    	SurfaceRules.ConditionSource y4BelowSurface = SurfaceRules.stoneDepthCheck(3, false, CaveSurface.FLOOR);
        SurfaceRules.ConditionSource below97 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(97), 2);
        SurfaceRules.ConditionSource below256 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(256), 0);
        SurfaceRules.ConditionSource above63 = SurfaceRules.yStartCheck(VerticalAnchor.absolute(63), -1);
        SurfaceRules.ConditionSource above74 = SurfaceRules.yStartCheck(VerticalAnchor.absolute(74), 1);
        SurfaceRules.ConditionSource below60 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(60), 0);
        SurfaceRules.ConditionSource below62 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(62), 0);
        SurfaceRules.ConditionSource below63 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(63), 0);
        SurfaceRules.ConditionSource y1BelowSurface = SurfaceRules.waterBlockCheck(-1, 0);
        SurfaceRules.ConditionSource yOnSurface = SurfaceRules.waterBlockCheck(0, 0);
        SurfaceRules.ConditionSource y6BelowSurface = SurfaceRules.waterStartCheck(-6, -1);
        SurfaceRules.ConditionSource hole = SurfaceRules.hole();
        SurfaceRules.ConditionSource frozenOcean = SurfaceRules.isBiome(Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN);
        SurfaceRules.ConditionSource badlands = SurfaceRules.isBiome(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
        SurfaceRules.ConditionSource steep = SurfaceRules.steep();
        // steep slopes wear down to bare rock and coarse dirt, unless the preset turns erosion off
        // (and everything above the rock line, which the default presets set above the build limit)
        // (sand dunes are steep but loose, so they stay sand)
        SurfaceRules.ConditionSource erodedRock = miscellaneousSettings.erosionDecorator ? and(
        	SurfaceRules.not(UTFSurfaceConditions.terrain(TerrainType.DUNES)),
        	UTFSurfaceConditions.any(
        		UTFSurfaceConditions.steepness(erosion.rockSteepness, noise.getOrThrow(PresetSurfaceNoise.STEEPNESS_VARIANCE)),
        		UTFSurfaceConditions.height(noise.getOrThrow(PresetSurfaceNoise.ERODED_ROCK), noise.getOrThrow(PresetSurfaceNoise.HEIGHT_VARIANCE))
        	)
        ) : NEVER;
        SurfaceRules.RuleSource erodedDirt = miscellaneousSettings.erosionDecorator ? makeErodedDirtRule(noise, erosion) : SurfaceRules.ifTrue(NEVER, COARSE_DIRT);
        SurfaceRules.RuleSource grass = SurfaceRules.sequence(
        	erodedDirt,
        	SurfaceRules.ifTrue(
        		yOnSurface,
        		GRASS_BLOCK
        	), 
        	DIRT
        );
        SurfaceRules.RuleSource sand = SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, SANDSTONE), SAND);
        SurfaceRules.RuleSource gravel = SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, STONE), GRAVEL);
        SurfaceRules.RuleSource windswept = SurfaceRules.sequence(
        	SurfaceRules.ifTrue(
        		UTFSurfaceConditions.sediment(5.3F), 
        		STONE
            ),
        	SurfaceRules.ifTrue(
        		UTFSurfaceConditions.sediment(2.4F), 
        		gravel
            ),
        	SurfaceRules.ifTrue(
        		UTFSurfaceConditions.erosion(8.25F),
        		STONE
        	),
        	SurfaceRules.ifTrue(
	        	UTFSurfaceConditions.erosion(5.5F),
	        	gravel
	        )
        );
        SurfaceRules.ConditionSource sandyBeach = SurfaceRules.isBiome(Biomes.WARM_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH);
        SurfaceRules.ConditionSource desert = SurfaceRules.isBiome(Biomes.DESERT);
        SurfaceRules.RuleSource stony = SurfaceRules.sequence(
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.STONY_PEAKS), 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				SurfaceRules.noiseCondition(Noises.CALCITE, -0.0125, 0.0125), 
        				CALCITE
        			), 
        			STONE
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.STONY_SHORE), 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				SurfaceRules.noiseCondition(Noises.GRAVEL, -0.05, 0.05), 
        				gravel
        			), 
        			STONE
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.WINDSWEPT_HILLS), 
        		SurfaceRules.ifTrue(
//        			PresetSurfaceRuleData.surfaceNoiseAbove(1.0), 
        			UTFSurfaceConditions.sediment(5.0F),
        			STONE
        		)
        	), 
        	SurfaceRules.ifTrue(
        		sandyBeach, 
        		sand
        	), 
        	SurfaceRules.ifTrue(
        		desert, 
        		sand
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.DRIPSTONE_CAVES), 
        		STONE
        	)
        );
        SurfaceRules.RuleSource snowUnderFloor = SurfaceRules.ifTrue(
        	SurfaceRules.noiseCondition(Noises.POWDER_SNOW, 0.45, 0.58), 
        	SurfaceRules.ifTrue(
        		yOnSurface,
        		POWDER_SNOW
        	)
        );
        SurfaceRules.RuleSource snowOnFloor = SurfaceRules.ifTrue(
        	SurfaceRules.noiseCondition(Noises.POWDER_SNOW, 0.35, 0.6), 
        	SurfaceRules.ifTrue(
        		yOnSurface, 
        		POWDER_SNOW
        	)
        );
        SurfaceRules.RuleSource underFloor = SurfaceRules.sequence(
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.FROZEN_PEAKS), 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				steep, 
        				PACKED_ICE
        			), 
        			SurfaceRules.ifTrue(
        				SurfaceRules.noiseCondition(Noises.PACKED_ICE, -0.5, 0.2), 
        				PACKED_ICE
        			), 
        			SurfaceRules.ifTrue(
        				SurfaceRules.noiseCondition(Noises.ICE, -0.0625, 0.025), 
        				ICE
        			), 
        			SurfaceRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.SNOWY_SLOPES), 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				steep, 
        				STONE
        			), 
        			snowUnderFloor, 
        			SurfaceRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.JAGGED_PEAKS), 
        		STONE
        	),
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.GROVE), 
        		SurfaceRules.sequence(
        			snowUnderFloor, 
        			DIRT
        		)
        	), 
        	stony, 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.WINDSWEPT_SAVANNA), 
        		SurfaceRules.ifTrue(
        			PresetSurfaceRuleData.surfaceNoiseAbove(1.75), 
        			STONE
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.WINDSWEPT_GRAVELLY_HILLS), 
        		SurfaceRules.sequence(
        			windswept,
        			DIRT
        			
//        			SurfaceRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(2.0), 
//        				gravel
//        			), 
//        			SurfaceRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(1.0), 
//        				STONE
//        			), 
//        			SurfaceRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(-1.0), 
//        				DIRT
//        			), 
//        			gravel
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.MANGROVE_SWAMP), 
        		MUD
        	),
        	erodedDirt,
        	DIRT
        );
        SurfaceRules.RuleSource onFloor = SurfaceRules.sequence(
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.FROZEN_PEAKS), 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				steep, 
        				PACKED_ICE
        			), 
        			SurfaceRules.ifTrue(
        				SurfaceRules.noiseCondition(Noises.PACKED_ICE, 0.0, 0.2), 
        				PACKED_ICE
        			), 
        			SurfaceRules.ifTrue(
        				SurfaceRules.noiseCondition(Noises.ICE, 0.0, 0.025), 
        				ICE
        			), 
        			SurfaceRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.SNOWY_SLOPES), 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				steep, 
        				STONE
        			), 
        			snowOnFloor, 
        			SurfaceRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.JAGGED_PEAKS), 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				steep, 
        				DEEPSLATE
        			), 
        			SurfaceRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.GROVE), 
        		SurfaceRules.sequence(
        			snowOnFloor, 
        			SurfaceRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	),
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.FOREST, Biomes.DARK_FOREST),
        		makeForestRule(noise)
        	),
        	stony, 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.WINDSWEPT_SAVANNA), 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				PresetSurfaceRuleData.surfaceNoiseAbove(1.75), 
        				STONE
        			), 
        			SurfaceRules.ifTrue(
        				PresetSurfaceRuleData.surfaceNoiseAbove(-0.5), 
        				COARSE_DIRT
        			)
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.WINDSWEPT_GRAVELLY_HILLS), 
        		SurfaceRules.sequence(
            		windswept,
            		GRASS_BLOCK
//        			SurfaceRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(2.0), 
//        				gravel
//        			), 
//        			SurfaceRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(1.0),
//        				STONE
//        			),
//        			SurfaceRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(-1.0),
//        				grass
//        			),
//        			gravel
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				PresetSurfaceRuleData.surfaceNoiseAbove(1.75), 
        				COARSE_DIRT
        			), 
        			SurfaceRules.ifTrue(
        				PresetSurfaceRuleData.surfaceNoiseAbove(-0.95), 
        				PODZOL
        			)
        		)
        	),
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.ICE_SPIKES), 
        		SurfaceRules.ifTrue(
        			yOnSurface,
        			SNOW_BLOCK
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.MANGROVE_SWAMP),
        		MUD
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.isBiome(Biomes.MUSHROOM_FIELDS),
        		MYCELIUM
        	), 
        	grass
        );
        SurfaceRules.ConditionSource surfaceNoise1 = SurfaceRules.noiseCondition(Noises.SURFACE, -0.909, -0.5454);
        SurfaceRules.ConditionSource surfaceNoise2 = SurfaceRules.noiseCondition(Noises.SURFACE, -0.1818, 0.1818);
        SurfaceRules.ConditionSource surfaceNoise3 = SurfaceRules.noiseCondition(Noises.SURFACE, 0.5454, 0.909);
        SurfaceRules.RuleSource surface = SurfaceRules.sequence(
        	preset.landforms().volcanicSurface ? makeVolcanoRule(noise) : SurfaceRules.ifTrue(NEVER, STONE),
        	// the gravel beaches at the foot of sea cliffs
        	SurfaceRules.ifTrue(
        		UTFSurfaceConditions.terrain(TerrainType.SHINGLE_BEACH),
        		SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, gravel)
        	),
        	makeLandformSurfaceRule(noise, yOnSurface, sand, gravel),
        	// river and lake beds and banks that suit their setting
        	miscellaneousSettings.riverBanks ? makeRiverBankRule(scaling, noise, yOnSurface, sand, gravel) : SurfaceRules.ifTrue(NEVER, STONE),
        	SurfaceRules.ifTrue(
        		y4BelowSurface, 
        		SurfaceRules.ifTrue(
        			SurfaceRules.not(UTFSurfaceConditions.terrain(TerrainType.DUNES)),
        			makeDesertRule(scaling, noise)
        		)
        	),
        	SurfaceRules.ifTrue(
        		SurfaceRules.ON_FLOOR, 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				SurfaceRules.isBiome(Biomes.WOODED_BADLANDS),
        				SurfaceRules.ifTrue(
	        				SurfaceRules.not(erodedRock),
	        				SurfaceRules.ifTrue(
	        					below97, 
	        					SurfaceRules.sequence(
	        						SurfaceRules.ifTrue(
	        							surfaceNoise1, 
	        							COARSE_DIRT
			        				), 
	        						SurfaceRules.ifTrue(
	        							surfaceNoise2, 
	        							COARSE_DIRT
			        				), 
	        						SurfaceRules.ifTrue(
	        							surfaceNoise3, 
	        							COARSE_DIRT
			        				), 
	        						grass
			        			)
	        				)
	        			)
        			),
        			SurfaceRules.ifTrue(
        				SurfaceRules.isBiome(Biomes.SWAMP), 
        				SurfaceRules.ifTrue(
        					below62, 
        					SurfaceRules.ifTrue(
        						SurfaceRules.not(below63), 
        						SurfaceRules.ifTrue(
        							SurfaceRules.noiseCondition(Noises.SWAMP, 0.0), 
        							WATER
        						)
        					)
        				)
        			), 
        			SurfaceRules.ifTrue(
        				SurfaceRules.isBiome(Biomes.MANGROVE_SWAMP), 
        				SurfaceRules.ifTrue(
        					below60, 
        					SurfaceRules.ifTrue(
        						SurfaceRules.not(below63), 
        						SurfaceRules.ifTrue(
        							SurfaceRules.noiseCondition(Noises.SWAMP, 0.0), 
        							WATER
        						)
        					)
        				)
        			)
        		)
        	), 
        	SurfaceRules.ifTrue(
        		badlands, 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				SurfaceRules.ON_FLOOR, 
        				SurfaceRules.sequence(
        					SurfaceRules.ifTrue(
        						below256,
        						ORANGE_TERRACOTTA
        					), 
        					SurfaceRules.ifTrue(
        						above74, 
        						SurfaceRules.sequence(
        							SurfaceRules.ifTrue(
        								surfaceNoise1, 
        								TERRACOTTA
        							), 
        							SurfaceRules.ifTrue(
        								surfaceNoise2, 
        								TERRACOTTA
        							), 
        							SurfaceRules.ifTrue(
        								surfaceNoise3, 
        								TERRACOTTA
        							), 
        							SurfaceRules.bandlands()
        						)
        					), 
        					SurfaceRules.ifTrue(
        						y1BelowSurface,
        						SurfaceRules.sequence(
        							SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, RED_SANDSTONE), 
        							RED_SAND
        						)
        					), 
        					SurfaceRules.ifTrue(
        						SurfaceRules.not(hole), 
        						ORANGE_TERRACOTTA
        					), 
        					SurfaceRules.ifTrue(
        						y6BelowSurface, 
        						WHITE_TERRACOTTA
        					), 
        					gravel
        				)
        			), 
        			SurfaceRules.ifTrue(
        				above63, 
        				SurfaceRules.sequence(
        					SurfaceRules.ifTrue(
        						below63, 
        						SurfaceRules.ifTrue(
        							SurfaceRules.not(above74), 
        							ORANGE_TERRACOTTA
        						)
        					), 
        					SurfaceRules.bandlands()
        				)
        			), 
        			SurfaceRules.ifTrue(
        				SurfaceRules.UNDER_FLOOR, 
        				SurfaceRules.ifTrue(
        					y6BelowSurface, 
        					WHITE_TERRACOTTA
        				)
        			)
        		)
        	), 
        	SurfaceRules.ifTrue(
        		SurfaceRules.ON_FLOOR, 
        		SurfaceRules.ifTrue(
        			y1BelowSurface, 
        			SurfaceRules.sequence(
        				SurfaceRules.ifTrue(
        					frozenOcean, 
        					SurfaceRules.ifTrue(
        						hole, 
        						SurfaceRules.sequence(
        							SurfaceRules.ifTrue(
        								yOnSurface, 
        								AIR
        							), 
        							SurfaceRules.ifTrue(
        								SurfaceRules.temperature(), 
        								ICE
        							), 
        							WATER
        						)
        					)
        				),
        				SurfaceRules.ifTrue(
        					SurfaceRules.not(erodedRock),
        					onFloor
        				)
        			)
        		)
        	), 
        	SurfaceRules.ifTrue(
        		y6BelowSurface, 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				SurfaceRules.ON_FLOOR, 
        				SurfaceRules.ifTrue(
        					frozenOcean, 
        					SurfaceRules.ifTrue(
        						hole, 
        						WATER
        					)
        				)
        			),
        			SurfaceRules.ifTrue(
        				SurfaceRules.UNDER_FLOOR,
        				SurfaceRules.ifTrue(
        					SurfaceRules.not(erodedRock),
        					underFloor
        				)
        			),
        			SurfaceRules.ifTrue(
        				sandyBeach,
        				SurfaceRules.ifTrue(
        					SurfaceRules.DEEP_UNDER_FLOOR, 
        					SANDSTONE
        				)
        			),
        			SurfaceRules.ifTrue(
        				desert, 
        				SurfaceRules.ifTrue(
        					SurfaceRules.VERY_DEEP_UNDER_FLOOR, 
        					SANDSTONE
        				)
        			)
        		)
        	),
        	SurfaceRules.ifTrue(
        		SurfaceRules.ON_FLOOR, 
        		SurfaceRules.sequence(
        			SurfaceRules.ifTrue(
        				SurfaceRules.isBiome(Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS), 
        				STONE
        			), 
        			SurfaceRules.ifTrue(
        				SurfaceRules.isBiome(Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN),
        				sand
        			),
        			SurfaceRules.ifTrue(
        				SurfaceRules.not(erodedRock),
        				gravel
        			)
        		)
        	)
        );
        List<SurfaceRules.RuleSource> list = Lists.newArrayList(
        	SurfaceRules.ifTrue(
        		SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)),
        		BEDROCK
        	),
        	// sediment on the sea floor: before the preliminary surface check, which doesn't hold on the deep sea floor
        	// (but not where the surface has floors of its own: reefs, deltas, lagoons, and river and lake beds)
        	preset.oceans().sediment ? SurfaceRules.ifTrue(UTFSurfaceConditions.nearGround(3), SurfaceRules.ifTrue(SurfaceRules.not(UTFSurfaceConditions.any(UTFSurfaceConditions.terrain(TerrainType.CORAL_REEF, TerrainType.DELTA, TerrainType.WETLAND, TerrainType.LAGOON, TerrainType.BARRIER_ISLAND, TerrainType.SAND_BAR, TerrainType.LAKE, TerrainType.RIVER), UTFSurfaceConditions.riverSide(1.0F))), makeSeaFloorRule(properties.seaLevel, noise, yOnSurface, sand, gravel))) : SurfaceRules.ifTrue(NEVER, STONE),
        	SurfaceRules.ifTrue(
        		SurfaceRules.abovePreliminarySurface(),
        		// tors and skerries are bare rock: the rock layers, or plain stone without them
        		SurfaceRules.ifTrue(SurfaceRules.not(UTFSurfaceConditions.terrain(TerrainType.TOR, TerrainType.SKERRY)), surface)
        	)
        );
        // vanilla's gradual change from stone to deepslate; with rock layers the deepslate is layered too
        SurfaceRules.ConditionSource deepslateLevel = SurfaceRules.verticalGradient("deepslate", VerticalAnchor.absolute(0), VerticalAnchor.absolute(DEEPSLATE_TOP));
        list.add(SurfaceRules.ifTrue(deepslateLevel, miscellaneousSettings.strataDecorator ? makeDeepStrataRule(noise) : DEEPSLATE));
        if (miscellaneousSettings.strataDecorator) {
        	// whatever stone the surface left
        	SurfaceRules.RuleSource strata = makeStrataRule(miscellaneousSettings, noise);
        	if (miscellaneousSettings.plainStoneErosion) {
        		// the rock laid bare on steep slopes stays plain stone; the layers only show deeper in
        		strata = SurfaceRules.sequence(
        			SurfaceRules.ifTrue(erodedRock, SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(STRATA_PLAIN_DEPTH, false, CaveSurface.FLOOR), STONE)),
        			strata
        		);
        	}
        	list.add(strata);
        }

        SurfaceRules.RuleSource rules = SurfaceRules.sequence(list.toArray(SurfaceRules.RuleSource[]::new));
        return rules;
    }
    
    private static SurfaceRules.RuleSource makeDesertRule(Scaling scaling, HolderGetter<Noise> noise) {
    	Holder<Noise> variance = noise.getOrThrow(PresetSurfaceNoise.DESERT);
    	float min = scaling.ground(10);
    	float level = scaling.ground(40);
    	
    	SurfaceRules.ConditionSource aboveLevel = UTFSurfaceConditions.height(level, variance);
        SurfaceRules.ConditionSource desert = SurfaceRules.isBiome(Biomes.DESERT);
    	return SurfaceRules.ifTrue(
    		UTFSurfaceConditions.height(min),
    		SurfaceRules.sequence(
    			SurfaceRules.ifTrue(
    				UTFSurfaceConditions.steepness(0.15F), 
			        SurfaceRules.ifTrue(
			        	desert, 
			        	SurfaceRules.ifTrue(
			        		aboveLevel, 
			        		SurfaceRules.sequence(
								SurfaceRules.ifTrue(UTFSurfaceConditions.steepness(0.975F), TERRACOTTA),
								SurfaceRules.ifTrue(UTFSurfaceConditions.steepness(0.85F), BROWN_TERRACOTTA),
								SurfaceRules.ifTrue(UTFSurfaceConditions.steepness(0.75F), ORANGE_TERRACOTTA),
								SurfaceRules.ifTrue(UTFSurfaceConditions.steepness(0.65F), TERRACOTTA), 
								SMOOTH_SANDSTONE
							)
						)
    	            )
    			),
        		SurfaceRules.ifTrue(
        			UTFSurfaceConditions.steepness(0.3F), 
        			SurfaceRules.ifTrue(
        				desert, 
        				SMOOTH_SANDSTONE
        			)
            	)
    		)
    	);
    }
    
    // salt crust on salt flats, sand on barrier islands, mud in deltas, and gravel and rubble where rock was broken up
    // and carried down: alluvial fans, moraines and cirques
    private static SurfaceRules.RuleSource makeLandformSurfaceRule(HolderGetter<Noise> noise, SurfaceRules.ConditionSource dry, SurfaceRules.RuleSource sand, SurfaceRules.RuleSource gravel) {
    	Holder<Noise> debris = noise.getOrThrow(PresetSurfaceNoise.GLACIAL_DEBRIS);
    	SurfaceRules.ConditionSource topLayers = SurfaceRules.stoneDepthCheck(2, false, CaveSurface.FLOOR);
    	return SurfaceRules.sequence(
    		SurfaceRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.SALT_FLAT),
    			SurfaceRules.sequence(
    				SurfaceRules.ifTrue(
    					SurfaceRules.ON_FLOOR,
    					UTFSurfaceRules.noise(noise.getOrThrow(PresetSurfaceNoise.SALT_FLAT), List.of(Pair.of(SALT_CRACK, WHITE_TERRACOTTA), Pair.of(-1.0F, CALCITE)))
    				),
    				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, sand)
    			)
    		),
    		SurfaceRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.BARRIER_ISLAND, TerrainType.LAGOON, TerrainType.SAND_BAR, TerrainType.SAND_WAVES),
    			SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, sand)
    		),
    		SurfaceRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.CORAL_REEF),
    			SurfaceRules.sequence(
    				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, UTFSurfaceRules.noise(noise.getOrThrow(PresetSurfaceNoise.CORAL), List.of(Pair.of(0.82F, FIRE_CORAL), Pair.of(0.66F, BRAIN_CORAL), Pair.of(0.5F, TUBE_CORAL), Pair.of(0.34F, HORN_CORAL), Pair.of(0.2F, BUBBLE_CORAL), Pair.of(-1.0F, sand)))),
    				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, sand)
    			)
    		),
    		SurfaceRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.DELTA),
    			SurfaceRules.ifTrue(
    				topLayers,
    				SurfaceRules.sequence(
    					SurfaceRules.ifTrue(SurfaceRules.not(dry), UTFSurfaceRules.noise(debris, List.of(Pair.of(0.65F, CLAY), Pair.of(0.4F, MUD), Pair.of(-1.0F, sand)))),
    					UTFSurfaceRules.noise(debris, List.of(Pair.of(0.62F, MUD), Pair.of(0.54F, sand)))
    				)
    			)
    		),
    		SurfaceRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.ALLUVIAL_FAN),
    			SurfaceRules.ifTrue(
    				topLayers,
    				UTFSurfaceRules.noise(debris, List.of(Pair.of(0.56F, gravel), Pair.of(0.44F, COARSE_DIRT)))
    			)
    		),
    		SurfaceRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.MORAINE),
    			SurfaceRules.ifTrue(
    				SurfaceRules.ON_FLOOR,
    				UTFSurfaceRules.noise(debris, List.of(Pair.of(0.74F, MOSSY_COBBLESTONE), Pair.of(0.64F, COBBLESTONE), Pair.of(0.48F, gravel), Pair.of(0.36F, COARSE_DIRT)))
    			)
    		),
    		SurfaceRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.CIRQUE),
    			SurfaceRules.ifTrue(
    				SurfaceRules.ON_FLOOR,
    				UTFSurfaceRules.noise(debris, List.of(Pair.of(0.5F, gravel), Pair.of(0.32F, STONE)))
    			)
    		)
    	);
    }

    // sand in the shallows; sand, gravel and clay further out; bare rock only on the steepest slopes, like the walls of
    // canyons and trenches
    private static SurfaceRules.RuleSource makeSeaFloorRule(int seaLevel, HolderGetter<Noise> noise, SurfaceRules.ConditionSource dry, SurfaceRules.RuleSource sand, SurfaceRules.RuleSource gravel) {
    	Holder<Noise> patches = noise.getOrThrow(PresetSurfaceNoise.RIVER_BED);
    	SurfaceRules.ConditionSource underSea = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(seaLevel - 1), 0));
    	SurfaceRules.ConditionSource shallow = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(seaLevel - SHALLOW_SEA), 0);
    	SurfaceRules.ConditionSource middle = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(seaLevel - MIDDLE_SEA), 0);
    	return SurfaceRules.ifTrue(
    		SurfaceRules.not(dry),
    		SurfaceRules.ifTrue(
    			underSea,
    			SurfaceRules.ifTrue(
    				SurfaceRules.not(UTFSurfaceConditions.steepness(0.85F)),
    				SurfaceRules.ifTrue(
    					SurfaceRules.stoneDepthCheck(1, false, CaveSurface.FLOOR),
    					SurfaceRules.sequence(
    						SurfaceRules.ifTrue(shallow, UTFSurfaceRules.noise(patches, List.of(Pair.of(0.74F, gravel), Pair.of(-1.0F, sand)))),
    						SurfaceRules.ifTrue(middle, UTFSurfaceRules.noise(patches, List.of(Pair.of(0.7F, CLAY), Pair.of(0.46F, gravel), Pair.of(-1.0F, sand)))),
    						UTFSurfaceRules.noise(patches, List.of(Pair.of(0.58F, CLAY), Pair.of(0.32F, gravel), Pair.of(-1.0F, sand)))
    					)
    				)
    			)
    		)
    	);
    }

    // beds and banks of gravel and cobbles in the mountains; sand, with patches of gravel and clay, in the lowlands; and
    // mud and clay in wetlands and deltas. Steep banks, like the sides of gorges, keep their rock
    private static SurfaceRules.RuleSource makeRiverBankRule(Scaling scaling, HolderGetter<Noise> noise, SurfaceRules.ConditionSource dry, SurfaceRules.RuleSource sand, SurfaceRules.RuleSource gravel) {
    	Holder<Noise> bed = noise.getOrThrow(PresetSurfaceNoise.RIVER_BED);
    	SurfaceRules.ConditionSource underwater = SurfaceRules.not(dry);
    	SurfaceRules.ConditionSource wet = UTFSurfaceConditions.any(UTFSurfaceConditions.terrain(TerrainType.WETLAND, TerrainType.DELTA), SurfaceRules.isBiome(Biomes.SWAMP, Biomes.MANGROVE_SWAMP));
    	SurfaceRules.ConditionSource high = UTFSurfaceConditions.height(scaling.ground(MOUNTAIN_RIVER_HEIGHT));
    	SurfaceRules.RuleSource wetBanks = SurfaceRules.sequence(
    		SurfaceRules.ifTrue(underwater, UTFSurfaceRules.noise(bed, List.of(Pair.of(0.62F, CLAY), Pair.of(-1.0F, MUD)))),
    		UTFSurfaceRules.noise(bed, List.of(Pair.of(0.55F, MUD)))
    	);
    	SurfaceRules.RuleSource mountainBanks = SurfaceRules.sequence(
    		SurfaceRules.ifTrue(underwater, UTFSurfaceRules.noise(bed, List.of(Pair.of(0.7F, MOSSY_COBBLESTONE), Pair.of(0.6F, COBBLESTONE), Pair.of(-1.0F, gravel)))),
    		SurfaceRules.ifTrue(UTFSurfaceConditions.riverSide(0.9F), UTFSurfaceRules.noise(bed, List.of(Pair.of(0.5F, gravel), Pair.of(0.4F, COARSE_DIRT))))
    	);
    	SurfaceRules.RuleSource lowlandBanks = SurfaceRules.sequence(
    		SurfaceRules.ifTrue(underwater, UTFSurfaceRules.noise(bed, List.of(Pair.of(0.7F, CLAY), Pair.of(0.58F, gravel), Pair.of(-1.0F, sand)))),
    		SurfaceRules.ifTrue(UTFSurfaceConditions.riverSide(0.85F), UTFSurfaceRules.noise(bed, List.of(Pair.of(0.45F, sand))))
    	);
    	return SurfaceRules.ifTrue(
    		SurfaceRules.not(UTFSurfaceConditions.steepness(0.45F)),
    		SurfaceRules.ifTrue(
    			SurfaceRules.stoneDepthCheck(2, false, CaveSurface.FLOOR),
    			SurfaceRules.sequence(
    				// the whole of a wetland, not just along its river
    				SurfaceRules.ifTrue(wet, SurfaceRules.ifTrue(UTFSurfaceConditions.any(UTFSurfaceConditions.terrain(TerrainType.WETLAND, TerrainType.DELTA), UTFSurfaceConditions.riverSide(1.0F)), wetBanks)),
    				SurfaceRules.ifTrue(
    					UTFSurfaceConditions.riverSide(1.0F),
    					SurfaceRules.ifTrue(
    						SurfaceRules.not(wet),
    						SurfaceRules.sequence(
    							SurfaceRules.ifTrue(high, mountainBanks),
    							SurfaceRules.ifTrue(SurfaceRules.not(high), lowlandBanks)
    						)
    					)
    				)
    			)
    		)
    	);
    }

    // lava over magma in the crater; basalt, blackstone and tuff all over the cone; patches of old lava flows on the
    // land around it, with the usual surface in between
    private static SurfaceRules.RuleSource makeVolcanoRule(HolderGetter<Noise> noise) {
    	SurfaceRules.RuleSource volcanicRock = UTFSurfaceRules.noise(
    		noise.getOrThrow(PresetSurfaceNoise.VOLCANIC_ROCK),
    		List.of(
    			Pair.of(0.0F, BASALT),
    			Pair.of(0.4F, BLACKSTONE),
    			Pair.of(0.62F, TUFF),
    			Pair.of(0.8F, SMOOTH_BASALT)
    		)
    	);
    	return SurfaceRules.sequence(
    		SurfaceRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.VOLCANO_PIPE),
    			SurfaceRules.sequence(
    				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, LAVA),
    				MAGMA_BLOCK
    			)
    		),
    		SurfaceRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.VOLCANO, TerrainType.VOLCANIC_ISLAND),
    			volcanicRock
    		),
    		SurfaceRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.VOLCANIC_LOWLANDS),
    			SurfaceRules.sequence(
    				SurfaceRules.ifTrue(UTFSurfaceConditions.steepness(0.15F), volcanicRock),
    				SurfaceRules.ifTrue(
    					SurfaceRules.ON_FLOOR,
    					UTFSurfaceRules.noise(
    						noise.getOrThrow(PresetSurfaceNoise.LAVA_FIELDS),
    						List.of(
    							Pair.of(0.6F, SMOOTH_BASALT),
    							Pair.of(0.7F, TUFF)
    						)
    					)
    				)
    			)
    		)
    	);
    }

    private static SurfaceRules.RuleSource makeForestRule(HolderGetter<Noise> noise) {
    	return SurfaceRules.ifTrue(
    		SurfaceRules.ON_FLOOR, 
    		UTFSurfaceRules.noise(
    			noise.getOrThrow(PresetSurfaceNoise.FOREST), 
    			List.of(
    				Pair.of(0.65F, PODZOL),
    				Pair.of(0.725F, DIRT)
    			)
    		)	
    	);
    }
    
	private static SurfaceRules.RuleSource makeStrataRule(MiscellaneousSettings miscellaneousSettings, HolderGetter<Noise> noise) {
		return new StrataRule(
			STRATA_CACHE_ID,
			noise.getOrThrow(PresetStrataNoise.STRATA_SELECTOR),
			noise.getOrThrow(PresetStrataNoise.STRATA_OFFSET),
			noise.getOrThrow(PresetStrataNoise.STRATA_THICKNESS),
			Blocks.STONE,
			STRATA_STONE_SHARE,
			miscellaneousSettings.rockTag(),
			UTFBlockTags.STRATA_EXCLUDED,
			STRATA_VARIANTS,
			STRATA_MIN_THICKNESS,
			STRATA_MAX_THICKNESS
		);
	}

	// deepslate layered with tuff and whatever else ores turn into deepslate ores in
	private static SurfaceRules.RuleSource makeDeepStrataRule(HolderGetter<Noise> noise) {
		return new StrataRule(
			DEEP_STRATA_CACHE_ID,
			noise.getOrThrow(PresetStrataNoise.STRATA_SELECTOR),
			noise.getOrThrow(PresetStrataNoise.STRATA_OFFSET),
			noise.getOrThrow(PresetStrataNoise.STRATA_THICKNESS),
			Blocks.DEEPSLATE,
			STRATA_DEEPSLATE_SHARE,
			UTFBlockTags.DEEP_ROCK,
			UTFBlockTags.STRATA_EXCLUDED,
			STRATA_VARIANTS,
			STRATA_MIN_THICKNESS,
			STRATA_MAX_THICKNESS
		);
	}

    // high, steep ground loses its soil: coarse dirt on the steeper slopes, loose gravel (scree) on the gentler ones
    private static SurfaceRules.RuleSource makeErodedDirtRule(HolderGetter<Noise> noise, SurfaceSettings.Erosion settings) {
    	SurfaceRules.ConditionSource high = UTFSurfaceConditions.height(noise.getOrThrow(PresetSurfaceNoise.ERODED_DIRT), noise.getOrThrow(PresetSurfaceNoise.HEIGHT_VARIANCE));
    	return SurfaceRules.ifTrue(
    		high,
    		SurfaceRules.sequence(
    			SurfaceRules.ifTrue(
    				UTFSurfaceConditions.steepness(settings.dirtSteepness, noise.getOrThrow(PresetSurfaceNoise.STEEPNESS_VARIANCE)),
    				COARSE_DIRT
    			),
    			SurfaceRules.ifTrue(
    				UTFSurfaceConditions.steepness(settings.screeSteepness, noise.getOrThrow(PresetSurfaceNoise.STEEPNESS_VARIANCE)),
    				SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, STONE), GRAVEL)
    			)
    		)
    	);
    }
	
    // both conditions hold: neither fails
    private static SurfaceRules.ConditionSource and(SurfaceRules.ConditionSource a, SurfaceRules.ConditionSource b) {
    	return SurfaceRules.not(UTFSurfaceConditions.any(SurfaceRules.not(a), SurfaceRules.not(b)));
    }

    private static SurfaceRules.ConditionSource surfaceNoiseAbove(double target) {
        return SurfaceRules.noiseCondition(Noises.SURFACE, target / 8.25D, Double.MAX_VALUE);
    }
}