package com.pandaismyname1.ultraterraforged.world.worldgen.surface;

import java.util.List;
import java.util.function.Function;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.StrataStack;

public interface UTFSurfaceSystem {
	List<StrataStack> getOrCreateStrata(Identifier name, Function<RandomSource, List<StrataStack>> factory);
}
