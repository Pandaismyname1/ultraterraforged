package com.pandaismyname1.ultraterraforged.world.worldgen.noise.function;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;

public interface CurveFunction {
    public static final Codec<CurveFunction> CODEC = UTFBuiltInRegistries.CURVE_FUNCTION_TYPE.byNameCodec().dispatch(CurveFunction::codec, Function.identity());
	
	float apply(float f);
	
	Codec<? extends CurveFunction> codec();
}
