package com.pandaismyname1.ultraterraforged.platform.neoforge;

import net.neoforged.fml.loading.FMLLoader;

public class ModLoaderUtilImpl {
	
	public static boolean isLoaded(String modId) {
		return FMLLoader.getCurrent().getLoadingModList().getModFileById(modId) != null;
	}

	public static boolean isDedicatedServer() {
		return FMLLoader.getCurrent().getDist().isDedicatedServer();
	}

	public static String loaderName() {
		return "neoforge";
	}
}
