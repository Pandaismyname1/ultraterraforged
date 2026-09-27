package com.pandaismyname1.ultraterraforged.data.preset;

import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBCompat;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBSurfaceRules;
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
import terrablender.core.TerraBlender;

public class PresetSurfaceLayerData {
	private static final SurfaceRules.RuleSource ORANGE_TERRACOTTA = makeStateRule(Blocks.ORANGE_TERRACOTTA);
	private static final SurfaceRules.RuleSource BROWN_TERRACOTTA = makeStateRule(Blocks.BROWN_TERRACOTTA);
	private static final SurfaceRules.RuleSource TERRACOTTA = makeStateRule(Blocks.TERRACOTTA);
    private static final SurfaceRules.RuleSource SMOOTH_SANDSTONE = makeStateRule(Blocks.SMOOTH_SANDSTONE);

	private static final SurfaceRules.RuleSource GRASS = makeStateRule(Blocks.GRASS_BLOCK);
	private static final SurfaceRules.RuleSource DIRT = makeStateRule(Blocks.DIRT);
	private static final SurfaceRules.RuleSource PODZOL = makeStateRule(Blocks.PODZOL);
	private static final SurfaceRules.RuleSource STONE = makeStateRule(Blocks.STONE);
    private static final SurfaceRules.RuleSource COARSE_DIRT = makeStateRule(Blocks.COARSE_DIRT);
    private static final SurfaceRules.RuleSource GRAVEL = makeStateRule(Blocks.GRAVEL);
    private static final SurfaceRules.RuleSource SAND = makeStateRule(Blocks.SAND);
	
	public static void bootstrap(Preset preset, BootstapContext<LayeredSurfaceRule.Layer> ctx) {
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
	    	SurfaceRules.ifTrue(
	    		erosionBiomeCheck(),
	    		SurfaceRules.ifTrue(
		    		UTFSurfaceConditions.steepness(settings.dirtSteepness, noise.getOrThrow(PresetSurfaceNoise.STEEPNESS_VARIANCE)),
		    		SurfaceRules.ifTrue(
		    			UTFSurfaceConditions.height(noise.getOrThrow(PresetSurfaceNoise.ERODED_DIRT), noise.getOrThrow(PresetSurfaceNoise.HEIGHT_VARIANCE)),
		    			COARSE_DIRT
		    		)
		    	)
        	)
    	);
    }

	private static LayeredSurfaceRule.Layer makeBadlandsErosion() {
		return LayeredSurfaceRule.layer(
			SurfaceRules.ifTrue(
				SurfaceRules.isBiome(Biomes.WOODED_BADLANDS),
				SurfaceRules.bandlands()
			)
		);
	}
	
//	private static LayeredSurfaceRule.Layer makeErosion(SurfaceSettings.Erosion erosion, HolderGetter<Noise> noise) {
//		SurfaceRules.ConditionSource erodedRock = UTFSurfaceConditions.steepness(erosion.rockSteepness, noise.getOrThrow(PresetSurfaceNoise.STEEPNESS_VARIANCE));
//		SurfaceRules.ConditionSource erodedRockVariance = UTFSurfaceConditions.height(noise.getOrThrow(PresetSurfaceNoise.ERODED_ROCK), noise.getOrThrow(PresetSurfaceNoise.HEIGHT_VARIANCE));
//		SurfaceRules.RuleSource erodedMaterial = UTFSurfaceRules.layered(UTFSurfaceLayerTags.EROSION_MATERIAL);
//		SurfaceRules.RuleSource erode = SurfaceRules.sequence(
//			SurfaceRules.ifTrue(
//				erodedRock, 
//				erodedMaterial
//			),	
//			SurfaceRules.ifTrue(
//				erodedRockVariance,
//				erodedMaterial
//			)
//		);
//		return LayeredSurfaceRule.layer(
//			SurfaceRuleData.overworld(),
//			SurfaceRules.ifTrue(
//				SurfaceRules.abovePreliminarySurface(),
//				SurfaceRules.sequence(
//					SurfaceRules.ifTrue(
//						SurfaceRules.ON_FLOOR, 
//						erode
//					),
//					SurfaceRules.ifTrue(
//						SurfaceRules.UNDER_FLOOR,
//						erode
//					)
//				)
//			)
//		);
//	}
	
	private static LayeredSurfaceRule.Layer makeDesert(Scaling scaling, HolderGetter<Noise> noise) {
    	Holder<Noise> variance = noise.getOrThrow(PresetSurfaceNoise.DESERT);
    	float min = scaling.ground(10);
    	float level = scaling.ground(40);
    	
    	SurfaceRules.ConditionSource aboveLevel = UTFSurfaceConditions.height(level, variance);
		return LayeredSurfaceRule.layer(
	    	SurfaceRules.ifTrue(
	    		SurfaceRules.isBiome(Biomes.DESERT),
	    		SurfaceRules.ifTrue(
		    		UTFSurfaceConditions.height(min), 
		    		SurfaceRules.sequence(
		    			SurfaceRules.ifTrue(
		    				UTFSurfaceConditions.steepness(0.15F), 
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
		    			),
		        		SurfaceRules.ifTrue(
		        			UTFSurfaceConditions.steepness(0.3F), 
		        			SMOOTH_SANDSTONE
		            	)
		    		)
		    	)
	    	)
    	);
    }

    private static LayeredSurfaceRule.Layer makeForest(HolderGetter<Noise> noise) {
		return LayeredSurfaceRule.layer(
			SurfaceRules.ifTrue(
				SurfaceRules.isBiome(Biomes.FOREST, Biomes.DARK_FOREST),
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
			SurfaceRules.ifTrue(
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
	
    private static SurfaceRules.ConditionSource erosionBiomeCheck() {
    	return SurfaceRules.not(UTFSurfaceConditions.biomeTag(UTFBiomeTags.EROSION_BLACKLIST));
    }
    
    private static SurfaceRules.RuleSource makeStateRule(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static ResourceKey<LayeredSurfaceRule.Layer> createKey(String name) {
        return ResourceKey.create(UTFRegistries.SURFACE_LAYERS, UTFCommon.location(name));
	}
}
