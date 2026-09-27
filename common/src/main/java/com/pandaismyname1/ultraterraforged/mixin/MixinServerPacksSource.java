package com.pandaismyname1.ultraterraforged.mixin;

import java.nio.file.Path;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;
import com.pandaismyname1.ultraterraforged.server.ServerPresets;

@Mixin(ServerPacksSource.class)
class MixinServerPacksSource {

	// a world's datapacks folder is set up here before its packs are loaded, which is where a new server world gets its preset
	@Inject(
		method = "createPackRepository(Ljava/nio/file/Path;Lnet/minecraft/world/level/validation/DirectoryValidator;)Lnet/minecraft/server/packs/repository/PackRepository;",
		at = @At("HEAD")
	)
	private static void installServerPreset(Path path, DirectoryValidator validator, CallbackInfoReturnable<PackRepository> callback) {
		if (ModLoaderUtil.isDedicatedServer()) {
			ServerPresets.installIfNewWorld(path);
		}
	}
}
