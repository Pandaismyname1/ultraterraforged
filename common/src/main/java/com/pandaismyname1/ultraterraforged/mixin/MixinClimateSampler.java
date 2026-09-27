package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.UTFClimateSampler;

@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = UTFClimateSampler.class, prefix = "ultraterraforged$UTFClimateSampler$"))
class MixinClimateSampler {
	private BlockPos spawnSearchCenter = BlockPos.ZERO;
	
	public void ultraterraforged$UTFClimateSampler$setSpawnSearchCenter(BlockPos spawnSearchCenter) {
		this.spawnSearchCenter = spawnSearchCenter;
	}
	
	public BlockPos ultraterraforged$UTFClimateSampler$getSpawnSearchCenter() {
		return this.spawnSearchCenter;
	}
}
