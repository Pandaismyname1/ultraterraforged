package com.pandaismyname1.ultraterraforged.compat.alexscaves;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;

/**
 * Alex's Caves places its cave biomes in cells of its own, each kept or dropped by the climate at the cell's centre,
 * with vanilla's continentalness in mind: primordial caves from 0.4 up, toxic caves and the candy cavity from 0.5.
 * UltraTerraForged's land reaches such values far less often (over five seeds, 6% of the world is at 0.4 or more
 * against vanilla's 14%, 3.5% at 0.5 against 8.5%), so in its worlds most of those cells were dropped (#7). Inland,
 * Alex's Caves now gets the continentalness vanilla has over the same share of the world: the most inland places get
 * vanilla's most inland values. The sea keeps its own, as the abyssal chasm's -0.95 to -0.65 already covers as much
 * of it as in vanilla (2.7% against 2.9%). Ranges set in Alex's Caves' own config keep their meaning. After Mikaaah's
 * Terraforged-Alexs-Caves-Fix, which found the causes.
 *
 * This class mentions none of Alex's Caves' classes, so it can be loaded without it.
 */
public class AlexsCavesCompat {
	public static final String MOD_ID = "alexscaves";

	// continentalness from the coast inland in UltraTerraForged's Default preset, and vanilla's at the same share of the
	// world, every 2% of it: five seeds, 32000 blocks square each
	private static final float[] TERRAIN = { -0.190F, -0.159F, -0.073F, -0.044F, -0.018F, 0.007F, 0.031F, 0.045F, 0.061F, 0.079F, 0.097F, 0.116F, 0.136F, 0.156F, 0.177F, 0.199F, 0.223F, 0.249F, 0.278F, 0.317F, 0.376F, 0.453F, 0.558F, 0.740F, 1.000F };
	private static final float[] VANILLA = { 0.042F, 0.060F, 0.079F, 0.097F, 0.116F, 0.134F, 0.154F, 0.174F, 0.194F, 0.215F, 0.236F, 0.259F, 0.282F, 0.306F, 0.332F, 0.360F, 0.390F, 0.422F, 0.458F, 0.500F, 0.550F, 0.615F, 0.703F, 0.900F, 1.000F };

	public static boolean isEnabled() {
		return ModLoaderUtil.isLoaded(MOD_ID);
	}

	// the climate Alex's Caves reads at the centre of one of its cells
	public static Climate.TargetPoint atCentre(Climate.Sampler sampler, Climate.TargetPoint point) {
		if (!((ACClimateSampler) (Object) sampler).ultraterraforged$isTerrain()) {
			return point;
		}
		float continentalness = continentalness(Climate.unquantizeCoord(point.continentalness()));
		return new Climate.TargetPoint(point.temperature(), point.humidity(), Climate.quantizeCoord(continentalness), point.erosion(), point.depth(), point.weirdness());
	}

	// vanilla's continentalness at the share of the world where UltraTerraForged's is this; the sea's as it is
	public static float continentalness(float terrain) {
		if (terrain < TERRAIN[0]) {
			return terrain;
		}
		for (int i = 1; i < TERRAIN.length; i++) {
			if (terrain <= TERRAIN[i]) {
				float alpha = (terrain - TERRAIN[i - 1]) / (TERRAIN[i] - TERRAIN[i - 1]);
				return VANILLA[i - 1] + alpha * (VANILLA[i] - VANILLA[i - 1]);
			}
		}
		return VANILLA[VANILLA.length - 1];
	}

	// whether a biome is one of Alex's Caves', which UltraTerraForged's own caves stay out of
	public static boolean isCaveBiome(Holder<Biome> biome) {
		return biome.unwrapKey().map((key) -> key.location().getNamespace().equals(MOD_ID)).orElse(false);
	}
}
