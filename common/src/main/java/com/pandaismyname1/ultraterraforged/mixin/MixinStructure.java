package com.pandaismyname1.ultraterraforged.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRule;

// each structure rule has its say only over the structures it's for; it once kept every structure of every mod away
// from rivers, Alex's Caves' underground chambers too (issue #7)
@Mixin(Structure.class)
public class MixinStructure {

	@Inject(
		at = @At("RETURN"),
		method = "findValidGenerationPoint",
		cancellable = true
	)
    private void findValidGenerationPoint(GenerationContext generationContext, CallbackInfoReturnable<Optional<GenerationStub>> callback) {
		Optional<GenerationStub> generationStub = callback.getReturnValue();
		if(generationStub.isEmpty()) {
			return;
		}
		RegistryAccess registry = generationContext.registryAccess();
		Holder<Structure> structure = registry.registryOrThrow(Registries.STRUCTURE).wrapAsHolder((Structure) (Object) this);
		for(StructureRule structureRule : registry.registryOrThrow(UTFRegistries.STRUCTURE_RULE)) {
			if(structureRule.appliesTo(structure) && !structureRule.test(generationContext.randomState(), generationStub.get().position())) {
				callback.setReturnValue(Optional.empty());
				return;
			}
		}
    }
}
