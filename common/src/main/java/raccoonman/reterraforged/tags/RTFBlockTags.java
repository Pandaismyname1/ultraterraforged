package raccoonman.reterraforged.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import raccoonman.reterraforged.RTFCommon;

public class RTFBlockTags {
	public static final TagKey<Block> SOIL = resolve("soil");
	public static final TagKey<Block> ROCK = resolve("rock");
	public static final TagKey<Block> ORE_COMPATIBLE_ROCK = resolve("ore_compatible_rock");
	// rocks that never go into rock layers, whatever other tags they are in
	public static final TagKey<Block> STRATA_EXCLUDED = resolve("strata_excluded");
	// the rocks layered into deepslate
	public static final TagKey<Block> DEEP_ROCK = resolve("deep_rock");
	public static final TagKey<Block> CLAY = resolve("clay");
	public static final TagKey<Block> SEDIMENT = resolve("sediment");
	public static final TagKey<Block> ERODIBLE = resolve("erodible");
	
    private static TagKey<Block> resolve(String path) {
    	return TagKey.create(Registries.BLOCK, RTFCommon.location(path));
    }
}
