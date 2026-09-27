package com.pandaismyname1.ultraterraforged.world.worldgen.noise.function;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import com.pandaismyname1.ultraterraforged.registries.RTFBuiltInRegistries;

public interface CurveFunction {
    public static final Codec<CurveFunction> CODEC = RTFBuiltInRegistries.CURVE_FUNCTION_TYPE.byNameCodec().dispatch(CurveFunction::codec, Function.identity());
	
	float apply(float f);
	
	Codec<? extends CurveFunction> codec();
}
