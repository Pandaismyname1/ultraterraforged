package com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Interval;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.generator.ConstantFunction;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFCompileContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellField;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.WorldLookup;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

/**
 * A value of UltraTerraForged's terrain, read from the cell at a column. It's the same all the way up a column, so the
 * compiler evaluates it once per column.
 *
 * A chunk being generated reads its cells from its tile, which MixinNoiseChunk puts in the sampler context under
 * {@link #CHUNK}, as vanilla does with the beardifier; anywhere else they're looked up one column at a time.
 */
public record CellSampler(CellField field) implements DensityFunction {
	public static final MapCodec<CellSampler> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		CellField.CODEC.fieldOf("field").forGetter(CellSampler::field)
	).apply(instance, CellSampler::new));
	public static final ContextKey<ChunkCells> CHUNK = new ContextKey<>(UTFCommon.location("chunk_cells"));

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		RandomState randomState = UTFCompileContext.randomState(context);
		if ((Object) randomState instanceof UTFRandomState utfRandomState) {
			return new Sampler(utfRandomState, this.field);
		}
		return new ConstantFunction.Sampler(this.field.read(Sampler.EMPTY));
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		return this;
	}

	// what each field can hold isn't bounded tightly, so the compiler isn't told a range
	@Override
	public Interval range() {
		return Interval.INFINITE;
	}

	@Override
	public @Axes int domainAxes() {
		return AXIS_X | AXIS_Z;
	}

	@Override
	public MapCodec<CellSampler> codec() {
		return CODEC;
	}

	/**
	 * The cells of the chunk being generated.
	 */
	public record ChunkCells(Tile.Chunk chunk, int chunkX, int chunkZ) {

		@Nullable
		public Cell getCell(int blockX, int blockZ) {
			return SectionPos.blockToSectionCoord(blockX) == this.chunkX && SectionPos.blockToSectionCoord(blockZ) == this.chunkZ ? this.chunk.getCell(blockX, blockZ) : null;
		}
	}

	private record Sampler(UTFRandomState randomState, CellField field) implements DensitySampler {
		private static final Cell EMPTY = new Cell();
		private static final ThreadLocal<Cache2d> LOCAL_CELL = ThreadLocal.withInitial(Cache2d::new);

		@Override
		public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
			return this.field.read(this.cell(context, blockX, blockZ));
		}

		@Override
		public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
			int sizeY = volume.sizeY();
			for (int z = 0; z < volume.sizeZ(); z++) {
				int blockZ = volume.blockZ(z);
				for (int x = 0; x < volume.sizeX(); x++) {
					float value = this.field.read(this.cell(context, volume.blockX(x), blockZ));
					outputBuffer.setRange(volume.indexUnchecked(x, 0, z), sizeY, value);
				}
			}
		}

		private Cell cell(SamplerContext context, int blockX, int blockZ) {
			ChunkCells chunk = context.getField(CHUNK);
			Cell cell;
			if (chunk != null && (cell = chunk.getCell(blockX, blockZ)) != null) {
				return cell;
			}
			GeneratorContext generatorContext = this.randomState.generatorContext();
			if (generatorContext == null) {
				return EMPTY;
			}
			return LOCAL_CELL.get().getAndUpdate(generatorContext.lookup, blockX, blockZ);
		}
	}

	// the last cell looked up on this thread, at the corner of its 4x4 column
	public static class Cache2d {
		private long lastPos = Long.MAX_VALUE;
		private Cell cell = new Cell();

		public Cell getAndUpdate(WorldLookup lookup, int blockX, int blockZ) {
			blockX = QuartPos.toBlock(QuartPos.fromBlock(blockX));
			blockZ = QuartPos.toBlock(QuartPos.fromBlock(blockZ));

			long packedPos = PosUtil.pack(blockX, blockZ);
			if(this.lastPos != packedPos) {
				lookup.apply(this.cell.reset(), blockX, blockZ);
				this.lastPos = packedPos;
			}
			return this.cell;
		}
	}
}
