package com.pandaismyname1.ultraterraforged.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

@Mixin(FabricDataGenerator.Pack.class)
public interface MixinFabricDataGenerator$Pack {
	
	@Invoker(value = "<init>", remap = false)
	public static FabricDataGenerator.Pack invokeNew(FabricDataGenerator fabricDataGenerator, boolean shouldRun, String name, FabricPackOutput output) {
		throw new UnsupportedOperationException();
	}
}
