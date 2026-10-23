package com.pandaismyname1.ultraterraforged.compat.generatoraccelerator;

import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;

/**
 * Generator Accelerator samples a column of terrain (for structures' heights, getBaseHeight and getBaseColumn) with a
 * copy of vanilla's loop of its own, over the whole height of the world. UltraTerraForged makes the noise chunk only as
 * tall as the terrain there, and tells vanilla's loop so, but not the copy: it read past the end of the noise chunk and
 * every chunk's structures failed. The copy is now told the same height (mixin.generatoraccelerator).
 *
 * On 1.20.1 it also puts faster density functions of its own in the place of vanilla's add, mul, min, max, abs,
 * square, cube, half_negative, quarter_negative, squeeze, clamp, range_choice and shifted_noise, and of every spline
 * made with CubicSpline.Builder. Files that name those types now load as its own, but the ones made in code (vanilla's,
 * and UltraTerraForged's own) could no longer be written: their types had no name any more. Making the world's preset
 * datapack failed, and so did creating a world. Those types are written under their vanilla names again
 * (MixinMappedRegistry), and the splines as vanilla's (GeneratorAcceleratorCodecs); Generator Accelerator reads them back
 * as its own.
 *
 * Its feature placement also asks every placement modifier for generatePositionsFast, which UltraTerraForged's own
 * modifiers now have. Its surface builder only knows vanilla's surface rules and fails on UltraTerraForged's; it steps
 * aside when ModernFix is installed, so on 1.20.1 Generator Accelerator needs ModernFix next to UltraTerraForged.
 */
public final class GeneratorAcceleratorCompat {
	public static final String MOD_ID = "generator_accelerator";

	private GeneratorAcceleratorCompat() {
	}

	// the mixin plugin calls this before Minecraft's classes may be loaded, so this class mentions none of them
	public static boolean isEnabled() {
		return ModLoaderUtil.isLoaded(MOD_ID);
	}
}
