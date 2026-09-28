package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.SectionPos;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

// the cells of the chunk a surface is being built in, fetched from the tile cache when the chunk changes (the material
// context no longer knows the chunk, only the position)
final class ChunkCellReader {
	private final GeneratorContext generatorContext;
	@Nullable
	private Tile.Chunk chunk;
	private int chunkX, chunkZ;

	ChunkCellReader(GeneratorContext generatorContext) {
		this.generatorContext = generatorContext;
	}

	Cell getCell(int blockX, int blockZ) {
		int chunkX = SectionPos.blockToSectionCoord(blockX);
		int chunkZ = SectionPos.blockToSectionCoord(blockZ);
		if (this.chunk == null || chunkX != this.chunkX || chunkZ != this.chunkZ) {
			this.chunk = this.generatorContext.cache.provideChunk(chunkX, chunkZ);
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}
		return this.chunk.getCell(blockX, blockZ);
	}
}
