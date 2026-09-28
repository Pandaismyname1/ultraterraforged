package com.pandaismyname1.ultraterraforged.mixin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;
import net.minecraft.world.level.levelgen.material.MaterialSystem;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.UTFMaterialContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.UTFSurfaceSystem;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.StrataStack;

@Mixin(MaterialSystem.class)
@Implements(@Interface(iface = UTFSurfaceSystem.class, prefix = UTFCommon.MOD_ID + "$UTFSurfaceSystem$"))
class MixinMaterialSystem {
	private static final Identifier STRATA_RANDOM = UTFCommon.location("strata");
	@Shadow
	@Final
	private PositionalRandomFactory noiseRandom;
	private RandomSource strataRandom;
	private Map<Identifier, List<StrataStack>> strata;

	// the rock layers' random comes from the world's positional random, as it did from the random state's before 26.3
	// (the material system is given that same random)
	@Inject(
		at = @At("TAIL"),
		method = "<init>"
	)
	private void ultraterraforged$init(CallbackInfo callback) {
		this.strataRandom = this.noiseRandom.fromHashOf(STRATA_RANDOM);
		this.strata = new ConcurrentHashMap<>();
	}

	public List<StrataStack> ultraterraforged$UTFSurfaceSystem$getOrCreateStrata(Identifier cacheId, Function<RandomSource, List<StrataStack>> factory) {
		return this.strata.computeIfAbsent(cacheId, (k) -> {
			return factory.apply(this.strataRandom.fork());
		});
	}

	@Inject(at = @At("HEAD"), method = "buildSurface")
	private void buildSurface$HEAD(RandomState randomState, BiomeManager biomeManager, WorldGenerationContext generationContext, ChunkAccess protoChunk, NoiseChunk noiseChunk, MaterialRule ruleSource, @Nullable Set<Holder<Biome>> possibleBiomes, CallbackInfo callback) {
		UTFMaterialContext.setChunk(protoChunk);
	}

	@Inject(at = @At("RETURN"), method = "buildSurface")
	private void buildSurface$RETURN(RandomState randomState, BiomeManager biomeManager, WorldGenerationContext generationContext, ChunkAccess protoChunk, NoiseChunk noiseChunk, MaterialRule ruleSource, @Nullable Set<Holder<Biome>> possibleBiomes, CallbackInfo callback) {
		UTFMaterialContext.setChunk(null);
	}

	// the grass put back over carved-out dirt
	@Inject(at = @At("HEAD"), method = "topMaterial")
	private void topMaterial$HEAD(MaterialRule ruleSource, RandomState randomState, WorldGenerationContext worldGenerationContext, Function<BlockPos, Holder<Biome>> biomeGetter, ChunkAccess chunk, DensitySamplerSet densitySamplers, BlockPos pos, boolean underFluid, CallbackInfoReturnable<Optional<BlockState>> callback) {
		UTFMaterialContext.setChunk(chunk);
	}

	@Inject(at = @At("RETURN"), method = "topMaterial")
	private void topMaterial$RETURN(MaterialRule ruleSource, RandomState randomState, WorldGenerationContext worldGenerationContext, Function<BlockPos, Holder<Biome>> biomeGetter, ChunkAccess chunk, DensitySamplerSet densitySamplers, BlockPos pos, boolean underFluid, CallbackInfoReturnable<Optional<BlockState>> callback) {
		UTFMaterialContext.setChunk(null);
	}
}
