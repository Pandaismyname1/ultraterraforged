package raccoonman.reterraforged.world.worldgen.surface.condition;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.SurfaceRules;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.tile.Tile;

/**
 * Within the given number of blocks under the ground the terrain was generated with, and no more than a block above
 * it: the surface of the land or the sea floor, not the floor of a cave. Unlike vanilla's preliminary surface, it holds
 * on the deep sea floor too.
 */
class NearGroundCondition extends SurfaceRules.LazyYCondition {
	private final int depth;
	@Nullable
	private final Tile.Chunk chunk;
	@Nullable
	private final GeneratorContext generatorContext;

	NearGroundCondition(SurfaceRules.Context context, int depth) {
		super(context);
		this.depth = depth;
		GeneratorContext generatorContext = null;
		Tile.Chunk chunk = null;
		if ((Object) context.randomState instanceof RTFRandomState randomState && (generatorContext = randomState.generatorContext()) != null) {
			ChunkPos chunkPos = context.chunk.getPos();
			chunk = generatorContext.cache.provideChunk(chunkPos.x, chunkPos.z);
		}
		this.generatorContext = generatorContext;
		this.chunk = chunk;
	}

	@Override
	protected boolean compute() {
		if (this.chunk == null) {
			return false;
		}
		int ground = this.generatorContext.levels.scale(this.chunk.getCell(this.context.blockX, this.context.blockZ).height);
		int y = this.context.blockY;
		return y <= ground + 1 && y >= ground - this.depth;
	}

	public record Source(int depth) implements SurfaceRules.ConditionSource {
		public static final Codec<Source> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("depth").forGetter(Source::depth)
		).apply(instance, Source::new));

		@Override
		public NearGroundCondition apply(SurfaceRules.Context ctx) {
			return new NearGroundCondition(ctx, this.depth);
		}

		@Override
		public KeyDispatchDataCodec<Source> codec() {
			return new KeyDispatchDataCodec<>(CODEC);
		}
	}
}
