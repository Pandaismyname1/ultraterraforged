package com.pandaismyname1.ultraterraforged;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBCompat;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBSurfaceRules;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.server.commands.UTFCommands;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifiers;
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.UTFDensityFunctions;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.UTFFeatures;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance.UTFChanceModifiers;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement.UTFPlacementModifiers;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.decorator.TemplateDecorators;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.placement.TemplatePlacements;
import com.pandaismyname1.ultraterraforged.world.worldgen.floatproviders.UTFFloatProviderTypes;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightproviders.UTFHeightProviderTypes;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domains;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.CurveFunctions;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRule;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRules;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition.UTFSurfaceConditions;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.LayeredSurfaceRule;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.UTFSurfaceRules;

public class UTFCommon {
	public static final String MOD_ID = "ultraterraforged";
	public static final String LEGACY_MOD_ID = "terraforged";
	public static final Logger LOGGER = LogManager.getLogger("UltraTerraForged");

	public static void bootstrap() {
		UTFBuiltInRegistries.bootstrap();
		TemplatePlacements.bootstrap();
		TemplateDecorators.bootstrap();
		UTFChanceModifiers.bootstrap();
		UTFPlacementModifiers.bootstrap();
		UTFDensityFunctions.bootstrap();
		Noises.bootstrap();
		Domains.bootstrap();
		CurveFunctions.bootstrap();
		UTFFeatures.bootstrap();
		UTFHeightProviderTypes.bootstrap();
		UTFFloatProviderTypes.bootstrap();
		UTFSurfaceRules.bootstrap();
		UTFSurfaceConditions.bootstrap();
		BiomeModifiers.bootstrap();
		StructureRules.bootstrap();

		UTFCommands.bootstrap();
		
		if(TBCompat.isEnabled()) {
			TBCompat.bootstrap();
		}
		
		RegistryUtil.createDataRegistry(UTFRegistries.NOISE, Noise.DIRECT_CODEC);
		RegistryUtil.createDataRegistry(UTFRegistries.BIOME_MODIFIER, BiomeModifier.CODEC);
		RegistryUtil.createDataRegistry(UTFRegistries.STRUCTURE_RULE, StructureRule.CODEC);
		RegistryUtil.createDataRegistry(UTFRegistries.SURFACE_LAYERS, LayeredSurfaceRule.Layer.CODEC);
		RegistryUtil.createDataRegistry(UTFRegistries.PRESET, Preset.CODEC);
	}
	
	public static ResourceLocation location(String name) {
		if (name.contains(":")) return new ResourceLocation(name);
		return new ResourceLocation(UTFCommon.MOD_ID, name);
	}
}
