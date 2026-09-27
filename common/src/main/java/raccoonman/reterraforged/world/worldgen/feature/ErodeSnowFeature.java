package raccoonman.reterraforged.world.worldgen.feature;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.feature.ErodeSnowFeature.Config;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.tile.Tile;

public class ErodeSnowFeature extends Feature<Config> {
    private static final float MIN = min(SnowLayerBlock.LAYERS);
    private static final float MAX = max(SnowLayerBlock.LAYERS);
    // the slope is measured over this many blocks either side
    private static final int ASPECT_REACH = 3;
    // slopes gentler than this, in blocks per block, get no change to their snow; from this one on they get it all
    private static final float ASPECT_MIN_SLOPE = 0.15F;
    private static final float ASPECT_FULL_SLOPE = 1.0F;

	public ErodeSnowFeature(Codec<Config> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> placeContext) {
		WorldGenLevel level = placeContext.level();
		RandomState randomState = level.getLevel().getChunkSource().randomState();
		
		@Nullable
		GeneratorContext generatorContext;
		if((Object) randomState instanceof RTFRandomState rtfRandomState && (generatorContext = rtfRandomState.generatorContext()) != null) {
			ChunkGenerator generator = placeContext.chunkGenerator();
			ChunkPos chunkPos = new ChunkPos(placeContext.origin());
			int chunkX = chunkPos.x;
			int chunkZ = chunkPos.z;
			ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
			Tile.Chunk tileChunk = generatorContext.cache.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ);
			raccoonman.reterraforged.world.worldgen.heightmap.Heightmap heightmap = generatorContext.localHeightmap.get();
			Levels levels = heightmap.levels();
			Noise rand = Noises.white(heightmap.climate().randomSeed(), 1);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			Config config = placeContext.config();
			
			if (config.aspect() > 0.0F) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						aspectSnow(level, chunk, tileChunk, chunkPos.getBlockX(x), chunkPos.getBlockZ(z), x, z, config.aspect() * shade(tileChunk, x, z, levels), pos);
					}
				}
			}
			
			for(int x = 0; x < 16; x++) {
				for(int z = 0; z < 16; z++) {
		        	Cell cell = tileChunk.getCell(x, z);
		        	
					int scaledY = levels.scale(cell.height);
			        int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			        if(scaledY == surfaceY && scaledY >= generator.getSeaLevel() - 1) {
		        		int worldX = chunkPos.getBlockX(x);
		        		int worldZ = chunkPos.getBlockZ(z);
				        pos.set(worldX, surfaceY, worldZ);
				        
				        if(config.erode) {
//				        	if(level.getBiome(pos).value().getTemperature(pos) <= 0.25F) {
					            float var = -ColumnDecorator.sampleNoise(worldX, worldZ, 16, 0);
					            float hNoise = rand.compute(worldX, worldZ, 4) * config.heightModifier();
					            float sNoise = rand.compute(worldX, worldZ, 5) * config.slopeModifier();
					            float vModifier = cell.terrain == TerrainType.VOLCANO ? 0.15F : 0F;
					            float height = cell.height;// + var + hNoise + vModifier;
					            float steepness = cell.gradient;// + var + sNoise + vModifier;
					            
					            if (snowErosion(config, worldX, worldZ, steepness, height)) {
					                Predicate<BlockState> predicate = Heightmap.Types.MOTION_BLOCKING.isOpaque();
					                for (int dy = 2; dy > 0; dy--) {
					                    pos.setY(surfaceY + dy);
					                    BlockState state = chunk.getBlockState(pos);
					                    if (!predicate.test(state) || state.is(BlockTags.SNOW)) {
							            	erodeSnow(chunk, pos);
					                    }
					                }
					            }
//					        }
				        }
				        
				        if(config.smooth) {
				            pos.setY(surfaceY + 1);

				            BlockState state = chunk.getBlockState(pos);
				            if (state.isAir()) {
				                pos.setY(surfaceY);
				                state = chunk.getBlockState(pos);
				                if (state.isAir()) {
				                    continue;
				                }
				            }

				            if(state.is(Blocks.SNOW)) {
				            	smoothSnow(chunk, pos, cell, levels, 0.0F);
				            }
				        }
			        }
				}
			}
	        return true;
		} else {
			throw new IllegalStateException();
		}
	}

    // where facing north, into the shade, 1 on steep slopes; where facing south, into the sun, -1; 0 on flat ground
    static float shade(Tile.Chunk tileChunk, int x, int z, Levels levels) {
    	int x0 = Math.max(0, x - ASPECT_REACH);
    	int x1 = Math.min(15, x + ASPECT_REACH);
    	int z0 = Math.max(0, z - ASPECT_REACH);
    	int z1 = Math.min(15, z + ASPECT_REACH);
    	// in blocks per block; the ground rising towards the south faces north
    	float slopeX = (tileChunk.getCell(x1, z).height - tileChunk.getCell(x0, z).height) * levels.worldHeight / (x1 - x0);
    	float slopeZ = (tileChunk.getCell(x, z1).height - tileChunk.getCell(x, z0).height) * levels.worldHeight / (z1 - z0);
    	return shade(slopeX, slopeZ);
    }

    static float shade(float slopeX, float slopeZ) {
    	float slope = (float) Math.sqrt(slopeX * slopeX + slopeZ * slopeZ);
    	if (slope < 1.0E-4F) {
    		return 0.0F;
    	}
    	float steepness = NoiseUtil.clamp((slope - ASPECT_MIN_SLOPE) / (ASPECT_FULL_SLOPE - ASPECT_MIN_SLOPE), 0.0F, 1.0F);
    	return slopeZ / slope * steepness;
    }

    // snow as the climate a given number of blocks higher up would have it: lower down on slopes in the shade, and
    // higher up on those in the sun
    private static void aspectSnow(WorldGenLevel level, ChunkAccess chunk, Tile.Chunk tileChunk, int worldX, int worldZ, int x, int z, float shift, BlockPos.MutableBlockPos pos) {
    	int blocks = Math.round(shift);
    	if (blocks == 0) {
    		return;
    	}
    	int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1;
    	pos.set(worldX, top, worldZ);
    	Biome biome = level.getBiome(pos).value();
    	if (!biome.hasPrecipitation()) {
    		return;
    	}
    	boolean cold = biome.coldEnoughToSnow(pos.offset(0, blocks, 0));
    	BlockState state = chunk.getBlockState(pos);
    	if (cold && state.isAir()) {
    		BlockState snow = Blocks.SNOW.defaultBlockState();
    		if (snow.canSurvive(level, pos)) {
    			chunk.setBlockState(pos, snow, false);
    			pos.setY(top - 1);
    			BlockState below = chunk.getBlockState(pos);
    			if (below.hasProperty(SnowyDirtBlock.SNOWY)) {
    				chunk.setBlockState(pos, below.setValue(SnowyDirtBlock.SNOWY, true), false);
    			}
    		}
    	} else if (!cold && state.is(Blocks.SNOW)) {
    		erodeSnow(chunk, pos);
    	}
    }

    private static boolean snowErosion(Config config, float x, float z, float steepness, float height) {
        return /*steepness > erodeConfig.rockSteepness() ||*/ (steepness * 0.55F > config.steepness());// && height > config.height() || (steepness > erodeConfig.dirtSteepness() && height > ColumnDecorator.sampleNoise(x, z, erodeConfig.dirtVar(), erodeConfig.dirtMin()));
    }

    private static void erodeSnow(ChunkAccess chunk, BlockPos.MutableBlockPos pos) {
        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);

        if (pos.getY() > 0) {
            pos.setY(pos.getY() - 1);
            BlockState below = chunk.getBlockState(pos);
            if (below.hasProperty(GrassBlock.SNOWY)) {
                chunk.setBlockState(pos, below.setValue(GrassBlock.SNOWY, false), false);
            }
        }
    }
    
    private static void smoothSnow(ChunkAccess chunk, BlockPos.MutableBlockPos pos, Cell cell, Levels levels, float min) {
        float height = cell.height * levels.worldHeight;
        float depth = getDepth(height);
        if (depth > min) {
            int level = getLevel(depth);
            BlockState layer = getState(level);
            if (layer.is(Blocks.AIR)) {
                return;
            }
            chunk.setBlockState(pos, layer, false);

           fixBaseBlock(chunk, pos, layer, level);
        }
    }

    private static void fixBaseBlock(ChunkAccess chunk, BlockPos.MutableBlockPos pos, BlockState layerMaterial, int level) {
        if (layerMaterial.is(Blocks.SNOW)) {
            pos.move(Direction.DOWN);
            BlockState belowState = chunk.getBlockState(pos);

            if(level > 1 && belowState.getBlock() instanceof SpreadingSnowyDirtBlock) {
                chunk.setBlockState(pos, Blocks.DIRT.defaultBlockState(), false);
            } else if(level > 0) {
                chunk.setBlockState(pos, Blocks.SNOW_BLOCK.defaultBlockState(), false);
            }
        }
    }

    private static BlockState getState(int level) {
        if (level < MIN) {
            return Blocks.AIR.defaultBlockState();
        }
        if (level >= MAX) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, level);
    }
    
    private static int getLevel(float depth) {
        if (depth > 1) {
            depth = getDepth(depth);
        } else if (depth < 0) {
            depth = 0;
        }
        return NoiseUtil.round(depth * MAX);
    }

    private static float getDepth(float height) {
        return height - (int) height;
    }

    private static int min(Property<Integer> property) {
        return property.getPossibleValues().stream().min(Integer::compareTo).orElse(0);
    }

    private static int max(Property<Integer> property) {
        return property.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
    }

	/**
	 * @param aspect how many blocks lower the snow reaches on steep slopes in the shade, and higher on those in the sun
	 */
	public record Config(float steepness, float height, boolean erode, boolean smooth, float slopeModifier, float heightModifier, float aspect) implements FeatureConfiguration {
		public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.fieldOf("steepness").forGetter(Config::steepness),
			Codec.FLOAT.fieldOf("height").forGetter(Config::height),
			Codec.BOOL.fieldOf("erode").forGetter(Config::erode),
			Codec.BOOL.fieldOf("smooth").forGetter(Config::smooth),
			Codec.FLOAT.fieldOf("slope_modifier").forGetter(Config::slopeModifier),
			Codec.FLOAT.fieldOf("height_modifier").forGetter(Config::heightModifier),
			Codec.FLOAT.optionalFieldOf("aspect", 0.0F).forGetter(Config::aspect)
		).apply(instance, Config::new));
	}
}
