package com.pandaismyname1.ultraterraforged.world.worldgen.biome;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;

public interface BiomeParameter {
	float min();
	
	float max();
	
	default float lerp(float alpha) {
		return NoiseUtil.lerp(this.min(), this.max(), alpha);
	}
	
	default float midpoint() {
		return (this.min() + this.max()) / 2.0F;
	}
	
	default Noise source() {
		return Noises.constant(this.midpoint());
	}
}
