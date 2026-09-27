package com.pandaismyname1.ultraterraforged.data.preset.tags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import com.pandaismyname1.ultraterraforged.data.preset.PresetSurfaceLayerData;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.registries.RTFRegistries;
import com.pandaismyname1.ultraterraforged.tags.RTFSurfaceLayerTags;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.LayeredSurfaceRule;

public class PresetSurfaceLayerProvider extends TagsProvider<LayeredSurfaceRule.Layer> {
	private Preset preset;
	
	public PresetSurfaceLayerProvider(Preset preset, PackOutput packOutput, CompletableFuture<Provider> completableFuture) {
		super(packOutput, RTFRegistries.SURFACE_LAYERS, completableFuture);
		
		this.preset = preset;
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {

	}
}