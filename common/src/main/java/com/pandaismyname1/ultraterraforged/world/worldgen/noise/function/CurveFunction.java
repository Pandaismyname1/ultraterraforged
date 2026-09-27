package com.pandaismyname1.ultraterraforged.world.worldgen.noise.function;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;

import com.mojang.serialization.Codec;

import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;

public interface CurveFunction {
    public static final Codec<CurveFunction> CODEC = UTFCodecs.dispatch(UTFBuiltInRegistries.CURVE_FUNCTION_TYPE, CurveFunction::codec);
	
	float apply(float f);
	
	Codec<? extends CurveFunction> codec();
}
