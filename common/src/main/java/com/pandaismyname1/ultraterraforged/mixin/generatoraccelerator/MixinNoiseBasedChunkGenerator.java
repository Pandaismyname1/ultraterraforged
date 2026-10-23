package com.pandaismyname1.ultraterraforged.mixin.generatoraccelerator;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;

// Generator Accelerator's column loop (bts$iterateNoiseColumn, and the array getBaseColumn fills with it) reads as many
// cells as UltraTerraForged's noise chunk has, as vanilla's does in MixinNoiseBasedChunkGenerator (GeneratorAcceleratorCompat).
// Its methods are merged into NoiseBasedChunkGenerator by its own mixins, so this one has to come after them.
@Mixin(value = NoiseBasedChunkGenerator.class, priority = 9001)
class MixinNoiseBasedChunkGenerator {

	@Shadow
	@Final
	private Holder<NoiseGeneratorSettings> settings;

	@Redirect(
		method = "bts$iterateNoiseColumn",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;height()I",
			remap = true
		),
		require = 0,
		remap = false
	)
	private int iterateNoiseColumn(NoiseSettings settings, LevelHeightAccessor levelHeightAccessor, RandomState randomState, int blockX, int blockZ, @Nullable BlockState[] states, @Nullable Predicate<BlockState> predicate) {
		return this.height(settings, randomState, blockX, blockZ);
	}

	@Redirect(
		method = "getBaseColumn",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;height()I"
		),
		require = 0
	)
	private int getBaseColumn(NoiseSettings settings, int blockX, int blockZ, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
		return this.height(settings, randomState, blockX, blockZ);
	}

	private int height(NoiseSettings settings, RandomState randomState, int blockX, int blockZ) {
		GeneratorContext generatorContext;
		if((Object) randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			return generatorContext.lookup.getGenerationHeight(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ), this.settings.value(), !WorldGenFlags.fastLookups());
		} else {
			return settings.height();
		}
	}
}
