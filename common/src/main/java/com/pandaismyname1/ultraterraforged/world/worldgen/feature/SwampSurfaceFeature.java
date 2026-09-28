package com.pandaismyname1.ultraterraforged.world.worldgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;

public record SwampSurfaceFeature(BlockState clayMaterial, BlockState gravelMaterial, BlockState dirtMaterial) implements Feature {
	public static final MapCodec<SwampSurfaceFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		BlockState.CODEC.fieldOf("clay_material").forGetter(SwampSurfaceFeature::clayMaterial),
		BlockState.CODEC.fieldOf("gravel_material").forGetter(SwampSurfaceFeature::gravelMaterial),
		BlockState.CODEC.fieldOf("dirt_material").forGetter(SwampSurfaceFeature::dirtMaterial)
	).apply(instance, SwampSurfaceFeature::new));
	private static final Noise MATERIAL_NOISE = makeMaterialNoise();

	@Override
	public MapCodec<SwampSurfaceFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		ChunkAccess chunk = level.getChunk(origin);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int waterY = generator.getSeaLevel() - 1;

		for(int localX = 0; localX < 16; localX++) {
			for(int localZ = 0; localZ < 16; localZ++) {
				int x = origin.getX() + localX;
				int z = origin.getZ() + localZ;
				int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
				
				double biomeInfoNoise = Biome.BIOME_INFO_NOISE.get(x * 0.25D, z * 0.25D);
				BlockState filler = this.getMaterial(x, waterY, z, waterY);

				pos.set(x, surfaceY, z);
				
				if(level.getBiome(pos).is(Biomes.SWAMP)) {
			        if (biomeInfoNoise > 0.0D) {
			            for (int y = surfaceY; y >= surfaceY - 10; --y) {
			                pos.setY(y);
			                if (level.getBlockState(pos).isAir()) {
			                    continue;
			                }

			                if (y == waterY && !level.getFluidState(pos).isEmpty()) {
			                    level.setBlock(pos, filler, 2);
			                }
			                break;
			            }
			        }

			        int oceanFloor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, localX, localZ);
			        if (oceanFloor <= waterY) {
			        	level.setBlock(pos.setY(oceanFloor), this.getMaterial(x, oceanFloor, z, waterY), 2);
			        }					
				}
			}	
		}
		return true;
	}

    private BlockState getMaterial(int x, int y, int z, int waterY) {
        float value = MATERIAL_NOISE.compute(x, z, 0);
        if (value > 0.6F) {
            if (value < 0.75F && y < waterY) {
                return this.clayMaterial;
            }
            return this.gravelMaterial;
        }
        return this.dirtMaterial;
    }
    
    private static Noise makeMaterialNoise() {
    	Noise base = Noises.simplex(23, 40, 2);
    	return Noises.warpWhite(base, 213, 2, 4);    	
    }
}
