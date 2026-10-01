package com.pandaismyname1.ultraterraforged.mixin; 

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.google.common.base.Suppliers;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBClimateSampler;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBCompat;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBNoiseRouterData;
import com.pandaismyname1.ultraterraforged.concurrent.ThreadPools;
import com.pandaismyname1.ultraterraforged.concurrent.cache.CacheManager;
import com.pandaismyname1.ultraterraforged.config.PerformanceConfig;
import com.pandaismyname1.ultraterraforged.data.preset.PresetData;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.CellSampler;
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.NoiseSampler;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;

@Mixin(RandomState.class)
@Implements(@Interface(iface = UTFRandomState.class, prefix = "ultraterraforged$UTFRandomState$"))
class MixinRandomState {
	@Shadow
	@Final
	private Climate.Sampler sampler;
	@Shadow
	@Final
    private SurfaceSystem surfaceSystem;
	
	@Nullable
	private Preset preset;
	@Nullable
	private NoiseSampler globalSampler;
	@Nullable
	private RegistryAccess registryAccess;
	
	@Deprecated
	private boolean hasContext;
	@Deprecated
	@Nullable
	private GeneratorContext generatorContext;
	
	private long seed;
	private DensityFunction.Visitor densityFunctionWrapper;
	
	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
		),
		method = "<init>",
		require = 1
	)
	private NoiseRouter RandomState(NoiseRouter router, DensityFunction.Visitor visitor, NoiseGeneratorSettings noiseGeneratorSettings, HolderGetter<NormalNoise.NoiseParameters> params, final long seed) {
		this.seed = seed;
		this.densityFunctionWrapper = new DensityFunction.Visitor() {
			
			@Override
			public DensityFunction apply(DensityFunction function) {
				if(function instanceof NoiseSampler.Marker marker) {
					return new NoiseSampler(marker.noise(), (int) seed);
				}
				if(function instanceof CellSampler.Marker marker) {
					MixinRandomState.this.hasContext |= true;
					return new CellSampler(Suppliers.memoize(() -> MixinRandomState.this.generatorContext), marker.field());
				}
				return visitor.apply(function);
			}

			@Override
			public NoiseHolder visitNoise(NoiseHolder noiseHolder) {
	            return visitor.visitNoise(noiseHolder);
	        }
		};
		return router.mapAll(this.densityFunctionWrapper);
	}

	public void ultraterraforged$UTFRandomState$initialize(RegistryAccess registryAccess) {
		this.registryAccess = registryAccess;
		
		RegistryLookup<Preset> presets = registryAccess.lookupOrThrow(UTFRegistries.PRESET);
		RegistryLookup<DensityFunction> functions = registryAccess.lookupOrThrow(Registries.DENSITY_FUNCTION);
		
		// only in a world whose terrain reads the cells: mapping the uniqueness, a cell too, would count any other
		// dimension TerraBlender places biomes in, such as the Nether, as UltraTerraForged's and generate it as one
		if((Object) this.sampler instanceof TBClimateSampler tbClimateSampler && TBCompat.isEnabled() && this.hasContext) {
			functions.get(TBNoiseRouterData.UNIQUENESS).ifPresent((uniqueness) -> {
				tbClimateSampler.setUniqueness(uniqueness.value().mapAll(this.densityFunctionWrapper));
			});
		}
		
		presets.get(PresetData.PRESET).ifPresent((presetHolder) -> {
			this.preset = presetHolder.value();

			if(this.hasContext) {
				//TODO move this somewhere else
				CacheManager.clear();
				
				PerformanceConfig config = PerformanceConfig.read(PerformanceConfig.DEFAULT_FILE_PATH)
					.resultOrPartial(UTFCommon.LOGGER::error)
					.orElseGet(PerformanceConfig::makeDefault);
				this.generatorContext = GeneratorContext.makeCached(this.preset, (int) this.seed, config.tileSize(), config.batchCount(), ThreadPools.availableProcessors() > 4);
			}
		});
	}
	
	public boolean ultraterraforged$UTFRandomState$usesCells() {
		return this.hasContext;
	}

	@Nullable
	public RegistryAccess ultraterraforged$UTFRandomState$registryAccess() {
		return this.registryAccess;
	}
	
	@Nullable
	public Preset ultraterraforged$UTFRandomState$preset() {
		return this.preset;
	}
	
	@Nullable
	public GeneratorContext ultraterraforged$UTFRandomState$generatorContext() {
		return this.generatorContext;
	}

	@Nullable
	public DensityFunction ultraterraforged$UTFRandomState$wrap(DensityFunction function) {
		return function.mapAll(this.densityFunctionWrapper);
	}

	public Noise ultraterraforged$UTFRandomState$wrap(Noise noise) {
		return Noises.shiftSeed(noise, (int) this.seed);
	}
}
