package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.MaterialSystem;

// what UltraTerraForged's material rules need from the context and it keeps private: the world's generator is reached
// through the random state, and the rock layers are cached on the material system
@Mixin(MaterialRuleContext.class)
public interface MaterialRuleContextAccessor {

	@Accessor("randomState")
	RandomState ultraterraforged$randomState();

	@Accessor("system")
	MaterialSystem ultraterraforged$system();
}
