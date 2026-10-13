package com.pandaismyname1.ultraterraforged.world.worldgen.tile;

import java.lang.ref.WeakReference;

/**
 * A chunk's cells in its tile, for something that can outlive the tile's time in the cache: a chunk being generated
 * keeps its NoiseChunk until it's done, which can take minutes while it waits for its neighbours, and other mods
 * (Distant Horizons, for one) hold thousands of chunks like that. Holding the chunk of the tile would keep the whole
 * tile alive with it, some 4 MB of cells; this lets the tile go with the cache, and takes it from the cache again (the
 * same tile, regenerated if it has to be) when the chunk reads it after that.
 */
public final class TileChunkRef {
	private final TileFactory cache;
	private final int chunkX, chunkZ;
	private WeakReference<Tile.Chunk> chunk;

	public TileChunkRef(TileFactory cache, int chunkX, int chunkZ) {
		this.cache = cache;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.chunk = new WeakReference<>(this.load());
	}

	public int chunkX() {
		return this.chunkX;
	}

	public int chunkZ() {
		return this.chunkZ;
	}

	// hold on to what this returns only while reading it
	public Tile.Chunk get() {
		Tile.Chunk chunk = this.chunk.get();
		if (chunk == null) {
			// two threads may both load it; either chunk reads the same cells
			chunk = this.load();
			this.chunk = new WeakReference<>(chunk);
		}
		return chunk;
	}

	private Tile.Chunk load() {
		return this.cache.provideChunk(this.chunkX, this.chunkZ);
	}
}
