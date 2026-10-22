package com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.RandomState;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;

public interface StructureRule {
    public static final Codec<StructureRule> CODEC = UTFBuiltInRegistries.STRUCTURE_RULE_TYPE.byNameCodec().dispatch(StructureRule::codec, Function.identity());

	// whether it has a say over where this structure goes
	boolean appliesTo(Holder<Structure> structure);

	boolean test(RandomState randomState, BlockPos pos);
	
	Codec<? extends StructureRule> codec();
}
