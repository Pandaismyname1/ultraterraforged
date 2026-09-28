package com.pandaismyname1.ultraterraforged.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
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
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.UTFDensityFunctions;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;

/*
 * UltraTerraForged's density functions find the world's generator through the random state they're compiled for
 * (UTFCompileContext). The generator is made once the registries are known, when the chunk map is (MixinChunkMap), and
 * only for a dimension whose router reads UltraTerraForged's terrain.
 */
@Mixin(RandomState.class)
@Implements(@Interface(iface = UTFRandomState.class, prefix = "ultraterraforged$UTFRandomState$"))
class MixinRandomState {
	@Shadow
	@Final
	private long seed;
	@Shadow
	@Final
	private NoiseRouter router;

	@Nullable
	private Preset preset;
	@Nullable
	private RegistryAccess registryAccess;

	private boolean hasContext;
	@Nullable
	private GeneratorContext generatorContext;
	// UltraTerraForged's biome regions, which pick TerraBlender's region when it's installed
	@Nullable
	private DensityFunction uniqueness;

	// no arguments captured, so the constructor's signature can change between versions
	@Inject(
		at = @At("TAIL"),
		method = "<init>"
	)
	private void ultraterraforged$init(CallbackInfo callback) {
		this.hasContext = UTFDensityFunctions.usesCells(this.router);
	}

	public void ultraterraforged$UTFRandomState$initialize(RegistryAccess registryAccess) {
		this.registryAccess = registryAccess;

		RegistryLookup<Preset> presets = registryAccess.lookupOrThrow(UTFRegistries.PRESET);
		if(this.hasContext && TBCompat.isEnabled()) {
			this.uniqueness = registryAccess.lookupOrThrow(Registries.DENSITY_FUNCTION).get(TBNoiseRouterData.UNIQUENESS).map(Holder::value).orElse(null);
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

	@Inject(
		at = @At("RETURN"),
		method = "createClimateSampler"
	)
	private void createClimateSampler(SamplerContext context, CallbackInfoReturnable<Climate.Sampler> callback) {
		if(this.uniqueness != null && (Object) callback.getReturnValue() instanceof TBClimateSampler tbClimateSampler) {
			tbClimateSampler.setUniqueness(((RandomState) (Object) this).getSampler(this.uniqueness).bind(context));
		}
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

	public Noise ultraterraforged$UTFRandomState$wrap(Noise noise) {
		return Noises.shiftSeed(noise, (int) this.seed);
	}
}
