package com.pandaismyname1.ultraterraforged.world.worldgen.heightmap;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public record RegionConfig(int seed, int scale, Noise warpX, Noise warpZ, float warpStrength) {
}
