package com.pandaismyname1.ultraterraforged.world.worldgen.floatproviders;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;

// since 1.21.2 a float provider's type is its MapCodec
public class UTFFloatProviderTypes {
	
	public static void bootstrap() {
		RegistryUtil.register(BuiltInRegistries.FLOAT_PROVIDER_TYPE, "legacy_canyon_y_scale", LegacyCanyonYScale.CODEC);
	}
}
