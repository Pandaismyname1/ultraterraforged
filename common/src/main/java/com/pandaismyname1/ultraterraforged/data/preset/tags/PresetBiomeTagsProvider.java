package com.pandaismyname1.ultraterraforged.data.preset.tags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import com.pandaismyname1.ultraterraforged.data.preset.settings.MiscellaneousSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.tags.UTFBiomeTags;

public class PresetBiomeTagsProvider extends TagsProvider<Biome> {
	private Preset preset;
	
	public PresetBiomeTagsProvider(Preset preset, PackOutput packOutput, CompletableFuture<Provider> completableFuture) {
		super(packOutput, Registries.BIOME, completableFuture);
		
		this.preset = preset;
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		MiscellaneousSettings miscellaneous = this.preset.miscellaneous();

		this.tag(UTFBiomeTags.HAS_SWAMP_SURFACE).add(Biomes.SWAMP);
		this.tag(UTFBiomeTags.EROSION_BLACKLIST).add(Biomes.DESERT, Biomes.BADLANDS, Biomes.WOODED_BADLANDS);
		
		if(miscellaneous.customBiomeFeatures) {
			this.tag(UTFBiomeTags.HAS_SWAMP_TREES).add(Biomes.SWAMP);
			this.tag(UTFBiomeTags.HAS_PLAINS_TREES).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.RIVER, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN);
			this.tag(UTFBiomeTags.HAS_FOREST_TREES).add(Biomes.FOREST);
			this.tag(UTFBiomeTags.HAS_FLOWER_FOREST_TREES).add(Biomes.FLOWER_FOREST);
			this.tag(UTFBiomeTags.HAS_BIRCH_FOREST_TREES).add(Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST);
			this.tag(UTFBiomeTags.HAS_DARK_FOREST_TREES).add(Biomes.DARK_FOREST);
			this.tag(UTFBiomeTags.HAS_SAVANNA_TREES).add(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU);
			this.tag(UTFBiomeTags.HAS_MEADOW_TREES).add(Biomes.MEADOW);
			this.tag(UTFBiomeTags.HAS_FIR_FOREST_TREES).add(Biomes.WINDSWEPT_FOREST);
			this.tag(UTFBiomeTags.HAS_GROVE_TREES).add(Biomes.GROVE);
			this.tag(UTFBiomeTags.HAS_WINDSWEPT_HILLS_TREES).add(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS);
			this.tag(UTFBiomeTags.HAS_PINE_FOREST_TREES).add(Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);
			this.tag(UTFBiomeTags.HAS_SPRUCE_FOREST_TREES).add(Biomes.SNOWY_TAIGA);
			this.tag(UTFBiomeTags.HAS_SPRUCE_TUNDRA_TREES).add(Biomes.SNOWY_PLAINS, Biomes.FROZEN_RIVER);
			this.tag(UTFBiomeTags.HAS_REDWOOD_FOREST_TREES).add(Biomes.OLD_GROWTH_PINE_TAIGA);
			this.tag(UTFBiomeTags.HAS_JUNGLE_TREES).add(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE);
			this.tag(UTFBiomeTags.HAS_JUNGLE_EDGE_TREES).add(Biomes.SPARSE_JUNGLE);
			this.tag(UTFBiomeTags.HAS_BADLANDS_TREES).add(Biomes.WINDSWEPT_SAVANNA);
			this.tag(UTFBiomeTags.HAS_WOODED_BADLANDS_TREES).add(Biomes.WOODED_BADLANDS);
	
			this.tag(UTFBiomeTags.HAS_MARSH_BUSHES).add();
			this.tag(UTFBiomeTags.HAS_PLAINS_BUSHES).add(Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.WINDSWEPT_HILLS, Biomes.MEADOW);
			this.tag(UTFBiomeTags.HAS_STEPPE_BUSHES).add(Biomes.SAVANNA, Biomes.WINDSWEPT_SAVANNA, Biomes.SAVANNA_PLATEAU);
			this.tag(UTFBiomeTags.HAS_COLD_STEPPE_BUSHES).add();
			this.tag(UTFBiomeTags.HAS_COLD_TAIGA_SCRUB_BUSHES).add(Biomes.SNOWY_PLAINS, Biomes.TAIGA, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_GRAVELLY_HILLS);
			
			this.tag(UTFBiomeTags.HAS_FOREST_GRASS).add(Biomes.FOREST, Biomes.DARK_FOREST);
			this.tag(UTFBiomeTags.HAS_MEADOW_GRASS).add(Biomes.MEADOW, Biomes.FLOWER_FOREST);
			this.tag(UTFBiomeTags.HAS_FERN_GRASS).add(Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_HILLS, Biomes.TAIGA);
			this.tag(UTFBiomeTags.HAS_BIRCH_GRASS).add(Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST);
		}
	}
}