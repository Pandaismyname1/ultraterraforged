package raccoonman.reterraforged.data.preset;

import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.util.Seed;

public class PresetStrataNoise {
	public static final ResourceKey<Noise> STRATA_SELECTOR = createKey("selector");
	public static final ResourceKey<Noise> STRATA_OFFSET = createKey("offset");
	public static final ResourceKey<Noise> STRATA_THICKNESS = createKey("thickness");

	// how far the layers rise and fall across the land, in blocks either way
	private static final float FOLD_HEIGHT = 14.0F;

	public static void bootstrap(Preset preset, BootstapContext<Noise> ctx) {
		Seed seed = new Seed(1234153);
		int strataScale = preset.miscellaneous().strataRegionSize;

		// irregular regions, each with its own sequence of rocks
		Noise selector = Noises.worley(seed.next(), strataScale);
		selector = Noises.warpPerlin(selector, seed.next(), strataScale / 4, 2, strataScale / 2F);
		selector = Noises.warpPerlin(selector, seed.next(), 15, 2, 30);
		ctx.register(STRATA_SELECTOR, selector);
		// broad folds, so layers climb and dip along a cliff instead of running perfectly flat
		ctx.register(STRATA_OFFSET, Noises.map(Noises.perlin(seed.next(), 350, 3), -FOLD_HEIGHT, FOLD_HEIGHT));
		ctx.register(STRATA_THICKNESS, Noises.map(Noises.perlin(seed.next(), 900, 2), 0.75F, 1.35F));
	}

	private static ResourceKey<Noise> createKey(String name) {
		return PresetNoiseData.createKey("strata/" + name);
	}
}
