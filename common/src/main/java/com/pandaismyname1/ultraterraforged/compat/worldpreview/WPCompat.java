package com.pandaismyname1.ultraterraforged.compat.worldpreview;

import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;

public class WPCompat {
	
	public static boolean isEnabled() {
		return ModLoaderUtil.isLoaded("world_preview");
	}
}
