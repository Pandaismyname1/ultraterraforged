package raccoonman.reterraforged.world.worldgen.surface;

import java.util.List;
import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataStack;

public interface RTFSurfaceSystem {
	List<StrataStack> getOrCreateStrata(ResourceLocation name, Function<RandomSource, List<StrataStack>> factory);
}
