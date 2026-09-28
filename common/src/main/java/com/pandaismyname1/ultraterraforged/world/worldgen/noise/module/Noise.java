package com.pandaismyname1.ultraterraforged.world.worldgen.noise.module;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.codec.RegistryCodecs;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;

public interface Noise {
    public static final Codec<Noise> DIRECT_CODEC = Noises.DIRECT_CODEC;
    public static final Codec<Holder<Noise>> CODEC = RegistryCodecs.holder(UTFRegistries.NOISE, DIRECT_CODEC);
    public static final Codec<Noise> HOLDER_HELPER_CODEC = CODEC.xmap(Noises.HolderHolder::new, noise -> {
        if (noise instanceof Noises.HolderHolder holderHolder) {
            return holderHolder.holder();
        }
        return Holder.direct(noise);
    });
    
	float compute(float x, float z, int seed);
	
	float minValue();
	
	float maxValue();
	
	Noise mapAll(Visitor visitor);
	
	Codec<? extends Noise> codec();
	
	public interface Visitor {
		Noise apply(Noise input);
	}
}
