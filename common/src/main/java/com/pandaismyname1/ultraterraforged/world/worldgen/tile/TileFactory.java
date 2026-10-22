package com.pandaismyname1.ultraterraforged.world.worldgen.tile;

public interface TileFactory {
	Tile provide(int tileX, int tileZ);
	
	void queue(int tileX, int tileZ);
	
	// a chunk of the tile is done with it
	void dropAtChunk(int chunkX, int chunkZ);

	int chunkToTile(int chunkCoord);
	
	default Tile provideAtChunk(int chunkX, int chunkZ) {
		return this.provide(this.chunkToTile(chunkX), this.chunkToTile(chunkZ));
	}

	default void queueAtChunk(int chunkX, int chunkZ) {
		this.queue(this.chunkToTile(chunkX), this.chunkToTile(chunkZ));
	}

	default Tile.Chunk provideChunk(int chunkX, int chunkZ) {
		return this.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ);
	}
}
