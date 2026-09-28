package com.pandaismyname1.ultraterraforged.mixin.terrablender;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.TargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBClimateSampler;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBTargetPoint;

// every climate sampler is made by RandomState.createClimateSampler, which gives it the uniqueness (MixinRandomState)
@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = TBClimateSampler.class, prefix = "ultraterraforged$TBClimateSampler$"))
class MixinClimateSampler {
	@Nullable
	private DensitySampler.Bound uniqueness;

	@Inject(
		at = @At("RETURN"),
		method = "sample"
	)
	public void sample(int quartX, int quartY, int quartZ, CallbackInfoReturnable<TargetPoint> callback) {
		if(this.uniqueness != null && (Object) callback.getReturnValue() instanceof TBTargetPoint tbTargetPoint) {
			tbTargetPoint.setUniqueness(this.uniqueness.sampleValue(QuartPos.toBlock(quartX), QuartPos.toBlock(quartY), QuartPos.toBlock(quartZ)));
		}
	}

	public void ultraterraforged$TBClimateSampler$setUniqueness(DensitySampler.Bound uniqueness) {
		this.uniqueness = uniqueness;
	}

	@Nullable
	public DensitySampler.Bound ultraterraforged$TBClimateSampler$getUniqueness() {
		return this.uniqueness;
	}
}
