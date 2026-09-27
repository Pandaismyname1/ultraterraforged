package com.pandaismyname1.ultraterraforged.world.worldgen.heightproviders;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;

public class UTFHeightProviderTypes {
	public static final HeightProviderType<LegacyCarverHeight> LEGACY_CARVER = register("legacy_carver", LegacyCarverHeight.CODEC);
	
	public static void bootstrap() {
	}
	
	private static <T extends HeightProvider> HeightProviderType<T> register(String name, Codec<T> codec) {
		MapCodec<T> mapCodec = UTFCodecs.asMap(codec);
		HeightProviderType<T> type = () -> mapCodec;
		RegistryUtil.register(BuiltInRegistries.HEIGHT_PROVIDER_TYPE, name, type);
		return type;
	}
}
