package com.pandaismyname1.ultraterraforged.mixin.worldpreview;

import java.net.Proxy;
import java.nio.file.Path;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldOptions;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;

@Pseudo
@Mixin(targets = "caeruleusTait.world.preview.backend.worker.SampleUtils", remap = false)
public class SampleUtilsMixin {
	@Shadow 
	@Final
    private RandomState randomState;
	@Shadow
	@Final
	private RegistryAccess registryAccess;
	
	@Inject(
		at = @At("TAIL"),
		require = 1,
		target = @Desc(
			value = "<init>",
			args = { 
				MinecraftServer.class, BiomeSource.class, ChunkGenerator.class, WorldOptions.class, LevelStem.class, LevelHeightAccessor.class
			}
		)
	)
	private void SampleUtils$1(CallbackInfo callback) {
		this.initUTF();
	}

	@Inject(
		at = @At("TAIL"), 
		require = 1,
		target = @Desc(
			value = "<init>",
			args = { 
				BiomeSource.class, ChunkGenerator.class, LayeredRegistryAccess.class, WorldOptions.class, LevelStem.class, LevelHeightAccessor.class, WorldDataConfiguration.class, Proxy.class, Path.class 
			}
		)
	) 
	private void SampleUtils$2(CallbackInfo callback) {
		this.initUTF();
	}
	
	private void initUTF() {
		if((Object) this.randomState instanceof UTFRandomState utfRandomState) {
			utfRandomState.initialize(this.registryAccess);
			// the preview reads every section: its own world isn't culled, and no other is touched
			if(utfRandomState.generatorContext() != null) {
				utfRandomState.generatorContext().lookup.setCullNoiseSections(false);
			}
			
			UTFCommon.LOGGER.info("initialized utf data");
		} else {
			throw new IllegalStateException();
		}
	}
}
