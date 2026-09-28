package com.pandaismyname1.ultraterraforged.data.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

public class PresetNoiseParameters {

	public static void bootstrap(Preset preset, BootstrapContext<NormalNoise> ctx) {
//		TODO
//		CaveSettings caveSettings = preset.caves();
//		CaveSettings.Pillar pillars = caveSettings.pillars;
//		
//		double pillarRarenessModifier = 1.0D;
//		double pillarThicknessModifier = 1.0D;
//		double caveLayerModifier = 1.0D;
//		double caveCheeseModifier = 1.0D;
//		
//        register(ctx, Noises.PILLAR, -7, 1.0 * pillars.multiplier, 1.0 * pillars.multiplier);
//        register(ctx, Noises.PILLAR_RARENESS, -8, 1.0 * pillarRarenessModifier);
//        register(ctx, Noises.PILLAR_THICKNESS, -8, 1.0 * pillarThicknessModifier);
//        register(ctx, Noises.CAVE_LAYER, -8, 1.0 * caveLayerModifier);
//        register(ctx, Noises.CAVE_CHEESE, -8, 
//        	0.0 * caveCheeseModifier, 
//        	0.0 * caveCheeseModifier, 
//        	2.0 * caveCheeseModifier, 
//        	1.0 * caveCheeseModifier, 
//        	2.0 * caveCheeseModifier, 
//        	1.0 * caveCheeseModifier, 
//        	0.0 * caveCheeseModifier, 
//        	2.0 * caveCheeseModifier, 
//        	0.0 * caveCheeseModifier
//        );
	}

    // same as vanilla's NoiseData.register (NormalNoise.createParity keeps the pre-26.3 octave/amplitude semantics),
    // so re-registered vanilla noises stay identical to vanilla's for the same seed
    private static void register(BootstrapContext<NormalNoise> bootstapContext, ResourceKey<NormalNoise> resourceKey, int firstOctave, double initialAmplitude, double ... amplitudes) {
        double[] allAmplitudes = new double[amplitudes.length + 1];
        allAmplitudes[0] = initialAmplitude;
        System.arraycopy(amplitudes, 0, allAmplitudes, 1, amplitudes.length);
        bootstapContext.register(resourceKey, NormalNoise.createParity(firstOctave, allAmplitudes));
    }
}
