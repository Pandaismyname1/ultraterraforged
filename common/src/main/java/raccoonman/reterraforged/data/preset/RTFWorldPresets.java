package raccoonman.reterraforged.data.preset;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import raccoonman.reterraforged.RTFCommon;

/**
 * The "ReTerraForged" world type (data/reterraforged/worldgen/world_preset/reterraforged.json). It has the same
 * dimensions as vanilla's default; ReTerraForged terrain comes from the preset datapack a world is created with,
 * which overrides the overworld's noise settings.
 */
public final class RTFWorldPresets {
	public static final ResourceKey<WorldPreset> RETERRAFORGED = ResourceKey.create(Registries.WORLD_PRESET, RTFCommon.location("reterraforged"));

	private RTFWorldPresets() {
	}
}
