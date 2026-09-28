package com.pandaismyname1.ultraterraforged.world.worldgen.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance.ChanceFeature;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.TemplateFeature;

// since 26.3 a feature is its own configuration, and what's registered here is the codec of each kind (feature_type);
// Registries.FEATURE is the datapack registry of the configured ones
public class UTFFeatures {
	public static final MapCodec<TemplateFeature<?>> TEMPLATE = register("template", TemplateFeature.CODEC);
	public static final MapCodec<BushFeature> BUSH = register("bush", BushFeature.CODEC);
	public static final MapCodec<DiskFeature> DISK = register("disk", DiskFeature.CODEC);
	public static final MapCodec<ChanceFeature> CHANCE = register("chance", ChanceFeature.CODEC);
	public static final MapCodec<ErodeSnowFeature> ERODE_SNOW = register("erode_snow", ErodeSnowFeature.CODEC);
	public static final MapCodec<SwampSurfaceFeature> SWAMP_SURFACE = register("swamp_surface", SwampSurfaceFeature.CODEC);

	public static void bootstrap() {
	}

	private static <F extends Feature> MapCodec<F> register(String name, MapCodec<F> codec) {
		RegistryUtil.register(BuiltInRegistries.FEATURE_TYPE, name, codec);
		return codec;
	}
}
