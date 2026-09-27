package com.pandaismyname1.ultraterraforged.mixin;

import java.util.Set;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.UTFSurfaceContext;

@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$BiomeConditionSource")
public class MixinSurfaceRules$BiomeConditionSource {
	@Shadow
	@Final
    Predicate<ResourceKey<Biome>> biomeNameTest;

    @Inject(at = @At("HEAD"), method = "apply", cancellable = true)
    public void apply(SurfaceRules.Context ctx, CallbackInfoReturnable<SurfaceRules.Condition> callback) {
    	Set<ResourceKey<Biome>> surroundingBiomes;
    	if((Object) ctx instanceof UTFSurfaceContext utfSurfaceContext && (surroundingBiomes = utfSurfaceContext.getSurroundingBiomes()) != null) {
    		boolean result = surroundingBiomes.stream().filter(this.biomeNameTest).findAny().isPresent();
    		if(!result || surroundingBiomes.size() == 1) {
    			callback.setReturnValue(() -> result);
    		}
    	}
    }
}