package com.pandaismyname1.ultraterraforged.world.worldgen;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

/**
 * The random state a density function is compiled for. Since 26.3 density functions compile into samplers through a
 * {@link DensityFunction.CompileContext}, which only offers noises and randoms; UTF's own functions need the world's
 * generator, so the random state's context gives it out (MixinRandomStateCompileContext).
 */
public interface UTFCompileContext {

	RandomState ultraterraforged$randomState();

	@Nullable
	static RandomState randomState(DensityFunction.CompileContext context) {
		return context instanceof UTFCompileContext utfContext ? utfContext.ultraterraforged$randomState() : null;
	}
}
