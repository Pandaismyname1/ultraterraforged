package com.pandaismyname1.ultraterraforged.tags;

import net.minecraft.tags.TagKey;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.LayeredSurfaceRule;

public class UTFSurfaceLayerTags {
	public static final TagKey<LayeredSurfaceRule.Layer> TERRABLENDER = resolve("terrablender");
	
    private static TagKey<LayeredSurfaceRule.Layer> resolve(String path) {
    	return TagKey.create(UTFRegistries.SURFACE_LAYERS, UTFCommon.location(path));
    }
}
