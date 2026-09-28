package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.condition.BiomeCondition;
import net.minecraft.world.level.levelgen.material.condition.ConditionEvaluator;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.UTFMaterialContext;

// vanilla's biome condition over the biomes in a tag, resolved against the world's registries
record BiomeTagCondition(TagKey<Biome> tag) implements MaterialCondition {
	public static final Codec<BiomeTagCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		TagKey.hashedCodec(Registries.BIOME).fieldOf("tag").forGetter(BiomeTagCondition::tag)
	).apply(instance, BiomeTagCondition::new));

	@Override
	public ConditionEvaluator compile(MaterialRuleContext ctx) {
		UTFRandomState randomState = UTFMaterialContext.utfRandomState(ctx);
		RegistryAccess registryAccess;
		if(randomState == null || (registryAccess = randomState.registryAccess()) == null) {
			throw new IllegalStateException("Biome tag conditions need UltraTerraForged's random state");
		}
		return new BiomeCondition(registryAccess.lookupOrThrow(Registries.BIOME).getOrThrow(this.tag)).compile(ctx);
	}

	@Override
	public MapCodec<BiomeTagCondition> codec() {
		return UTFCodecs.asMap(CODEC);
	}
}
