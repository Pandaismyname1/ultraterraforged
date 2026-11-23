package com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.KeyDispatchDataCodec;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellField;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.WorldLookup;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileChunkRef;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

public record CellSampler(Supplier<GeneratorContext> generatorContext, CellField field) implements MappedFunction {
	private static final Cell EMPTY = new Cell();
	private static final ThreadLocal<Cache2d> LOCAL_CELL = ThreadLocal.withInitial(Cache2d::new);
	// apart, so the heightmap's cell, read in a pass that only looks, isn't handed out afterwards as the tile's
	private static final ThreadLocal<Cache2d> LOCAL_TILE_CELL = ThreadLocal.withInitial(Cache2d::new);
	
	@Override
	public double compute(FunctionContext ctx) {
		return this.field.read(cell(this.generatorContext.get(), null, ctx.blockX(), ctx.blockZ()));
	}

	// no bounds: continentalness, erosion and weirdness are negative too, and vanilla's min and max skip an argument
	// by its bounds
	@Override
	public double minValue() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double maxValue() {
		return Double.POSITIVE_INFINITY;
	}

	/**
	 * The cells a NoiseChunk over the chunk reads, from its tile; null when it looks them up one column at a time. A
	 * single column (a structure asking for the height) reads its chunk's cells too, so it has the terrain its chunk is
	 * generated with on whichever thread asks; only a pass that just looks makes a quick guess.
	 */
	@Nullable
	public static ChunkCells chunkCells(GeneratorContext generatorContext, int cellCountXZ, int chunkX, int chunkZ) {
		if (cellCountXZ <= 1 && generatorContext.lookup.approximate()) {
			return null;
		}
		return new ChunkCells(new TileChunkRef(generatorContext.cache, chunkX, chunkZ));
	}

	// the cell a function reads at a column, given the cells of the NoiseChunk it samples for. Outside them (the corners
	// on a chunk's far edges, in the next chunk or tile, or no NoiseChunk at all) it's the cell as the chunk there is
	// generated with it, or the heightmap's in a pass that only looks (WorldLookup.applyAsGenerated); never whatever
	// tile happens to be made, which depends on the thread and the moment
	public static Cell cell(@Nullable GeneratorContext generatorContext, @Nullable ChunkCells chunk, int blockX, int blockZ) {
		Cell cell;
		if (chunk != null && (cell = chunk.getCell(blockX, blockZ)) != null) {
			return cell;
		}
		if (generatorContext == null) {
			return EMPTY;
		}
		return (generatorContext.lookup.approximate() ? LOCAL_CELL : LOCAL_TILE_CELL).get().getAndUpdate(generatorContext.lookup, blockX, blockZ, true);
	}

	/**
	 * The cells of the chunk being generated. Not the tile's chunk itself: a chunk waiting to be generated would keep
	 * its whole tile alive.
	 */
	public record ChunkCells(TileChunkRef chunk) {

		@Nullable
		public Cell getCell(int blockX, int blockZ) {
			return SectionPos.blockToSectionCoord(blockX) == this.chunk.chunkX() && SectionPos.blockToSectionCoord(blockZ) == this.chunk.chunkZ() ? this.chunk.get().getCell(blockX, blockZ) : null;
		}
	}
	
	// the last cell looked up on this thread, at the corner of its 4x4 column, and the world it was looked up in: worlds
	// open one after another, or side by side, share the worker threads. Held weakly, so a closed world can go
	public static class Cache2d {
		private long lastPos = Long.MAX_VALUE;
		private WeakReference<WorldLookup> lastLookup = new WeakReference<>(null);
		private Cell cell = new Cell();
		
		public Cell getAndUpdate(WorldLookup lookup, int blockX, int blockZ) {
			return this.getAndUpdate(lookup, blockX, blockZ, false);
		}

		// asGenerated: the cell as the chunk is generated with it (WorldLookup.applyAsGenerated), instead of the tile's
		// if it's made already and the heightmap's if not
		public Cell getAndUpdate(WorldLookup lookup, int blockX, int blockZ, boolean asGenerated) {
			blockX = QuartPos.toBlock(QuartPos.fromBlock(blockX));
			blockZ = QuartPos.toBlock(QuartPos.fromBlock(blockZ));
			
			long packedPos = PosUtil.pack(blockX, blockZ);
			if(this.lastPos != packedPos || this.lastLookup.get() != lookup) {
				if (asGenerated) {
					lookup.applyAsGenerated(this.cell.reset(), blockX, blockZ);
				} else {
					lookup.apply(this.cell.reset(), blockX, blockZ);
				}
				this.lastPos = packedPos;
				if(this.lastLookup.get() != lookup) {
					this.lastLookup = new WeakReference<>(lookup);
				}
			}
			return this.cell;
		}
	}
	
	public class CacheChunk implements MappedFunction {
		@Nullable
		private final ChunkCells chunk;

		public CacheChunk(@Nullable ChunkCells chunk) {
			this.chunk = chunk;
		}

		@Override
		public double compute(FunctionContext ctx) {
			return CellSampler.this.field.read(cell(CellSampler.this.generatorContext.get(), this.chunk, ctx.blockX(), ctx.blockZ()));
		}

		@Override
		public double minValue() {
			return CellSampler.this.minValue();
		}

		@Override
		public double maxValue() {
			return CellSampler.this.maxValue();
		}
	}
	
	public record Marker(CellField field) implements MappedFunction.Marker {
		public static final Codec<Marker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				CellField.CODEC.fieldOf("field").forGetter(Marker::field)
		).apply(instance, Marker::new));
		
		@Override
		public KeyDispatchDataCodec<Marker> codec() {
			return new KeyDispatchDataCodec<>(CODEC);
		}
	}
}