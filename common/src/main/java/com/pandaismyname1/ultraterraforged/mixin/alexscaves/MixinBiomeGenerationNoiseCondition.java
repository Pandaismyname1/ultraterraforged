package com.pandaismyname1.ultraterraforged.mixin.alexscaves;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.compat.alexscaves.AlexsCavesCompat;

/**
 * Alex's Caves keeps or drops each of its cave biome cells by the climate at the cell's centre: here that climate is
 * the one AlexsCavesCompat gives. Every version of Alex's Caves, and its ports, asks in this one method. It has two
 * overloads, of which a plain name would only find the first (which some versions never call): hence the pattern.
 */
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition", remap = false)
abstract class MixinBiomeGenerationNoiseCondition {

	@Redirect(method = "/^test$/", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Climate$Sampler;sample(III)Lnet/minecraft/world/level/biome/Climate$TargetPoint;", remap = true), require = 0)
	private Climate.TargetPoint sample(Climate.Sampler sampler, int x, int y, int z) {
		return AlexsCavesCompat.atCentre(sampler, sampler.sample(x, y, z));
	}
}
