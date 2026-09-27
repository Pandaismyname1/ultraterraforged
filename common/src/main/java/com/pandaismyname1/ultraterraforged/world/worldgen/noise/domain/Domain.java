package com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;

import com.mojang.serialization.Codec;

import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public interface Domain {
    public static final Codec<Domain> CODEC = UTFCodecs.dispatch(UTFBuiltInRegistries.DOMAIN_TYPE, Domain::codec);
	
    float getOffsetX(float x, float z, int seed);
    
    float getOffsetZ(float x, float z, int seed);
    
    Domain mapAll(Noise.Visitor visitor);
    
    Codec<? extends Domain> codec();

    default float getX(float x, float z, int seed) {
        return x + this.getOffsetX(x, z, seed);
    }
    
    default float getZ(float x, float z, int seed) {
        return z + this.getOffsetZ(x, z, seed);
    }
}
