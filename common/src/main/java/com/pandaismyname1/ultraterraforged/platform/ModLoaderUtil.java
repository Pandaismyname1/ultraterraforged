package com.pandaismyname1.ultraterraforged.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class ModLoaderUtil {
	
	@ExpectPlatform
	public static boolean isLoaded(String modId) {
		throw new IllegalStateException();
	}

	@ExpectPlatform
	public static boolean isDedicatedServer() {
		throw new IllegalStateException();
	}

	/**
	 * The mod loader running the game: "fabric", or "forge", which also covers NeoForge on 1.20.1.
	 */
	@ExpectPlatform
	public static String loaderName() {
		throw new IllegalStateException();
	}
}
