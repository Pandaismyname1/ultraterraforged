package com.pandaismyname1.ultraterraforged.data.preset;

import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.registries.RTFRegistries;

//TODO support different presets per dimension
public class PresetData {
	public static final ResourceKey<Preset> PRESET = RTFRegistries.createKey(RTFRegistries.PRESET, "preset");
	
	public static void bootstrap(Preset preset, BootstapContext<Preset> ctx) {
		ctx.register(PRESET, preset);
	}
}
