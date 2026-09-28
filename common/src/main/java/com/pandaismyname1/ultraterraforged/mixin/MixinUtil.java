package com.pandaismyname1.ultraterraforged.mixin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.Util;
import com.pandaismyname1.ultraterraforged.concurrent.ThreadPools;
import com.pandaismyname1.ultraterraforged.concurrent.cache.Cache;

@Mixin(Util.class)
public class MixinUtil {

	@Inject(method = "shutdownExecutors()V", at = @At("TAIL"))
	private static void shutdownExecutors(CallbackInfo callback) {
		ultraterraforged$shutdown(ThreadPools.WORLD_GEN);
		ultraterraforged$shutdown(Cache.SCHEDULER);
	}

	// as Util.shutdownExecutor did before 26.1 removed it
	@Unique
	private static void ultraterraforged$shutdown(ExecutorService executor) {
		executor.shutdown();
		boolean terminated;
		try {
			terminated = executor.awaitTermination(3L, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			terminated = false;
		}
		if (!terminated) {
			executor.shutdownNow();
		}
	}
}
