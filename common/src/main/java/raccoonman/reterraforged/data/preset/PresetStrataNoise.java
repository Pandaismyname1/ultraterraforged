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

	// how far tilted layers rise and fall across a region, in blocks either way, in the most tilted regions
	private static final float TILT_HEIGHT = 40.0F;
	private static final int TILT_SCALE = 400;
	// small waves on top, so a single cliff face shows bent layers
	private static final float FOLD_HEIGHT = 6.0F;
	private static final int FOLD_SCALE = 64;

	public static void bootstrap(Preset preset, BootstapContext<Noise> ctx) {
		Seed seed = new Seed(1234153);
		int strataScale = preset.miscellaneous().strataRegionSize;

		// irregular regions, each with its own sequence of rocks
		Noise selector = Noises.worley(seed.next(), strataScale);
		selector = Noises.warpPerlin(selector, seed.next(), strataScale / 4, 2, strataScale / 2F);
		selector = Noises.warpPerlin(selector, seed.next(), 15, 2, 30);
		ctx.register(STRATA_SELECTOR, selector);
		// Layers tilt, and how much depends on the region: some lie almost flat, others dip steeply. Where regions
		// meet the layers jump, like at a fault.
		Noise tiltStrength = Noises.map(selector, 0.15F, 1.0F);
		Noise tilt = Noises.mul(Noises.map(Noises.perlin(seed.next(), TILT_SCALE, 3), -TILT_HEIGHT, TILT_HEIGHT), tiltStrength);
		Noise folds = Noises.map(Noises.perlin(seed.next(), FOLD_SCALE, 2), -FOLD_HEIGHT, FOLD_HEIGHT);
		ctx.register(STRATA_OFFSET, Noises.add(tilt, folds));
		ctx.register(STRATA_THICKNESS, Noises.map(Noises.perlin(seed.next(), 900, 2), 0.75F, 1.35F));
	}

	private static ResourceKey<Noise> createKey(String name) {
		return PresetNoiseData.createKey("strata/" + name);
	}
}
