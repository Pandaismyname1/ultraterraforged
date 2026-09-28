package com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement.poisson;

import java.util.Random;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

public class FastPoissonModifier implements PlacementModifier {
	public static final MapCodec<FastPoissonModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.INT.fieldOf("radius").forGetter((p) -> p.radius),
		Codec.FLOAT.fieldOf("scale").forGetter((p) -> p.scale),
		Codec.FLOAT.fieldOf("jitter").forGetter((p) -> p.jitter),
		Codec.FLOAT.fieldOf("biome_fade").forGetter((p) -> p.biomeFade),
		Codec.INT.fieldOf("density_variance_scale").forGetter((p) -> p.densityVariationScale),
		Codec.FLOAT.fieldOf("density_variation").forGetter((p) -> p.densityVariation)
	).apply(instance, FastPoissonModifier::new));
	
	protected int radius;
	protected float scale;
	protected float jitter;
	protected float biomeFade;
	protected int densityVariationScale;
	protected float densityVariation;
	
	public FastPoissonModifier(
		int radius,
		float scale,
		float jitter,
		float biomeFade,
		int densityVariationScale,
		float densityVariation
	) {
		this.radius = radius;
		this.scale = scale;
		this.jitter = jitter;
		this.biomeFade = biomeFade;
		this.densityVariationScale = densityVariationScale;
		this.densityVariation = densityVariation;
	}
	
	@Override
	public void modify(PlacementContext ctx, RandomSource random, BlockPos pos, Consumer<BlockPos> output) {
		WorldGenLevel level = ctx.getLevel();
		ChunkAccess chunk = ctx.getLevel().getChunk(pos);
		long levelSeed = level.getSeed();
		int seed = (int) levelSeed + 234523;
		ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x();
        int chunkZ = chunkPos.z();
        FastPoisson poisson = FastPoisson.LOCAL_POISSON.get();
        DensityNoise density = this.getDensityNoise(seed, chunkPos, level.getLevel().getChunkSource().randomState());
        FastPoissonContext poissonConfig = new FastPoissonContext(this.radius, this.jitter, this.scale, density);
        poisson.visit(seed, chunkX, chunkZ, new Random(levelSeed), poissonConfig, output, (x, z, out) -> {
        	out.accept(new BlockPos(x, 0, z));
        });
    }

	@Override
	public MapCodec<FastPoissonModifier> codec() {
		return CODEC;
	}

	private DensityNoise getDensityNoise(int seed, ChunkPos chunkPos, RandomState randomState) {
		BiomeVariance biomeVariance = BiomeVariance.NONE;
		
		if (this.biomeFade > BiomeVariance.MIN_FADE) {
			if((Object) randomState instanceof UTFRandomState utfRandomState) {
				Tile.Chunk reader = utfRandomState.generatorContext().cache.provideAtChunk(chunkPos.x(), chunkPos.z()).getChunkReader(chunkPos.x(), chunkPos.z());
				if (reader != null) {
					biomeVariance = new BiomeVariance(reader, this.biomeFade);
				}
			}
		}

		Noise densityVariance = Noises.one();
		if (this.densityVariation > 0) {
			densityVariance = Noises.simplex(seed + 1, this.densityVariationScale, 1);
			densityVariance = Noises.mul(densityVariance, this.densityVariation);
			densityVariance = Noises.add(densityVariance, 1.0F - this.densityVariation);
		}

		return new DensityNoise(biomeVariance, densityVariance);
	}
}
