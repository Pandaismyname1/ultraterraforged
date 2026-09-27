package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;

record BiomeTagCondition(TagKey<Biome> tag) implements SurfaceRules.ConditionSource {
	public static final Codec<BiomeTagCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		TagKey.hashedCodec(Registries.BIOME).fieldOf("tag").forGetter(BiomeTagCondition::tag)
	).apply(instance, BiomeTagCondition::new));
	
	@SuppressWarnings("unchecked")
	@Override
	public SurfaceRules.Condition apply(Context ctx) {
		if((Object) ctx.randomState instanceof UTFRandomState utfRandomState) {
			RegistryLookup<Biome> registry = utfRandomState.registryAccess().lookupOrThrow(Registries.BIOME);
			return SurfaceRules.isBiome(registry.getOrThrow(this.tag)
				.stream()
				.map((holder) -> holder.unwrapKey().orElseThrow())
				.toArray(ResourceKey[]::new)
			).apply(ctx);
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public KeyDispatchDataCodec<BiomeTagCondition> codec() {
		return new KeyDispatchDataCodec<>(CODEC);
	}
}
