package com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;

public interface StructureRule {
    public static final Codec<StructureRule> CODEC = UTFBuiltInRegistries.STRUCTURE_RULE_TYPE.byNameCodec().dispatch(StructureRule::codec, Function.identity());

	boolean test(RandomState randomState, BlockPos pos);
	
	Codec<? extends StructureRule> codec();
}
