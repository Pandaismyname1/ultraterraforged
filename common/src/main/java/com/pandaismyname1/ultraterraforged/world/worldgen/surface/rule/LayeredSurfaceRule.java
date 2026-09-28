package com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;

public record LayeredSurfaceRule(TagKey<Layer> layers) implements SurfaceRules.RuleSource {
	public static final Codec<LayeredSurfaceRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		TagKey.hashedCodec(UTFRegistries.SURFACE_LAYERS).fieldOf("layers").forGetter(LayeredSurfaceRule::layers)
	).apply(instance, LayeredSurfaceRule::new));
		
	@Override
	public SurfaceRules.SurfaceRule apply(Context ctx) {
		if((Object) ctx.randomState instanceof UTFRandomState utfRandomState) {
			RegistryLookup<Layer> layerLookup = utfRandomState.registryAccess().lookupOrThrow(UTFRegistries.SURFACE_LAYERS);
			return SurfaceRules.sequence(layerLookup.getOrThrow(this.layers).stream().map(Layer::unwrapRule).toArray(SurfaceRules.RuleSource[]::new)).apply(ctx);
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public MapCodec<LayeredSurfaceRule> codec() {
		return UTFCodecs.asMap(CODEC);
	}

	public static Layer layer(TagKey<Layer> layers) {
		return new Layer(UTFSurfaceRules.layered(layers));
	}
	
	public static Layer layer(SurfaceRules.RuleSource rule) {
		return new Layer(rule);
	}
	
	public record Layer(SurfaceRules.RuleSource rule) {
		public static final Codec<Layer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			SurfaceRules.RuleSource.CODEC.fieldOf("rule").forGetter(Layer::rule)
		).apply(instance, Layer::new));
		
		protected static SurfaceRules.RuleSource unwrapRule(Holder<Layer> layer) {
			return layer.value().rule();
		}
	}
}
