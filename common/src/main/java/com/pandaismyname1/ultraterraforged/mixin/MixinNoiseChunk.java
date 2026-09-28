package com.pandaismyname1.ultraterraforged.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.SectionPos;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.CellSampler;

@Mixin(NoiseChunk.class)
class MixinNoiseChunk {
	@Shadow
	@Final
	private RandomState randomState;
	@Shadow
	@Final
	private DensityVolume volume;
	@Shadow
	@Final
	@Mutable
	private Aquifer aquifer;

	// the chunk's cells for its density functions to read (CellSampler), as vanilla gives them the chunk's beardifier;
	// the random state and volume are set by then
	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/context/ContextMap$Builder;build()Lnet/minecraft/util/context/ContextMap;"
		),
		method = "<init>",
		require = 1
	)
	private ContextMap ultraterraforged$samplerFields(ContextMap.Builder fields) {
		GeneratorContext generatorContext;
		if((Object) this.randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			// a single column (a structure asking for the height) only loads the tile under fast lookups' rules
			boolean cache = !WorldGenFlags.fastLookups() || this.volume.sizeX() > 1 || this.volume.sizeZ() > 1;
			if(cache) {
				int chunkX = SectionPos.blockToSectionCoord(this.volume.minBlockX());
				int chunkZ = SectionPos.blockToSectionCoord(this.volume.minBlockZ());
				fields.set(CellSampler.CHUNK, new CellSampler.ChunkCells(generatorContext.cache.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ), chunkX, chunkZ));
			}
		}
		return fields.build();
	}

	// The volume ends at the chunk's highest ground (MixinNoiseBasedChunkGenerator), but carvers in the same chunk ask
	// the aquifer about blocks up to the build limit; above the volume there's only air, as the disabled aquifer says.
	@Inject(
		method = "<init>",
		at = @At("TAIL")
	)
	private void ultraterraforged$init(RandomState randomState, @Nullable Beardifier beardifier, NoiseGeneratorSettings noiseGeneratorSettings, Aquifer.FluidPicker fluidPicker, Blender blender, DensityVolume volume, CallbackInfo callback) {
		if((Object) randomState instanceof UTFRandomState utfRandomState && utfRandomState.generatorContext() != null) {
			this.aquifer = new VolumeAquifer(this.aquifer, Aquifer.createDisabled(fluidPicker), volume.maxBlockY());
		}
	}

	// lava below the preset's lava level instead of vanilla's -54
	@ModifyVariable(
		method = "<init>",
		at = @At("HEAD"),
		ordinal = 0,
		argsOnly = true
	)
	private static Aquifer.FluidPicker modifyFluidPicker(Aquifer.FluidPicker fluidPicker, RandomState randomState, @Nullable Beardifier beardifier, NoiseGeneratorSettings noiseGeneratorSettings) {
		if((Object) randomState instanceof UTFRandomState utfRandomState) {
			@Nullable
			Preset preset = utfRandomState.preset();
			if(preset != null && utfRandomState.generatorContext() != null) {
				int lavaLevel = preset.world().properties.lavaLevel;
		        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(lavaLevel, Blocks.LAVA.defaultBlockState());
		        int seaLevel = noiseGeneratorSettings.seaLevel();
		        Aquifer.FluidStatus defaultFluid = new Aquifer.FluidStatus(seaLevel, noiseGeneratorSettings.defaultFluid());
		        return (x, y, z) -> {
		        	if (y < Math.min(lavaLevel, seaLevel)) {
		                return lava;
		            }
		            return defaultFluid;
		        };
			}
		}
		return fluidPicker;
	}

	private static class VolumeAquifer implements Aquifer {
		private final Aquifer inside;
		private final Aquifer above;
		private final int maxY;
		private Aquifer last;

		VolumeAquifer(Aquifer inside, Aquifer above, int maxY) {
			this.inside = inside;
			this.above = above;
			this.maxY = maxY;
			this.last = inside;
		}

		@Nullable
		@Override
		public BlockState computeSubstance(int blockX, int blockY, int blockZ, double density) {
			this.last = blockY > this.maxY ? this.above : this.inside;
			return this.last.computeSubstance(blockX, blockY, blockZ, density);
		}

		@Override
		public boolean shouldScheduleFluidUpdate() {
			return this.last.shouldScheduleFluidUpdate();
		}
	}
}
