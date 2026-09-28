package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRule;

// since 26.3 isValidBiome is an instance method of Structure.GenerationContext (it was static on Structure)
@Mixin(GenerationContext.class)
public class MixinStructure {

	@Inject(
		at = @At("HEAD"), 
		method = "isValidBiome",
		cancellable = true
	)
    private void isValidBiome(GenerationStub generationStub, CallbackInfoReturnable<Boolean> callback) {
		GenerationContext generationContext = (GenerationContext) (Object) this;
		RegistryAccess registry = generationContext.registryAccess();
		RegistryLookup<StructureRule> structureRules = registry.lookupOrThrow(UTFRegistries.STRUCTURE_RULE);
		
		for(StructureRule structureRule : structureRules.listElements().map(Holder::value).toList()) {
			if(!structureRule.test(generationContext.randomState(), generationStub.position())) {
				callback.setReturnValue(false);
			}
		}
    }
}
