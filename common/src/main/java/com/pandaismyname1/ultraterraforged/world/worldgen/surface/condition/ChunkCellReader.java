package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.SectionPos;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileChunkRef;

// the cells of the chunk a surface is being built in, fetched from the tile cache when the chunk changes (the material
// context no longer knows the chunk, only the position); not the tile's chunk itself, which would keep the whole tile alive
final class ChunkCellReader {
	private final GeneratorContext generatorContext;
	@Nullable
	private TileChunkRef chunk;

	ChunkCellReader(GeneratorContext generatorContext) {
		this.generatorContext = generatorContext;
	}

	Cell getCell(int blockX, int blockZ) {
		int chunkX = SectionPos.blockToSectionCoord(blockX);
		int chunkZ = SectionPos.blockToSectionCoord(blockZ);
		if (this.chunk == null || chunkX != this.chunk.chunkX() || chunkZ != this.chunk.chunkZ()) {
			this.chunk = new TileChunkRef(this.generatorContext.cache, chunkX, chunkZ);
		}
		return this.chunk.get().getCell(blockX, blockZ);
	}
}
