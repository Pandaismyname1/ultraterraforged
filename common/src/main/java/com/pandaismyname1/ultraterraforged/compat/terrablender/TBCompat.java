package com.pandaismyname1.ultraterraforged.compat.terrablender;

import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;
import terrablender.core.TerraBlender;

public class TBCompat {

	public static void bootstrap() {
		TBSurfaceRules.bootstrap();
	}
	
	public static boolean isEnabled() {
		return ModLoaderUtil.isLoaded(TerraBlender.MOD_ID);
	}
}
