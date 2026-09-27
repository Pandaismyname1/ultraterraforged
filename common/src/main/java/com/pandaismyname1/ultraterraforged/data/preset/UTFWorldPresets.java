package com.pandaismyname1.ultraterraforged.data.preset;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import com.pandaismyname1.ultraterraforged.UTFCommon;

/**
 * The "UltraTerraForged" world type (data/ultraterraforged/worldgen/world_preset/ultraterraforged.json). It has the same
 * dimensions as vanilla's default; UltraTerraForged terrain comes from the preset datapack a world is created with,
 * which overrides the overworld's noise settings.
 */
public final class UTFWorldPresets {
	public static final ResourceKey<WorldPreset> ULTRATERRAFORGED = ResourceKey.create(Registries.WORLD_PRESET, UTFCommon.location("ultraterraforged"));

	private UTFWorldPresets() {
	}
}
