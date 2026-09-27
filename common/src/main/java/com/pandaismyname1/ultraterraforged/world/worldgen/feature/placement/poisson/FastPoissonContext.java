package com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement.poisson;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public record FastPoissonContext(int radius, float jitter, float frequency, Noise density) {

	public FastPoissonContext {
    	frequency = Math.min(0.5F, frequency);
    	jitter = NoiseUtil.clamp(jitter, 0.0F, 1.0F);
    }
    
    public float scale() {
    	return 1.0F / this.frequency;
    }
    
    public int radiusSq() {
    	return this.radius * this.radius;
    }
    
    public float pad() {
    	return (1.0F - this.jitter) * 0.5F;
    }
}
