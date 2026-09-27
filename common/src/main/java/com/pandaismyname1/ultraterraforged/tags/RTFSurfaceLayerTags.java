package com.pandaismyname1.ultraterraforged.tags;

import net.minecraft.tags.TagKey;
import com.pandaismyname1.ultraterraforged.RTFCommon;
import com.pandaismyname1.ultraterraforged.registries.RTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.LayeredSurfaceRule;

public class RTFSurfaceLayerTags {
	public static final TagKey<LayeredSurfaceRule.Layer> TERRABLENDER = resolve("terrablender");
	
    private static TagKey<LayeredSurfaceRule.Layer> resolve(String path) {
    	return TagKey.create(RTFRegistries.SURFACE_LAYERS, RTFCommon.location(path));
    }
}
