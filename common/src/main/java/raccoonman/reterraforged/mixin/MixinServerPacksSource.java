package raccoonman.reterraforged.mixin;

import java.nio.file.Path;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Lists;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import raccoonman.reterraforged.data.packs.RTFBuiltinPackSource;
import raccoonman.reterraforged.platform.ModLoaderUtil;
import raccoonman.reterraforged.server.ServerPresets;

@Mixin(ServerPacksSource.class)
class MixinServerPacksSource {

	// a world's datapacks folder is set up here before its packs are loaded, which is where a new server world gets its preset
	@Inject(
		method = "createPackRepository(Ljava/nio/file/Path;)Lnet/minecraft/server/packs/repository/PackRepository;",
		at = @At("HEAD")
	)
	private static void installServerPreset(Path path, CallbackInfoReturnable<PackRepository> callback) {
		if (ModLoaderUtil.isDedicatedServer()) {
			ServerPresets.installIfNewWorld(path);
		}
	}
    
	@Redirect(
		method = "createPackRepository(Ljava/nio/file/Path;)Lnet/minecraft/server/packs/repository/PackRepository;",
		at = @At(
			value = "NEW",
			target = "Lnet/minecraft/server/packs/repository/PackRepository;"
		),
		require = 1
	)
    private static PackRepository createPackRepository(RepositorySource[] repositorySources, Path path) {
		List<RepositorySource> sourceList = Lists.newArrayList(repositorySources);
		sourceList.add(new RTFBuiltinPackSource());
    	return new PackRepository(sourceList.toArray(RepositorySource[]::new));
    }
}
