package com.pandaismyname1.ultraterraforged.platform.fabric;

import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;

public final class ConfigUtilImpl {

	public static Path getConfigPath() {
		return FabricLoader.getInstance().getConfigDir();
	}
}
