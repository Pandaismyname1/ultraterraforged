package com.pandaismyname1.ultraterraforged.registries;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance.ChanceModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.decorator.TemplateDecorator;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.placement.TemplatePlacement;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domain;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.CurveFunction;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRule;

public class UTFBuiltInRegistries {
	public static final Registry<Codec<? extends Noise>> NOISE_TYPE = RegistryUtil.createRegistry(UTFRegistries.NOISE_TYPE);
	public static final Registry<Codec<? extends Domain>> DOMAIN_TYPE = RegistryUtil.createRegistry(UTFRegistries.DOMAIN_TYPE);
	public static final Registry<Codec<? extends CurveFunction>> CURVE_FUNCTION_TYPE = RegistryUtil.createRegistry(UTFRegistries.CURVE_FUNCTION_TYPE);
	public static final Registry<Codec<? extends ChanceModifier>> CHANCE_MODIFIER_TYPE = RegistryUtil.createRegistry(UTFRegistries.CHANCE_MODIFIER_TYPE);
	public static final Registry<Codec<? extends TemplatePlacement<?>>> TEMPLATE_PLACEMENT_TYPE = RegistryUtil.createRegistry(UTFRegistries.TEMPLATE_PLACEMENT_TYPE);
	public static final Registry<Codec<? extends TemplateDecorator<?>>> TEMPLATE_DECORATOR_TYPE = RegistryUtil.createRegistry(UTFRegistries.TEMPLATE_DECORATOR_TYPE);
	public static final Registry<Codec<? extends BiomeModifier>> BIOME_MODIFIER_TYPE = RegistryUtil.createRegistry(UTFRegistries.BIOME_MODIFIER_TYPE);
	public static final Registry<Codec<? extends StructureRule>> STRUCTURE_RULE_TYPE = RegistryUtil.createRegistry(UTFRegistries.STRUCTURE_RULE_TYPE);

	public static void bootstrap() {
	}
}
