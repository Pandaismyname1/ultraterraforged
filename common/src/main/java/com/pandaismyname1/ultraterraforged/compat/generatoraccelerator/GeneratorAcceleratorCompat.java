package com.pandaismyname1.ultraterraforged.compat.generatoraccelerator;

import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;

/**
 * Generator Accelerator samples a column of terrain (for structures' heights, getBaseHeight and getBaseColumn) with a
 * copy of vanilla's loop of its own, over the whole height of the world. UltraTerraForged makes the noise chunk only as
 * tall as the terrain there, and tells vanilla's loop so, but not the copy: it read past the end of the noise chunk and
 * every chunk's structures failed. The copy is now told the same height (mixin.generatoraccelerator).
 */
public final class GeneratorAcceleratorCompat {
	public static final String MOD_ID = "generator_accelerator";

	private GeneratorAcceleratorCompat() {
	}

	public static boolean isEnabled() {
		return ModLoaderUtil.isLoaded(MOD_ID);
	}
}
