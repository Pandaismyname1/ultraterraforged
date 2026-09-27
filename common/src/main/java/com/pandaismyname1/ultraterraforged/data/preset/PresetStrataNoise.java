package com.pandaismyname1.ultraterraforged.data.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.Seed;

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

	// the most a layer is moved up or down; StrataRule keeps this much margin around the world
	public static final float MAX_OFFSET = TILT_HEIGHT + FOLD_HEIGHT;

	public static void bootstrap(Preset preset, BootstrapContext<Noise> ctx) {
		Layers noises = make(preset.miscellaneous().strataRegionSize);
		ctx.register(STRATA_SELECTOR, noises.selector());
		ctx.register(STRATA_OFFSET, noises.offset());
		ctx.register(STRATA_THICKNESS, noises.thickness());
	}

	// the noises that shape the rock layers
	public record Layers(Noise selector, Noise offset, Noise thickness) {
	}

	public static Layers make(int regionSize) {
		Seed seed = new Seed(1234153);

		// irregular regions, each with its own sequence of rocks
		Noise selector = Noises.worley(seed.next(), regionSize);
		selector = Noises.warpPerlin(selector, seed.next(), regionSize / 4, 2, regionSize / 2F);
		selector = Noises.warpPerlin(selector, seed.next(), 15, 2, 30);
		// Layers tilt, and how much depends on the region: some lie almost flat, others dip steeply. Where regions
		// meet the layers jump, like at a fault.
		Noise tiltStrength = Noises.map(selector, 0.15F, 1.0F);
		Noise tilt = Noises.mul(Noises.map(Noises.perlin(seed.next(), TILT_SCALE, 3), -TILT_HEIGHT, TILT_HEIGHT), tiltStrength);
		Noise folds = Noises.map(Noises.perlin(seed.next(), FOLD_SCALE, 2), -FOLD_HEIGHT, FOLD_HEIGHT);
		Noise offset = Noises.add(tilt, folds);
		Noise thickness = Noises.map(Noises.perlin(seed.next(), 900, 2), 0.75F, 1.35F);
		return new Layers(selector, offset, thickness);
	}

	private static ResourceKey<Noise> createKey(String name) {
		return PresetNoiseData.createKey("strata/" + name);
	}
}
