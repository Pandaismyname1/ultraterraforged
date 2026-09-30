package com.pandaismyname1.ultraterraforged.compat.c2me;

import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;

public class C2MECompat {

	// C2ME OpenCL: the Fabric mod's id, and the NeoForge build's nested mod, spelled with underscores
	public static boolean isOpenCLLoaded() {
		return ModLoaderUtil.isLoaded("c2me-opts-accel-opencl") || ModLoaderUtil.isLoaded("c2me_opts_accel_opencl");
	}
}
