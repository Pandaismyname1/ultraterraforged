package com.pandaismyname1.ultraterraforged.mixin.terrablender;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBTargetPoint;

@Mixin(Climate.TargetPoint.class)
@Implements(@Interface(iface = TBTargetPoint.class, prefix = "ultraterraforged$TBTargetPoint$"))
class MixinTargetPoint {
	private double uniqueness = Double.NaN;

	public double ultraterraforged$TBTargetPoint$getUniqueness() {
		return this.uniqueness;
	}
	
	public void ultraterraforged$TBTargetPoint$setUniqueness(double uniqueness) {
		this.uniqueness = uniqueness;
	}
}
