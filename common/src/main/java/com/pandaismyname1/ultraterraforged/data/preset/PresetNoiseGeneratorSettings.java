package com.pandaismyname1.ultraterraforged.data.preset;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public class PresetNoiseGeneratorSettings {

	public static void bootstrap(Preset preset, BootstrapContext<NoiseGeneratorSettings> ctx) {
		HolderGetter<DensityFunction> densityFunctions = ctx.lookup(Registries.DENSITY_FUNCTION);
		HolderGetter<NormalNoise> noiseParams = ctx.lookup(Registries.NOISE);
		HolderGetter<Noise> noises = ctx.lookup(UTFRegistries.NOISE);

		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		int worldHeight = properties.worldHeight;
		int worldDepth = properties.worldDepth;

		NoiseRouter router = PresetNoiseRouterData.overworld(densityFunctions);
		ctx.register(NoiseGeneratorSettings.OVERWORLD, new NoiseGeneratorSettings(
			NoiseSettings.create(-worldDepth, worldDepth + worldHeight),
			Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(),
			router,
			// the material rule is the preset's own, kept in the noise settings rather than registered by name
			Holder.direct(PresetSurfaceRuleData.overworld(ctx.lookup(Registries.BIOME), densityFunctions, preset, noises)),
			properties.spawnType.getTargetPoints(densityFunctions),
			properties.seaLevel,
			false,
			Optional.of(PresetNoiseRouterData.overworldAquifers(densityFunctions, noiseParams)),
			false,
			new NoiseGeneratorSettings.DebugFunctions(List.of(
				new NoiseGeneratorSettings.DebugFunctionEntry("N", router.finalDensity()),
				new NoiseGeneratorSettings.DebugFunctionEntry("T", router.temperature()),
				new NoiseGeneratorSettings.DebugFunctionEntry("V", router.vegetation()),
				new NoiseGeneratorSettings.DebugFunctionEntry("C", router.continents()),
				new NoiseGeneratorSettings.DebugFunctionEntry("E", router.erosion()),
				new NoiseGeneratorSettings.DebugFunctionEntry("D", router.depth()),
				new NoiseGeneratorSettings.DebugFunctionEntry("W", router.ridges()),
				new NoiseGeneratorSettings.DebugFunctionEntry("PS", NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.preliminarySurfaceLevel()))
			))
		));
    }
}
