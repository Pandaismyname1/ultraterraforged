package com.pandaismyname1.ultraterraforged;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBCompat;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBSurfaceRules;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.registries.RTFBuiltInRegistries;
import com.pandaismyname1.ultraterraforged.registries.RTFRegistries;
import com.pandaismyname1.ultraterraforged.server.commands.RTFCommands;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifiers;
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.RTFDensityFunctions;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.RTFFeatures;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance.RTFChanceModifiers;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement.RTFPlacementModifiers;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.decorator.TemplateDecorators;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.placement.TemplatePlacements;
import com.pandaismyname1.ultraterraforged.world.worldgen.floatproviders.RTFFloatProviderTypes;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightproviders.RTFHeightProviderTypes;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domains;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.CurveFunctions;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRule;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRules;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition.RTFSurfaceConditions;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.LayeredSurfaceRule;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.RTFSurfaceRules;

public class RTFCommon {
	public static final String MOD_ID = "ultraterraforged";
	public static final String LEGACY_MOD_ID = "terraforged";
	public static final Logger LOGGER = LogManager.getLogger("UltraTerraForged");

	public static void bootstrap() {
		RTFBuiltInRegistries.bootstrap();
		TemplatePlacements.bootstrap();
		TemplateDecorators.bootstrap();
		RTFChanceModifiers.bootstrap();
		RTFPlacementModifiers.bootstrap();
		RTFDensityFunctions.bootstrap();
		Noises.bootstrap();
		Domains.bootstrap();
		CurveFunctions.bootstrap();
		RTFFeatures.bootstrap();
		RTFHeightProviderTypes.bootstrap();
		RTFFloatProviderTypes.bootstrap();
		RTFSurfaceRules.bootstrap();
		RTFSurfaceConditions.bootstrap();
		BiomeModifiers.bootstrap();
		StructureRules.bootstrap();

		RTFCommands.bootstrap();
		
		if(TBCompat.isEnabled()) {
			TBCompat.bootstrap();
		}
		
		RegistryUtil.createDataRegistry(RTFRegistries.NOISE, Noise.DIRECT_CODEC);
		RegistryUtil.createDataRegistry(RTFRegistries.BIOME_MODIFIER, BiomeModifier.CODEC);
		RegistryUtil.createDataRegistry(RTFRegistries.STRUCTURE_RULE, StructureRule.CODEC);
		RegistryUtil.createDataRegistry(RTFRegistries.SURFACE_LAYERS, LayeredSurfaceRule.Layer.CODEC);
		RegistryUtil.createDataRegistry(RTFRegistries.PRESET, Preset.CODEC);
	}
	
	public static ResourceLocation location(String name) {
		if (name.contains(":")) return new ResourceLocation(name);
		return new ResourceLocation(RTFCommon.MOD_ID, name);
	}
}
