package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.level.levelgen.RandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFCompileContext;

// the anonymous CompileContext RandomState compiles its density functions with; javac names it RandomState$1 and keeps
// the outer instance in this$0, as it uses it
@Mixin(targets = "net.minecraft.world.level.levelgen.RandomState$1")
abstract class MixinRandomStateCompileContext implements UTFCompileContext {
	@Shadow
	@Final
	RandomState this$0;

	@Override
	public RandomState ultraterraforged$randomState() {
		return this.this$0;
	}
}
