package com.pandaismyname1.ultraterraforged.data.preset.tags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.pandaismyname1.ultraterraforged.tags.UTFBlockTags;

public class PresetBlockTagsProvider extends TagsProvider<Block> {
	
	public PresetBlockTagsProvider(PackOutput packOutput, CompletableFuture<Provider> completableFuture) {
		super(packOutput, Registries.BLOCK, completableFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		this.tag(UTFBlockTags.SOIL).add(key(Blocks.DIRT), key(Blocks.COARSE_DIRT));
		this.tag(UTFBlockTags.CLAY).add(key(Blocks.CLAY));
		this.tag(UTFBlockTags.SEDIMENT).add(key(Blocks.SAND), key(Blocks.GRAVEL));
		this.tag(UTFBlockTags.ERODIBLE).add(key(Blocks.SNOW_BLOCK)).add(key(Blocks.POWDER_SNOW)).add(key(Blocks.PACKED_ICE)).add(key(Blocks.GRAVEL)).addOptionalTag(BlockTags.DIRT);

		// Rock layers are built from the vanilla stone tags, which mods already add their natural stone to, so their rocks
		// show up in the layers without the mods knowing about UltraTerraForged. Ores generate in stone_ore_replaceables.
		// The references are optional because this pack is generated on its own, where vanilla's tags don't exist yet;
		// they resolve once the world loads the pack alongside vanilla.
		this.tag(UTFBlockTags.ORE_COMPATIBLE_ROCK).addOptionalTag(BlockTags.STONE_ORE_REPLACEABLES);
		this.tag(UTFBlockTags.ROCK)
			.add(key(Blocks.CALCITE))
			.addOptionalTag(BlockTags.STONE_ORE_REPLACEABLES)
			.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD);
		// Create's decorative stones, which it doesn't tag as stone; by id, as they aren't blocks of this game
		for (String stone : new String[] { "asurine", "crimsite", "limestone", "ochrum", "scorchia", "scoria", "veridium" }) {
			this.getOrCreateRawBuilder(UTFBlockTags.ROCK).addOptionalElement(Identifier.fromNamespaceAndPath("create", stone));
		}
		// deepslate is layered on its own, with the rocks ores turn into deepslate ores in
		this.tag(UTFBlockTags.DEEP_ROCK).addOptionalTag(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
		this.tag(UTFBlockTags.STRATA_EXCLUDED).add(key(Blocks.DEEPSLATE));
		this.tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES).add(key(Blocks.CLAY));
	}

	// since 26.2 tags are written from resource keys only
	private static ResourceKey<Block> key(Block block) {
		return block.builtInRegistryHolder().key();
	}
}
