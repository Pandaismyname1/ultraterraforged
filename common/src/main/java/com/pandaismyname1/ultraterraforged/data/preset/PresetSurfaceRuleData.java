package com.pandaismyname1.ultraterraforged.data.preset;

import net.minecraft.world.level.biome.Biome;
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
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.OreVeinRule;
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
    private static final MaterialRule AIR = PresetSurfaceRuleData.makeStateRule(Blocks.AIR);
    private static final MaterialRule BEDROCK = PresetSurfaceRuleData.makeStateRule(Blocks.BEDROCK);
    private static final MaterialRule WHITE_TERRACOTTA = PresetSurfaceRuleData.makeStateRule(Blocks.DYED_TERRACOTTA.white());
    private static final MaterialRule ORANGE_TERRACOTTA = PresetSurfaceRuleData.makeStateRule(Blocks.DYED_TERRACOTTA.orange());
    private static final MaterialRule BROWN_TERRACOTTA = PresetSurfaceRuleData.makeStateRule(Blocks.DYED_TERRACOTTA.brown());
    private static final MaterialRule TERRACOTTA = PresetSurfaceRuleData.makeStateRule(Blocks.TERRACOTTA);
    private static final MaterialRule RED_SAND = PresetSurfaceRuleData.makeStateRule(Blocks.RED_SAND);
    private static final MaterialRule RED_SANDSTONE = PresetSurfaceRuleData.makeStateRule(Blocks.RED_SANDSTONE);
    private static final MaterialRule STONE = PresetSurfaceRuleData.makeStateRule(Blocks.STONE);
    private static final MaterialRule DEEPSLATE = PresetSurfaceRuleData.makeStateRule(Blocks.DEEPSLATE);
    private static final MaterialRule DIRT = PresetSurfaceRuleData.makeStateRule(Blocks.DIRT);
    private static final MaterialRule PODZOL = PresetSurfaceRuleData.makeStateRule(Blocks.PODZOL);
    private static final MaterialRule COARSE_DIRT = PresetSurfaceRuleData.makeStateRule(Blocks.COARSE_DIRT);
    private static final MaterialRule MYCELIUM = PresetSurfaceRuleData.makeStateRule(Blocks.MYCELIUM);
    private static final MaterialRule GRASS_BLOCK = PresetSurfaceRuleData.makeStateRule(Blocks.GRASS_BLOCK);
    private static final MaterialRule CALCITE = PresetSurfaceRuleData.makeStateRule(Blocks.CALCITE);
    private static final MaterialRule GRAVEL = PresetSurfaceRuleData.makeStateRule(Blocks.GRAVEL);
    private static final MaterialRule SAND = PresetSurfaceRuleData.makeStateRule(Blocks.SAND);
    private static final MaterialRule SANDSTONE = PresetSurfaceRuleData.makeStateRule(Blocks.SANDSTONE);
    private static final MaterialRule SMOOTH_SANDSTONE = PresetSurfaceRuleData.makeStateRule(Blocks.SMOOTH_SANDSTONE);
    private static final MaterialRule PACKED_ICE = PresetSurfaceRuleData.makeStateRule(Blocks.PACKED_ICE);
    private static final MaterialRule SNOW_BLOCK = PresetSurfaceRuleData.makeStateRule(Blocks.SNOW_BLOCK);
    private static final MaterialRule MUD = PresetSurfaceRuleData.makeStateRule(Blocks.MUD);
    private static final MaterialRule POWDER_SNOW = PresetSurfaceRuleData.makeStateRule(Blocks.POWDER_SNOW);
    private static final MaterialRule ICE = PresetSurfaceRuleData.makeStateRule(Blocks.ICE);
    private static final MaterialRule WATER = PresetSurfaceRuleData.makeStateRule(Blocks.WATER);
    private static final MaterialRule LAVA = PresetSurfaceRuleData.makeStateRule(Blocks.LAVA);
    private static final MaterialRule MAGMA_BLOCK = PresetSurfaceRuleData.makeStateRule(Blocks.MAGMA_BLOCK);
    private static final MaterialRule BASALT = PresetSurfaceRuleData.makeStateRule(Blocks.BASALT);
    private static final MaterialRule SMOOTH_BASALT = PresetSurfaceRuleData.makeStateRule(Blocks.SMOOTH_BASALT);
    private static final MaterialRule BLACKSTONE = PresetSurfaceRuleData.makeStateRule(Blocks.BLACKSTONE);
    private static final MaterialRule TUFF = PresetSurfaceRuleData.makeStateRule(Blocks.TUFF);
    private static final MaterialRule CLAY = PresetSurfaceRuleData.makeStateRule(Blocks.CLAY);
    private static final MaterialRule TUBE_CORAL = PresetSurfaceRuleData.makeStateRule(Blocks.TUBE_CORAL_BLOCK);
    private static final MaterialRule BRAIN_CORAL = PresetSurfaceRuleData.makeStateRule(Blocks.BRAIN_CORAL_BLOCK);
    private static final MaterialRule BUBBLE_CORAL = PresetSurfaceRuleData.makeStateRule(Blocks.BUBBLE_CORAL_BLOCK);
    private static final MaterialRule FIRE_CORAL = PresetSurfaceRuleData.makeStateRule(Blocks.FIRE_CORAL_BLOCK);
    private static final MaterialRule HORN_CORAL = PresetSurfaceRuleData.makeStateRule(Blocks.HORN_CORAL_BLOCK);
    private static final MaterialRule COBBLESTONE = PresetSurfaceRuleData.makeStateRule(Blocks.COBBLESTONE);
    private static final MaterialRule MOSSY_COBBLESTONE = PresetSurfaceRuleData.makeStateRule(Blocks.MOSSY_COBBLESTONE);
    private static final MaterialRule CINNABAR = PresetSurfaceRuleData.makeStateRule(Blocks.CINNABAR);
    private static final MaterialRule SULFUR = PresetSurfaceRuleData.makeStateRule(Blocks.SULFUR);
    // vanilla registers these as VanillaMaterialConditions; built here the same way
    private static final MaterialCondition ON_FLOOR = MaterialRules.stoneDepthCheck(0, false, CaveSurface.FLOOR);
    private static final MaterialCondition UNDER_FLOOR = MaterialRules.stoneDepthCheck(0, true, CaveSurface.FLOOR);
    private static final MaterialCondition DEEP_UNDER_FLOOR = MaterialRules.stoneDepthCheck(0, true, 6, CaveSurface.FLOOR);
    private static final MaterialCondition VERY_DEEP_UNDER_FLOOR = MaterialRules.stoneDepthCheck(0, true, 30, CaveSurface.FLOOR);
    private static final MaterialCondition ON_CEILING = MaterialRules.stoneDepthCheck(0, false, CaveSurface.CEILING);
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
    private static final MaterialCondition NEVER = MaterialRules.not(MaterialRules.yBlockCheck(VerticalAnchor.bottom(), 0));
    
    private static MaterialRule makeStateRule(Block block) {
        return MaterialRules.state(block.defaultBlockState());
    }
    
    public static MaterialRule overworld(HolderGetter<Biome> biomes, HolderGetter<DensityFunction> functions, Preset preset, HolderGetter<Noise> noise) {
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		Scaling scaling = Scaling.make(properties.terrainScaler(), properties.seaLevel);
    	MiscellaneousSettings miscellaneousSettings = preset.miscellaneous();
    	
    	SurfaceSettings surfaceSettings = preset.surface();
    	SurfaceSettings.Erosion erosion = surfaceSettings.erosion();

    	MaterialCondition y4BelowSurface = MaterialRules.stoneDepthCheck(3, false, CaveSurface.FLOOR);
        MaterialCondition below97 = MaterialRules.yBlockCheck(VerticalAnchor.absolute(97), 2);
        MaterialCondition below256 = MaterialRules.yBlockCheck(VerticalAnchor.absolute(256), 0);
        MaterialCondition above63 = MaterialRules.yStartCheck(VerticalAnchor.absolute(63), -1);
        MaterialCondition above74 = MaterialRules.yStartCheck(VerticalAnchor.absolute(74), 1);
        MaterialCondition below60 = MaterialRules.yBlockCheck(VerticalAnchor.absolute(60), 0);
        MaterialCondition below62 = MaterialRules.yBlockCheck(VerticalAnchor.absolute(62), 0);
        MaterialCondition below63 = MaterialRules.yBlockCheck(VerticalAnchor.absolute(63), 0);
        MaterialCondition y1BelowSurface = MaterialRules.waterBlockCheck(-1, 0);
        MaterialCondition yOnSurface = MaterialRules.waterBlockCheck(0, 0);
        MaterialCondition y6BelowSurface = MaterialRules.waterStartCheck(-6, -1);
        MaterialCondition hole = MaterialRules.hole();
        MaterialCondition frozenOcean = MaterialRules.isBiome(biomes, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN);
        MaterialCondition badlands = MaterialRules.isBiome(biomes, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
        MaterialCondition steep = MaterialRules.steep();
        MaterialCondition sulfurCaves = MaterialRules.isBiome(biomes, Biomes.SULFUR_CAVES);
        // vanilla's bands of cinnabar and sulfur through the sulfur caves
        MaterialRule sulfurCaveBands = MaterialRules.sequence(
        	MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.SULFUR_CAVE_GRADIENT, -0.4F, -0.1F), CINNABAR),
        	MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.SULFUR_CAVE_GRADIENT, 0.0, 0.4F), SULFUR),
        	MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.SULFUR_CAVE_GRADIENT, 0.4F), CINNABAR)
        );
        // steep slopes wear down to bare rock and coarse dirt, unless the preset turns erosion off
        // (and everything above the rock line, which the default presets set above the build limit)
        // (sand dunes are steep but loose, so they stay sand)
        MaterialCondition erodedRock = miscellaneousSettings.erosionDecorator ? and(
        	MaterialRules.not(UTFSurfaceConditions.terrain(TerrainType.DUNES)),
        	UTFSurfaceConditions.any(
        		UTFSurfaceConditions.steepness(erosion.rockSteepness, noise.getOrThrow(PresetSurfaceNoise.STEEPNESS_VARIANCE)),
        		UTFSurfaceConditions.height(noise.getOrThrow(PresetSurfaceNoise.ERODED_ROCK), noise.getOrThrow(PresetSurfaceNoise.HEIGHT_VARIANCE))
        	)
        ) : NEVER;
        MaterialRule erodedDirt = miscellaneousSettings.erosionDecorator ? makeErodedDirtRule(noise, erosion) : MaterialRules.ifTrue(NEVER, COARSE_DIRT);
        MaterialRule grass = MaterialRules.sequence(
        	erodedDirt,
        	MaterialRules.ifTrue(
        		yOnSurface,
        		GRASS_BLOCK
        	), 
        	DIRT
        );
        MaterialRule sand = MaterialRules.sequence(MaterialRules.ifTrue(ON_CEILING, SANDSTONE), SAND);
        MaterialRule gravel = MaterialRules.sequence(MaterialRules.ifTrue(ON_CEILING, STONE), GRAVEL);
        MaterialRule windswept = MaterialRules.sequence(
        	MaterialRules.ifTrue(
        		UTFSurfaceConditions.sediment(5.3F), 
        		STONE
            ),
        	MaterialRules.ifTrue(
        		UTFSurfaceConditions.sediment(2.4F), 
        		gravel
            ),
        	MaterialRules.ifTrue(
        		UTFSurfaceConditions.erosion(8.25F),
        		STONE
        	),
        	MaterialRules.ifTrue(
	        	UTFSurfaceConditions.erosion(5.5F),
	        	gravel
	        )
        );
        MaterialCondition sandyBeach = MaterialRules.isBiome(biomes, Biomes.WARM_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH);
        MaterialCondition desert = MaterialRules.isBiome(biomes, Biomes.DESERT);
        MaterialRule stony = MaterialRules.sequence(
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.STONY_PEAKS), 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				MaterialRules.noiseCondition2d(Noises.CALCITE, -0.0125, 0.0125), 
        				CALCITE
        			), 
        			STONE
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.STONY_SHORE), 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				MaterialRules.noiseCondition2d(Noises.GRAVEL, -0.05, 0.05), 
        				gravel
        			), 
        			STONE
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.WINDSWEPT_HILLS), 
        		MaterialRules.ifTrue(
//        			PresetSurfaceRuleData.surfaceNoiseAbove(1.0), 
        			UTFSurfaceConditions.sediment(5.0F),
        			STONE
        		)
        	), 
        	MaterialRules.ifTrue(
        		sandyBeach, 
        		sand
        	), 
        	MaterialRules.ifTrue(
        		desert, 
        		sand
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.DRIPSTONE_CAVES),
        		STONE
        	),
        	MaterialRules.ifTrue(
        		sulfurCaves,
        		MaterialRules.sequence(
        			sulfurCaveBands,
        			STONE
        		)
        	)
        );
        MaterialRule snowUnderFloor = MaterialRules.ifTrue(
        	MaterialRules.noiseCondition2d(Noises.POWDER_SNOW, 0.45, 0.58), 
        	MaterialRules.ifTrue(
        		yOnSurface,
        		POWDER_SNOW
        	)
        );
        MaterialRule snowOnFloor = MaterialRules.ifTrue(
        	MaterialRules.noiseCondition2d(Noises.POWDER_SNOW, 0.35, 0.6), 
        	MaterialRules.ifTrue(
        		yOnSurface, 
        		POWDER_SNOW
        	)
        );
        MaterialRule underFloor = MaterialRules.sequence(
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.FROZEN_PEAKS), 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				steep, 
        				PACKED_ICE
        			), 
        			MaterialRules.ifTrue(
        				MaterialRules.noiseCondition2d(Noises.PACKED_ICE, -0.5, 0.2), 
        				PACKED_ICE
        			), 
        			MaterialRules.ifTrue(
        				MaterialRules.noiseCondition2d(Noises.ICE, -0.0625, 0.025), 
        				ICE
        			), 
        			MaterialRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.SNOWY_SLOPES), 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				steep, 
        				STONE
        			), 
        			snowUnderFloor, 
        			MaterialRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.JAGGED_PEAKS), 
        		STONE
        	),
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.GROVE), 
        		MaterialRules.sequence(
        			snowUnderFloor, 
        			DIRT
        		)
        	), 
        	stony, 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.WINDSWEPT_SAVANNA), 
        		MaterialRules.ifTrue(
        			PresetSurfaceRuleData.surfaceNoiseAbove(1.75), 
        			STONE
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.WINDSWEPT_GRAVELLY_HILLS), 
        		MaterialRules.sequence(
        			windswept,
        			DIRT
        			
//        			MaterialRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(2.0), 
//        				gravel
//        			), 
//        			MaterialRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(1.0), 
//        				STONE
//        			), 
//        			MaterialRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(-1.0), 
//        				DIRT
//        			), 
//        			gravel
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.MANGROVE_SWAMP), 
        		MUD
        	),
        	erodedDirt,
        	DIRT
        );
        MaterialRule onFloor = MaterialRules.sequence(
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.FROZEN_PEAKS), 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				steep, 
        				PACKED_ICE
        			), 
        			MaterialRules.ifTrue(
        				MaterialRules.noiseCondition2d(Noises.PACKED_ICE, 0.0, 0.2), 
        				PACKED_ICE
        			), 
        			MaterialRules.ifTrue(
        				MaterialRules.noiseCondition2d(Noises.ICE, 0.0, 0.025), 
        				ICE
        			), 
        			MaterialRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.SNOWY_SLOPES), 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				steep, 
        				STONE
        			), 
        			snowOnFloor, 
        			MaterialRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.JAGGED_PEAKS), 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				steep, 
        				DEEPSLATE
        			), 
        			MaterialRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.GROVE), 
        		MaterialRules.sequence(
        			snowOnFloor, 
        			MaterialRules.ifTrue(
        				yOnSurface, 
        				SNOW_BLOCK
        			)
        		)
        	),
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.FOREST, Biomes.DARK_FOREST),
        		makeForestRule(noise)
        	),
        	stony, 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.WINDSWEPT_SAVANNA), 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				PresetSurfaceRuleData.surfaceNoiseAbove(1.75), 
        				STONE
        			), 
        			MaterialRules.ifTrue(
        				PresetSurfaceRuleData.surfaceNoiseAbove(-0.5), 
        				COARSE_DIRT
        			)
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.WINDSWEPT_GRAVELLY_HILLS), 
        		MaterialRules.sequence(
            		windswept,
            		GRASS_BLOCK
//        			MaterialRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(2.0), 
//        				gravel
//        			), 
//        			MaterialRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(1.0),
//        				STONE
//        			),
//        			MaterialRules.ifTrue(
//        				PresetSurfaceRuleData.surfaceNoiseAbove(-1.0),
//        				grass
//        			),
//        			gravel
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				PresetSurfaceRuleData.surfaceNoiseAbove(1.75), 
        				COARSE_DIRT
        			), 
        			MaterialRules.ifTrue(
        				PresetSurfaceRuleData.surfaceNoiseAbove(-0.95), 
        				PODZOL
        			)
        		)
        	),
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.ICE_SPIKES), 
        		MaterialRules.ifTrue(
        			yOnSurface,
        			SNOW_BLOCK
        		)
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.MANGROVE_SWAMP),
        		MUD
        	), 
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.MUSHROOM_FIELDS),
        		MYCELIUM
        	),
        	MaterialRules.ifTrue(
        		MaterialRules.isBiome(biomes, Biomes.DAPPLED_FOREST),
        		MaterialRules.ifTrue(
        			MaterialRules.noiseCondition2d(Noises.SMALL_PATCH, 1.2F),
        			COARSE_DIRT
        		)
        	),
        	grass
        );
        MaterialCondition surfaceNoise1 = MaterialRules.noiseCondition2d(Noises.SURFACE, -0.909, -0.5454);
        MaterialCondition surfaceNoise2 = MaterialRules.noiseCondition2d(Noises.SURFACE, -0.1818, 0.1818);
        MaterialCondition surfaceNoise3 = MaterialRules.noiseCondition2d(Noises.SURFACE, 0.5454, 0.909);
        MaterialRule surface = MaterialRules.sequence(
        	preset.landforms().volcanicSurface ? makeVolcanoRule(noise) : MaterialRules.ifTrue(NEVER, STONE),
        	// the gravel beaches at the foot of sea cliffs
        	MaterialRules.ifTrue(
        		UTFSurfaceConditions.terrain(TerrainType.SHINGLE_BEACH),
        		MaterialRules.ifTrue(UNDER_FLOOR, gravel)
        	),
        	makeLandformSurfaceRule(noise, yOnSurface, sand, gravel),
        	// river and lake beds and banks that suit their setting
        	miscellaneousSettings.riverBanks ? makeRiverBankRule(biomes, scaling, noise, yOnSurface, sand, gravel) : MaterialRules.ifTrue(NEVER, STONE),
        	MaterialRules.ifTrue(
        		y4BelowSurface, 
        		MaterialRules.ifTrue(
        			MaterialRules.not(UTFSurfaceConditions.terrain(TerrainType.DUNES)),
        			makeDesertRule(biomes, scaling, noise)
        		)
        	),
        	MaterialRules.ifTrue(
        		ON_FLOOR, 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				MaterialRules.isBiome(biomes, Biomes.WOODED_BADLANDS),
        				MaterialRules.ifTrue(
	        				MaterialRules.not(erodedRock),
	        				MaterialRules.ifTrue(
	        					below97, 
	        					MaterialRules.sequence(
	        						MaterialRules.ifTrue(
	        							surfaceNoise1, 
	        							COARSE_DIRT
			        				), 
	        						MaterialRules.ifTrue(
	        							surfaceNoise2, 
	        							COARSE_DIRT
			        				), 
	        						MaterialRules.ifTrue(
	        							surfaceNoise3, 
	        							COARSE_DIRT
			        				), 
	        						grass
			        			)
	        				)
	        			)
        			),
        			MaterialRules.ifTrue(
        				MaterialRules.isBiome(biomes, Biomes.SWAMP), 
        				MaterialRules.ifTrue(
        					below62, 
        					MaterialRules.ifTrue(
        						MaterialRules.not(below63), 
        						MaterialRules.ifTrue(
        							MaterialRules.noiseCondition2d(Noises.SWAMP, 0.0), 
        							WATER
        						)
        					)
        				)
        			), 
        			MaterialRules.ifTrue(
        				MaterialRules.isBiome(biomes, Biomes.MANGROVE_SWAMP), 
        				MaterialRules.ifTrue(
        					below60, 
        					MaterialRules.ifTrue(
        						MaterialRules.not(below63), 
        						MaterialRules.ifTrue(
        							MaterialRules.noiseCondition2d(Noises.SWAMP, 0.0), 
        							WATER
        						)
        					)
        				)
        			)
        		)
        	), 
        	MaterialRules.ifTrue(
        		badlands, 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				ON_FLOOR, 
        				MaterialRules.sequence(
        					MaterialRules.ifTrue(
        						below256,
        						ORANGE_TERRACOTTA
        					), 
        					MaterialRules.ifTrue(
        						above74, 
        						MaterialRules.sequence(
        							MaterialRules.ifTrue(
        								surfaceNoise1, 
        								TERRACOTTA
        							), 
        							MaterialRules.ifTrue(
        								surfaceNoise2, 
        								TERRACOTTA
        							), 
        							MaterialRules.ifTrue(
        								surfaceNoise3, 
        								TERRACOTTA
        							), 
        							MaterialRules.bandlands()
        						)
        					), 
        					MaterialRules.ifTrue(
        						y1BelowSurface,
        						MaterialRules.sequence(
        							MaterialRules.ifTrue(ON_CEILING, RED_SANDSTONE), 
        							RED_SAND
        						)
        					), 
        					MaterialRules.ifTrue(
        						MaterialRules.not(hole), 
        						ORANGE_TERRACOTTA
        					), 
        					MaterialRules.ifTrue(
        						y6BelowSurface, 
        						WHITE_TERRACOTTA
        					), 
        					gravel
        				)
        			), 
        			MaterialRules.ifTrue(
        				above63, 
        				MaterialRules.sequence(
        					MaterialRules.ifTrue(
        						below63, 
        						MaterialRules.ifTrue(
        							MaterialRules.not(above74), 
        							ORANGE_TERRACOTTA
        						)
        					), 
        					MaterialRules.bandlands()
        				)
        			), 
        			MaterialRules.ifTrue(
        				UNDER_FLOOR, 
        				MaterialRules.ifTrue(
        					y6BelowSurface, 
        					WHITE_TERRACOTTA
        				)
        			)
        		)
        	), 
        	MaterialRules.ifTrue(
        		ON_FLOOR, 
        		MaterialRules.ifTrue(
        			y1BelowSurface, 
        			MaterialRules.sequence(
        				MaterialRules.ifTrue(
        					frozenOcean, 
        					MaterialRules.ifTrue(
        						hole, 
        						MaterialRules.sequence(
        							MaterialRules.ifTrue(
        								yOnSurface, 
        								AIR
        							), 
        							MaterialRules.ifTrue(
        								MaterialRules.temperature(), 
        								ICE
        							), 
        							WATER
        						)
        					)
        				),
        				MaterialRules.ifTrue(
        					MaterialRules.not(erodedRock),
        					onFloor
        				)
        			)
        		)
        	), 
        	MaterialRules.ifTrue(
        		y6BelowSurface, 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				ON_FLOOR, 
        				MaterialRules.ifTrue(
        					frozenOcean, 
        					MaterialRules.ifTrue(
        						hole, 
        						WATER
        					)
        				)
        			),
        			MaterialRules.ifTrue(
        				UNDER_FLOOR,
        				MaterialRules.ifTrue(
        					MaterialRules.not(erodedRock),
        					underFloor
        				)
        			),
        			MaterialRules.ifTrue(
        				sandyBeach,
        				MaterialRules.ifTrue(
        					DEEP_UNDER_FLOOR, 
        					SANDSTONE
        				)
        			),
        			MaterialRules.ifTrue(
        				desert, 
        				MaterialRules.ifTrue(
        					VERY_DEEP_UNDER_FLOOR, 
        					SANDSTONE
        				)
        			)
        		)
        	),
        	MaterialRules.ifTrue(
        		ON_FLOOR, 
        		MaterialRules.sequence(
        			MaterialRules.ifTrue(
        				MaterialRules.isBiome(biomes, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS), 
        				STONE
        			), 
        			MaterialRules.ifTrue(
        				MaterialRules.isBiome(biomes, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN),
        				sand
        			),
        			MaterialRules.ifTrue(
        				MaterialRules.not(erodedRock),
        				gravel
        			)
        		)
        	)
        );
        List<MaterialRule> list = Lists.newArrayList(
        	MaterialRules.ifTrue(
        		MaterialRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)),
        		BEDROCK
        	)
        );
        // the large copper and iron ore veins: the noise router placed these before 26.3, now they're material rules,
        // right after the bedrock as in vanilla
        if (preset.caves().largeOreVeins) {
        	list.addAll(makeOreVeinRules(functions));
        }
        list.addAll(List.of(
        	// sediment on the sea floor: before the preliminary surface check, which doesn't hold on the deep sea floor
        	// (but not where the surface has floors of its own: reefs, deltas, lagoons, and river and lake beds)
        	preset.oceans().sediment ? MaterialRules.ifTrue(UTFSurfaceConditions.nearGround(3), MaterialRules.ifTrue(MaterialRules.not(UTFSurfaceConditions.any(UTFSurfaceConditions.terrain(TerrainType.CORAL_REEF, TerrainType.DELTA, TerrainType.WETLAND, TerrainType.LAGOON, TerrainType.BARRIER_ISLAND, TerrainType.SAND_BAR, TerrainType.LAKE, TerrainType.RIVER), UTFSurfaceConditions.riverSide(1.0F))), makeSeaFloorRule(properties.seaLevel, noise, yOnSurface, sand, gravel))) : MaterialRules.ifTrue(NEVER, STONE),
        	MaterialRules.ifTrue(
        		MaterialRules.abovePreliminarySurface(),
        		// tors and skerries are bare rock: the rock layers, or plain stone without them
        		MaterialRules.ifTrue(MaterialRules.not(UTFSurfaceConditions.terrain(TerrainType.TOR, TerrainType.SKERRY)), surface)
        	),
        	// vanilla's underground rule: the sulfur caves are banded all through, not just at the surface
        	MaterialRules.ifTrue(sulfurCaves, sulfurCaveBands)
        ));
        // vanilla's gradual change from stone to deepslate; with rock layers the deepslate is layered too
        MaterialCondition deepslateLevel = MaterialRules.verticalGradient("deepslate", VerticalAnchor.absolute(0), VerticalAnchor.absolute(DEEPSLATE_TOP));
        list.add(MaterialRules.ifTrue(deepslateLevel, miscellaneousSettings.strataDecorator ? makeDeepStrataRule(noise) : DEEPSLATE));
        if (miscellaneousSettings.strataDecorator) {
        	// whatever stone the surface left
        	MaterialRule strata = makeStrataRule(miscellaneousSettings, noise);
        	if (miscellaneousSettings.plainStoneErosion) {
        		// the rock laid bare on steep slopes stays plain stone; the layers only show deeper in
        		strata = MaterialRules.sequence(
        			MaterialRules.ifTrue(erodedRock, MaterialRules.ifTrue(MaterialRules.stoneDepthCheck(STRATA_PLAIN_DEPTH, false, CaveSurface.FLOOR), STONE)),
        			strata
        		);
        	}
        	list.add(strata);
        }

        MaterialRule rules = MaterialRules.sequence(list);
        return rules;
    }

    // as vanilla's OverworldMaterialRules.registerOreVeins
    private static List<MaterialRule> makeOreVeinRules(HolderGetter<DensityFunction> functions) {
    	DensityFunction richness = NoiseRouterData.getFunction(functions, NoiseRouterData.ORE_VEIN_RICHNESS);
    	DensityFunction gap = NoiseRouterData.getFunction(functions, NoiseRouterData.ORE_VEIN_GAP);
    	return List.of(
    		OreVeinRule.VeinType.COPPER.create(NoiseRouterData.getFunction(functions, NoiseRouterData.ORE_VEIN_COPPER_DENSITY), richness, gap),
    		OreVeinRule.VeinType.IRON.create(NoiseRouterData.getFunction(functions, NoiseRouterData.ORE_VEIN_IRON_DENSITY), richness, gap)
    	);
    }
    
    private static MaterialRule makeDesertRule(HolderGetter<Biome> biomes, Scaling scaling, HolderGetter<Noise> noise) {
    	Holder<Noise> variance = noise.getOrThrow(PresetSurfaceNoise.DESERT);
    	float min = scaling.ground(10);
    	float level = scaling.ground(40);
    	
    	MaterialCondition aboveLevel = UTFSurfaceConditions.height(level, variance);
        MaterialCondition desert = MaterialRules.isBiome(biomes, Biomes.DESERT);
    	return MaterialRules.ifTrue(
    		UTFSurfaceConditions.height(min),
    		MaterialRules.sequence(
    			MaterialRules.ifTrue(
    				UTFSurfaceConditions.steepness(0.15F), 
			        MaterialRules.ifTrue(
			        	desert, 
			        	MaterialRules.ifTrue(
			        		aboveLevel, 
			        		MaterialRules.sequence(
								MaterialRules.ifTrue(UTFSurfaceConditions.steepness(0.975F), TERRACOTTA),
								MaterialRules.ifTrue(UTFSurfaceConditions.steepness(0.85F), BROWN_TERRACOTTA),
								MaterialRules.ifTrue(UTFSurfaceConditions.steepness(0.75F), ORANGE_TERRACOTTA),
								MaterialRules.ifTrue(UTFSurfaceConditions.steepness(0.65F), TERRACOTTA), 
								SMOOTH_SANDSTONE
							)
						)
    	            )
    			),
        		MaterialRules.ifTrue(
        			UTFSurfaceConditions.steepness(0.3F), 
        			MaterialRules.ifTrue(
        				desert, 
        				SMOOTH_SANDSTONE
        			)
            	)
    		)
    	);
    }
    
    // salt crust on salt flats, sand on barrier islands, mud in deltas, and gravel and rubble where rock was broken up
    // and carried down: alluvial fans, moraines and cirques
    private static MaterialRule makeLandformSurfaceRule(HolderGetter<Noise> noise, MaterialCondition dry, MaterialRule sand, MaterialRule gravel) {
    	Holder<Noise> debris = noise.getOrThrow(PresetSurfaceNoise.GLACIAL_DEBRIS);
    	MaterialCondition topLayers = MaterialRules.stoneDepthCheck(2, false, CaveSurface.FLOOR);
    	return MaterialRules.sequence(
    		MaterialRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.SALT_FLAT),
    			MaterialRules.sequence(
    				MaterialRules.ifTrue(
    					ON_FLOOR,
    					UTFSurfaceRules.noise(noise.getOrThrow(PresetSurfaceNoise.SALT_FLAT), List.of(Pair.of(SALT_CRACK, WHITE_TERRACOTTA), Pair.of(-1.0F, CALCITE)))
    				),
    				MaterialRules.ifTrue(UNDER_FLOOR, sand)
    			)
    		),
    		MaterialRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.BARRIER_ISLAND, TerrainType.LAGOON, TerrainType.SAND_BAR, TerrainType.SAND_WAVES),
    			MaterialRules.ifTrue(UNDER_FLOOR, sand)
    		),
    		MaterialRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.CORAL_REEF),
    			MaterialRules.sequence(
    				MaterialRules.ifTrue(ON_FLOOR, UTFSurfaceRules.noise(noise.getOrThrow(PresetSurfaceNoise.CORAL), List.of(Pair.of(0.82F, FIRE_CORAL), Pair.of(0.66F, BRAIN_CORAL), Pair.of(0.5F, TUBE_CORAL), Pair.of(0.34F, HORN_CORAL), Pair.of(0.2F, BUBBLE_CORAL), Pair.of(-1.0F, sand)))),
    				MaterialRules.ifTrue(UNDER_FLOOR, sand)
    			)
    		),
    		MaterialRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.DELTA),
    			MaterialRules.ifTrue(
    				topLayers,
    				MaterialRules.sequence(
    					MaterialRules.ifTrue(MaterialRules.not(dry), UTFSurfaceRules.noise(debris, List.of(Pair.of(0.65F, CLAY), Pair.of(0.4F, MUD), Pair.of(-1.0F, sand)))),
    					UTFSurfaceRules.noise(debris, List.of(Pair.of(0.62F, MUD), Pair.of(0.54F, sand)))
    				)
    			)
    		),
    		MaterialRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.ALLUVIAL_FAN),
    			MaterialRules.ifTrue(
    				topLayers,
    				UTFSurfaceRules.noise(debris, List.of(Pair.of(0.56F, gravel), Pair.of(0.44F, COARSE_DIRT)))
    			)
    		),
    		MaterialRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.MORAINE),
    			MaterialRules.ifTrue(
    				ON_FLOOR,
    				UTFSurfaceRules.noise(debris, List.of(Pair.of(0.74F, MOSSY_COBBLESTONE), Pair.of(0.64F, COBBLESTONE), Pair.of(0.48F, gravel), Pair.of(0.36F, COARSE_DIRT)))
    			)
    		),
    		MaterialRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.CIRQUE),
    			MaterialRules.ifTrue(
    				ON_FLOOR,
    				UTFSurfaceRules.noise(debris, List.of(Pair.of(0.5F, gravel), Pair.of(0.32F, STONE)))
    			)
    		)
    	);
    }

    // sand in the shallows; sand, gravel and clay further out; bare rock only on the steepest slopes, like the walls of
    // canyons and trenches
    private static MaterialRule makeSeaFloorRule(int seaLevel, HolderGetter<Noise> noise, MaterialCondition dry, MaterialRule sand, MaterialRule gravel) {
    	Holder<Noise> patches = noise.getOrThrow(PresetSurfaceNoise.RIVER_BED);
    	MaterialCondition underSea = MaterialRules.not(MaterialRules.yBlockCheck(VerticalAnchor.absolute(seaLevel - 1), 0));
    	MaterialCondition shallow = MaterialRules.yBlockCheck(VerticalAnchor.absolute(seaLevel - SHALLOW_SEA), 0);
    	MaterialCondition middle = MaterialRules.yBlockCheck(VerticalAnchor.absolute(seaLevel - MIDDLE_SEA), 0);
    	return MaterialRules.ifTrue(
    		MaterialRules.not(dry),
    		MaterialRules.ifTrue(
    			underSea,
    			MaterialRules.ifTrue(
    				MaterialRules.not(UTFSurfaceConditions.steepness(0.85F)),
    				MaterialRules.ifTrue(
    					MaterialRules.stoneDepthCheck(1, false, CaveSurface.FLOOR),
    					MaterialRules.sequence(
    						MaterialRules.ifTrue(shallow, UTFSurfaceRules.noise(patches, List.of(Pair.of(0.74F, gravel), Pair.of(-1.0F, sand)))),
    						MaterialRules.ifTrue(middle, UTFSurfaceRules.noise(patches, List.of(Pair.of(0.7F, CLAY), Pair.of(0.46F, gravel), Pair.of(-1.0F, sand)))),
    						UTFSurfaceRules.noise(patches, List.of(Pair.of(0.58F, CLAY), Pair.of(0.32F, gravel), Pair.of(-1.0F, sand)))
    					)
    				)
    			)
    		)
    	);
    }

    // beds and banks of gravel and cobbles in the mountains; sand, with patches of gravel and clay, in the lowlands; and
    // mud and clay in wetlands and deltas. Steep banks, like the sides of gorges, keep their rock
    private static MaterialRule makeRiverBankRule(HolderGetter<Biome> biomes, Scaling scaling, HolderGetter<Noise> noise, MaterialCondition dry, MaterialRule sand, MaterialRule gravel) {
    	Holder<Noise> bed = noise.getOrThrow(PresetSurfaceNoise.RIVER_BED);
    	MaterialCondition underwater = MaterialRules.not(dry);
    	MaterialCondition wet = UTFSurfaceConditions.any(UTFSurfaceConditions.terrain(TerrainType.WETLAND, TerrainType.DELTA), MaterialRules.isBiome(biomes, Biomes.SWAMP, Biomes.MANGROVE_SWAMP));
    	MaterialCondition high = UTFSurfaceConditions.height(scaling.ground(MOUNTAIN_RIVER_HEIGHT));
    	MaterialRule wetBanks = MaterialRules.sequence(
    		MaterialRules.ifTrue(underwater, UTFSurfaceRules.noise(bed, List.of(Pair.of(0.62F, CLAY), Pair.of(-1.0F, MUD)))),
    		UTFSurfaceRules.noise(bed, List.of(Pair.of(0.55F, MUD)))
    	);
    	MaterialRule mountainBanks = MaterialRules.sequence(
    		MaterialRules.ifTrue(underwater, UTFSurfaceRules.noise(bed, List.of(Pair.of(0.7F, MOSSY_COBBLESTONE), Pair.of(0.6F, COBBLESTONE), Pair.of(-1.0F, gravel)))),
    		MaterialRules.ifTrue(UTFSurfaceConditions.riverSide(0.9F), UTFSurfaceRules.noise(bed, List.of(Pair.of(0.5F, gravel), Pair.of(0.4F, COARSE_DIRT))))
    	);
    	MaterialRule lowlandBanks = MaterialRules.sequence(
    		MaterialRules.ifTrue(underwater, UTFSurfaceRules.noise(bed, List.of(Pair.of(0.7F, CLAY), Pair.of(0.58F, gravel), Pair.of(-1.0F, sand)))),
    		MaterialRules.ifTrue(UTFSurfaceConditions.riverSide(0.85F), UTFSurfaceRules.noise(bed, List.of(Pair.of(0.45F, sand))))
    	);
    	return MaterialRules.ifTrue(
    		MaterialRules.not(UTFSurfaceConditions.steepness(0.45F)),
    		MaterialRules.ifTrue(
    			MaterialRules.stoneDepthCheck(2, false, CaveSurface.FLOOR),
    			MaterialRules.sequence(
    				// the whole of a wetland, not just along its river
    				MaterialRules.ifTrue(wet, MaterialRules.ifTrue(UTFSurfaceConditions.any(UTFSurfaceConditions.terrain(TerrainType.WETLAND, TerrainType.DELTA), UTFSurfaceConditions.riverSide(1.0F)), wetBanks)),
    				MaterialRules.ifTrue(
    					UTFSurfaceConditions.riverSide(1.0F),
    					MaterialRules.ifTrue(
    						MaterialRules.not(wet),
    						MaterialRules.sequence(
    							MaterialRules.ifTrue(high, mountainBanks),
    							MaterialRules.ifTrue(MaterialRules.not(high), lowlandBanks)
    						)
    					)
    				)
    			)
    		)
    	);
    }

    // lava over magma in the crater; basalt, blackstone and tuff all over the cone; patches of old lava flows on the
    // land around it, with the usual surface in between
    private static MaterialRule makeVolcanoRule(HolderGetter<Noise> noise) {
    	MaterialRule volcanicRock = UTFSurfaceRules.noise(
    		noise.getOrThrow(PresetSurfaceNoise.VOLCANIC_ROCK),
    		List.of(
    			Pair.of(0.0F, BASALT),
    			Pair.of(0.4F, BLACKSTONE),
    			Pair.of(0.62F, TUFF),
    			Pair.of(0.8F, SMOOTH_BASALT)
    		)
    	);
    	return MaterialRules.sequence(
    		MaterialRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.VOLCANO_PIPE),
    			MaterialRules.sequence(
    				MaterialRules.ifTrue(ON_FLOOR, LAVA),
    				MAGMA_BLOCK
    			)
    		),
    		MaterialRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.VOLCANO, TerrainType.VOLCANIC_ISLAND),
    			volcanicRock
    		),
    		MaterialRules.ifTrue(
    			UTFSurfaceConditions.terrain(TerrainType.VOLCANIC_LOWLANDS),
    			MaterialRules.sequence(
    				MaterialRules.ifTrue(UTFSurfaceConditions.steepness(0.15F), volcanicRock),
    				MaterialRules.ifTrue(
    					ON_FLOOR,
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

    private static MaterialRule makeForestRule(HolderGetter<Noise> noise) {
    	return MaterialRules.ifTrue(
    		ON_FLOOR, 
    		UTFSurfaceRules.noise(
    			noise.getOrThrow(PresetSurfaceNoise.FOREST), 
    			List.of(
    				Pair.of(0.65F, PODZOL),
    				Pair.of(0.725F, DIRT)
    			)
    		)	
    	);
    }
    
	private static MaterialRule makeStrataRule(MiscellaneousSettings miscellaneousSettings, HolderGetter<Noise> noise) {
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
	private static MaterialRule makeDeepStrataRule(HolderGetter<Noise> noise) {
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
    private static MaterialRule makeErodedDirtRule(HolderGetter<Noise> noise, SurfaceSettings.Erosion settings) {
    	MaterialCondition high = UTFSurfaceConditions.height(noise.getOrThrow(PresetSurfaceNoise.ERODED_DIRT), noise.getOrThrow(PresetSurfaceNoise.HEIGHT_VARIANCE));
    	return MaterialRules.ifTrue(
    		high,
    		MaterialRules.sequence(
    			MaterialRules.ifTrue(
    				UTFSurfaceConditions.steepness(settings.dirtSteepness, noise.getOrThrow(PresetSurfaceNoise.STEEPNESS_VARIANCE)),
    				COARSE_DIRT
    			),
    			MaterialRules.ifTrue(
    				UTFSurfaceConditions.steepness(settings.screeSteepness, noise.getOrThrow(PresetSurfaceNoise.STEEPNESS_VARIANCE)),
    				MaterialRules.sequence(MaterialRules.ifTrue(ON_CEILING, STONE), GRAVEL)
    			)
    		)
    	);
    }
	
    // both conditions hold: neither fails
    private static MaterialCondition and(MaterialCondition a, MaterialCondition b) {
    	return MaterialRules.not(UTFSurfaceConditions.any(MaterialRules.not(a), MaterialRules.not(b)));
    }

    private static MaterialCondition surfaceNoiseAbove(double target) {
        return MaterialRules.noiseCondition2d(Noises.SURFACE, target / 8.25D, Double.MAX_VALUE);
    }
}