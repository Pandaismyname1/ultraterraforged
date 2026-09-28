package com.pandaismyname1.ultraterraforged.mixin;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.spawn.SpawnFinder;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.RaisedWater;

@Mixin(value = NoiseBasedChunkGenerator.class, priority = 9001 /* we need this so we don't break noisium */)
class MixinNoiseBasedChunkGenerator {

	@Shadow
	@Final
    private Holder<NoiseGeneratorSettings> settings;

	// since 26.3 the surface is built in the terrain step, right after the noise fills the chunk; carvers follow it
	@Inject(at = @At("HEAD"), method = "buildSurface", require = 1)
    private void buildSurface$HEAD(ChunkAccess chunkAccess, NoiseChunk noiseChunk, RandomState randomState, BiomeManager biomeManager, Set<Holder<Biome>> possibleBiomes, MaterialRule materialRule, CallbackInfo callback) {
		GeneratorContext generatorContext;
		if((Object) randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			// the water of rivers and lakes above the sea, so the surface is built under it
			RaisedWater.fill(chunkAccess, generatorContext);
		}
    }

	@Inject(at = @At("TAIL"), method = "buildSurface", require = 1)
    private void buildSurface$TAIL(ChunkAccess chunkAccess, NoiseChunk noiseChunk, RandomState randomState, BiomeManager biomeManager, Set<Holder<Biome>> possibleBiomes, MaterialRule materialRule, CallbackInfo callback) {
		GeneratorContext generatorContext;
		if((Object) randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			// caves shaped by the land, carved into the finished surface
			generatorContext.caveFeatures.carve(chunkAccess);
		}
    }

	// noise is only filled in up to the highest ground of the chunk, and air above it
	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;chunkVolume(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/NoiseSettings;)Lnet/minecraft/world/level/levelgen/densityfunction/DensityVolume;"
		),
		method = "createNoiseChunk",
		require = 1
	)
    private DensityVolume chunkVolume(ChunkAccess chunk, NoiseSettings noiseSettings, ChunkAccess chunk2, StructureManager structureManager, Blender blender, RandomState randomState) {
		ChunkPos chunkPos = chunk.getPos();
		int height = noiseSettings.height();
		GeneratorContext generatorContext;
		if((Object) randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			height = Math.min(height, generatorContext.lookup.getGenerationHeight(chunkPos.x(), chunkPos.z(), this.settings.value(), true));
		}
		return new DensityVolume(16, height, 16, chunkPos.getMinBlockX(), noiseSettings.minY(), chunkPos.getMinBlockZ());
    }

	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;height()I"
		),
		require = 2,
		method = "iterateNoiseColumn"
	)
    private int iterateNoiseColumn(NoiseSettings settings, LevelHeightAccessor levelHeightAccessor, RandomState randomState, int blockX, int blockZ, @Nullable MutableObject<NoiseColumn> mutableObject, @Nullable Predicate<BlockState> predicate) {
		GeneratorContext generatorContext;
		if((Object) randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			return Math.min(settings.height(), generatorContext.lookup.getGenerationHeight(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ), this.settings.value(), !WorldGenFlags.fastLookups()));
    	} else {
    		return settings.height();
    	}
    }

	// the spawn is searched for around the spawn type's center (the middle of a continent) instead of the world origin
	@Inject(at = @At("HEAD"), method = "getOrigin", cancellable = true)
	private void getOrigin(RandomState randomState, CallbackInfoReturnable<ChunkPos> callback) {
		GeneratorContext generatorContext;
		Preset preset;
		if((Object) randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null && (preset = utfRandomState.preset()) != null) {
			BlockPos center = preset.world().properties.spawnType.getSearchCenter(generatorContext);
			List<SpawnTargetPoint> targetPoints = this.settings.value().spawnTarget();
			BlockPos spawn = targetPoints.isEmpty() ? center : SpawnFinder.findSpawnPosition(targetPoints, randomState.samplersWithContext(SamplerContext.builder().enableCaches().build()), center);
			callback.setReturnValue(ChunkPos.containing(spawn));
		}
	}

	@Inject(
		at = @At("TAIL"),
		method = "addDebugScreenInfo"
	)
    private void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos, SamplerContext samplerContext, CallbackInfo callback) {
		@Nullable
		GeneratorContext generatorContext;
		if((Object) randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			Cell cell = new Cell();
			generatorContext.lookup.apply(cell, blockPos.getX(), blockPos.getZ());

			WorldSettings worldSettings = generatorContext.preset.world();
			WorldSettings.ControlPoints controlPoints = worldSettings.controlPoints;

			list.add("");
			list.add("Terrain Type: " + cell.terrain.getName());
			list.add("Terrain Region: " + cell.terrainRegionEdge);
			list.add("Terrain Mask: " + cell.terrainMask);
			list.add("Continent Edge: " + cell.continentEdge);
			list.add("Ground Variance: " + NoiseUtil.map(cell.continentNoise, controlPoints.coast, controlPoints.farInland));
			list.add("River Distance: " + cell.riverDistance);
			list.add("Mountain Chain: " + cell.mountainChainAlpha);
			list.add("Biome Type: " + cell.biomeType.name());
			list.add("Macro Biome: " + cell.macroBiomeId);
			list.add("Temperature: " + cell.regionTemperature);
			list.add("Moisture: " + cell.regionMoisture);
			list.add("");
    	}
    }
}
