package com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.UTFMaterialContext;

public record LayeredSurfaceRule(TagKey<Layer> layers) implements MaterialRule {
	public static final Codec<LayeredSurfaceRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		TagKey.hashedCodec(UTFRegistries.SURFACE_LAYERS).fieldOf("layers").forGetter(LayeredSurfaceRule::layers)
	).apply(instance, LayeredSurfaceRule::new));

	@Override
	public RuleEvaluator compile(MaterialRuleContext ctx) {
		UTFRandomState randomState = UTFMaterialContext.utfRandomState(ctx);
		RegistryAccess registryAccess;
		if(randomState == null || (registryAccess = randomState.registryAccess()) == null) {
			throw new IllegalStateException("Surface layers need UltraTerraForged's random state");
		}
		RegistryLookup<Layer> layerLookup = registryAccess.lookupOrThrow(UTFRegistries.SURFACE_LAYERS);
		return MaterialRules.sequence(layerLookup.getOrThrow(this.layers).stream().map(Layer::unwrapRule).toList()).compile(ctx);
	}

	@Override
	public MapCodec<LayeredSurfaceRule> codec() {
		return UTFCodecs.asMap(CODEC);
	}

	public static Layer layer(TagKey<Layer> layers) {
		return new Layer(UTFSurfaceRules.layered(layers));
	}

	public static Layer layer(MaterialRule rule) {
		return new Layer(rule);
	}

	public record Layer(MaterialRule rule) {
		public static final Codec<Layer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			MaterialRule.CODEC.fieldOf("rule").forGetter(Layer::rule)
		).apply(instance, Layer::new));

		protected static MaterialRule unwrapRule(Holder<Layer> layer) {
			return layer.value().rule();
		}
	}
}
