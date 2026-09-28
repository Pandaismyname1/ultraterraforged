package com.pandaismyname1.ultraterraforged.data.preset;

import net.minecraft.world.level.biome.Biome;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.SurfaceSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.tags.UTFBiomeTags;
import com.pandaismyname1.ultraterraforged.tags.UTFSurfaceLayerTags;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition.UTFSurfaceConditions;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.LayeredSurfaceRule;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.UTFSurfaceRules;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.Scaling;

public class PresetSurfaceLayerData {
	private static final MaterialRule ORANGE_TERRACOTTA = makeStateRule(Blocks.DYED_TERRACOTTA.orange());
	private static final MaterialRule BROWN_TERRACOTTA = makeStateRule(Blocks.DYED_TERRACOTTA.brown());
	private static final MaterialRule TERRACOTTA = makeStateRule(Blocks.TERRACOTTA);
    private static final MaterialRule SMOOTH_SANDSTONE = makeStateRule(Blocks.SMOOTH_SANDSTONE);

	private static final MaterialRule GRASS = makeStateRule(Blocks.GRASS_BLOCK);
	private static final MaterialRule DIRT = makeStateRule(Blocks.DIRT);
	private static final MaterialRule PODZOL = makeStateRule(Blocks.PODZOL);
	private static final MaterialRule STONE = makeStateRule(Blocks.STONE);
    private static final MaterialRule COARSE_DIRT = makeStateRule(Blocks.COARSE_DIRT);
    private static final MaterialRule GRAVEL = makeStateRule(Blocks.GRAVEL);
    private static final MaterialRule SAND = makeStateRule(Blocks.SAND);
	
	public static void bootstrap(Preset preset, BootstrapContext<LayeredSurfaceRule.Layer> ctx) {
		HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		
		SurfaceSettings surfaceSettings = preset.surface();
		SurfaceSettings.Erosion erosion = surfaceSettings.erosion();
		
		Scaling scaling = Scaling.make(properties.terrainScaler(), properties.seaLevel);
		
	}
	
	private static LayeredSurfaceRule.Layer makeRockErosion() {
		return LayeredSurfaceRule.layer(STONE);
	}

    private static LayeredSurfaceRule.Layer makeDirtErosion(HolderGetter<Noise> noise, SurfaceSettings.Erosion settings) {
		return LayeredSurfaceRule.layer(
	    	MaterialRules.ifTrue(
	    		erosionBiomeCheck(),
	    		MaterialRules.ifTrue(
		    		UTFSurfaceConditions.steepness(settings.dirtSteepness, noise.getOrThrow(PresetSurfaceNoise.STEEPNESS_VARIANCE)),
		    		MaterialRules.ifTrue(
		    			UTFSurfaceConditions.height(noise.getOrThrow(PresetSurfaceNoise.ERODED_DIRT), noise.getOrThrow(PresetSurfaceNoise.HEIGHT_VARIANCE)),
		    			COARSE_DIRT
		    		)
		    	)
        	)
    	);
    }

	private static LayeredSurfaceRule.Layer makeBadlandsErosion(HolderGetter<Biome> biomes) {
		return LayeredSurfaceRule.layer(
			MaterialRules.ifTrue(
				MaterialRules.isBiome(biomes, Biomes.WOODED_BADLANDS),
				MaterialRules.bandlands()
			)
		);
	}
	
//	private static LayeredSurfaceRule.Layer makeErosion(SurfaceSettings.Erosion erosion, HolderGetter<Noise> noise) {
//		MaterialCondition erodedRock = UTFSurfaceConditions.steepness(erosion.rockSteepness, noise.getOrThrow(PresetSurfaceNoise.STEEPNESS_VARIANCE));
//		MaterialCondition erodedRockVariance = UTFSurfaceConditions.height(noise.getOrThrow(PresetSurfaceNoise.ERODED_ROCK), noise.getOrThrow(PresetSurfaceNoise.HEIGHT_VARIANCE));
//		MaterialRule erodedMaterial = UTFSurfaceRules.layered(UTFSurfaceLayerTags.EROSION_MATERIAL);
//		MaterialRule erode = MaterialRules.sequence(
//			MaterialRules.ifTrue(
//				erodedRock, 
//				erodedMaterial
//			),	
//			MaterialRules.ifTrue(
//				erodedRockVariance,
//				erodedMaterial
//			)
//		);
//		return LayeredSurfaceRule.layer(
//			SurfaceRuleData.overworld(),
//			MaterialRules.ifTrue(
//				MaterialRules.abovePreliminarySurface(),
//				MaterialRules.sequence(
//					MaterialRules.ifTrue(
//						ON_FLOOR, 
//						erode
//					),
//					MaterialRules.ifTrue(
//						UNDER_FLOOR,
//						erode
//					)
//				)
//			)
//		);
//	}
	
	private static LayeredSurfaceRule.Layer makeDesert(HolderGetter<Biome> biomes, Scaling scaling, HolderGetter<Noise> noise) {
    	Holder<Noise> variance = noise.getOrThrow(PresetSurfaceNoise.DESERT);
    	float min = scaling.ground(10);
    	float level = scaling.ground(40);
    	
    	MaterialCondition aboveLevel = UTFSurfaceConditions.height(level, variance);
		return LayeredSurfaceRule.layer(
	    	MaterialRules.ifTrue(
	    		MaterialRules.isBiome(biomes, Biomes.DESERT),
	    		MaterialRules.ifTrue(
		    		UTFSurfaceConditions.height(min), 
		    		MaterialRules.sequence(
		    			MaterialRules.ifTrue(
		    				UTFSurfaceConditions.steepness(0.15F), 
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
		    			),
		        		MaterialRules.ifTrue(
		        			UTFSurfaceConditions.steepness(0.3F), 
		        			SMOOTH_SANDSTONE
		            	)
		    		)
		    	)
	    	)
    	);
    }

    private static LayeredSurfaceRule.Layer makeForest(HolderGetter<Biome> biomes, HolderGetter<Noise> noise) {
		return LayeredSurfaceRule.layer(
			MaterialRules.ifTrue(
				MaterialRules.isBiome(biomes, Biomes.FOREST, Biomes.DARK_FOREST),
				UTFSurfaceRules.noise(
					noise.getOrThrow(PresetSurfaceNoise.FOREST), 
					List.of(
						Pair.of(0.65F, PODZOL),
						Pair.of(0.725F, DIRT)
		    		)
		    	)
			)
    	);
    }
    

    private static LayeredSurfaceRule.Layer makeRiverBank(HolderGetter<Noise> noise) {
		return LayeredSurfaceRule.layer(
			MaterialRules.ifTrue(
				UTFSurfaceConditions.riverBank(0.002F),
				UTFSurfaceRules.noise(
					noise.getOrThrow(PresetSurfaceNoise.RIVER_BANK), 
					List.of(
						Pair.of(0.35F, GRAVEL),
						Pair.of(0.425F, COARSE_DIRT)
		    		)
		    	)
			)
    	);
    }
	
    private static MaterialCondition erosionBiomeCheck() {
    	return MaterialRules.not(UTFSurfaceConditions.biomeTag(UTFBiomeTags.EROSION_BLACKLIST));
    }
    
    private static MaterialRule makeStateRule(Block block) {
        return MaterialRules.state(block.defaultBlockState());
    }

    public static ResourceKey<LayeredSurfaceRule.Layer> createKey(String name) {
        return ResourceKey.create(UTFRegistries.SURFACE_LAYERS, UTFCommon.location(name));
	}
}
