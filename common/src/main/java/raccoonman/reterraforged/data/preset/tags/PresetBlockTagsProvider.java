package raccoonman.reterraforged.data.preset.tags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import raccoonman.reterraforged.tags.RTFBlockTags;

public class PresetBlockTagsProvider extends IntrinsicHolderTagsProvider<Block> {
	
	public PresetBlockTagsProvider(PackOutput packOutput, CompletableFuture<Provider> completableFuture) {
		super(packOutput, Registries.BLOCK, completableFuture, (block) -> block.builtInRegistryHolder().key());
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		this.tag(RTFBlockTags.SOIL).add(Blocks.DIRT, Blocks.COARSE_DIRT);
		this.tag(RTFBlockTags.CLAY).add(Blocks.CLAY);
		this.tag(RTFBlockTags.SEDIMENT).add(Blocks.SAND, Blocks.GRAVEL);
		this.tag(RTFBlockTags.ERODIBLE).add(Blocks.SNOW_BLOCK).add(Blocks.POWDER_SNOW).add(Blocks.PACKED_ICE).add(Blocks.GRAVEL).addOptionalTag(BlockTags.DIRT.location());

		// Rock layers are built from the vanilla stone tags, which mods already add their natural stone to, so their rocks
		// show up in the layers without the mods knowing about ReTerraForged. Ores generate in stone_ore_replaceables.
		// The references are optional because this pack is generated on its own, where vanilla's tags don't exist yet;
		// they resolve once the world loads the pack alongside vanilla.
		this.tag(RTFBlockTags.ORE_COMPATIBLE_ROCK).addOptionalTag(BlockTags.STONE_ORE_REPLACEABLES.location());
		this.tag(RTFBlockTags.ROCK)
			.add(Blocks.CALCITE)
			.addOptionalTag(BlockTags.STONE_ORE_REPLACEABLES.location())
			.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD.location())
			// Create's decorative stones, which it doesn't tag as stone
			.addOptional(new ResourceLocation("create", "asurine"))
			.addOptional(new ResourceLocation("create", "crimsite"))
			.addOptional(new ResourceLocation("create", "limestone"))
			.addOptional(new ResourceLocation("create", "ochrum"))
			.addOptional(new ResourceLocation("create", "scorchia"))
			.addOptional(new ResourceLocation("create", "scoria"))
			.addOptional(new ResourceLocation("create", "veridium"));
		// deepslate is layered on its own, with the rocks ores turn into deepslate ores in
		this.tag(RTFBlockTags.DEEP_ROCK).addOptionalTag(BlockTags.DEEPSLATE_ORE_REPLACEABLES.location());
		this.tag(RTFBlockTags.STRATA_EXCLUDED).add(Blocks.DEEPSLATE);
		this.tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES).add(Blocks.CLAY);
	}
}