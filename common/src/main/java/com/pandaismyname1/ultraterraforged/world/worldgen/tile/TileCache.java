package com.pandaismyname1.ultraterraforged.world.worldgen.tile;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import org.jetbrains.annotations.Nullable;

import com.pandaismyname1.ultraterraforged.concurrent.cache.Cache;
import com.pandaismyname1.ultraterraforged.concurrent.cache.CacheEntry;
import com.pandaismyname1.ultraterraforged.concurrent.cache.CacheManager;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

public class TileCache implements TileFactory {
	private int tileSize;
	private boolean queue;
	private Cache<CacheEntry<Entry>> cache;
	private TileGenerator generator;
	
	public TileCache(int tileSize, boolean queue, TileGenerator generator) {
		this.tileSize = tileSize;
		this.queue = queue;
		this.cache = CacheManager.createCache(256, 60L, 20L, TimeUnit.SECONDS);
		this.generator = generator;
	}
	
	public TileGenerator getGenerator() {
		return this.generator;
	}
	
	@Nullable
	public Tile provideIfPresent(int tileX, int tileZ) {
		@Nullable
		CacheEntry<Entry> entry = this.cache.get(PosUtil.pack(tileX, tileZ));
		if(entry == null || !entry.isDone()) {
			return null;
		}
		try {
			return entry.get().tile;
		} catch(RuntimeException | Error e) {
			this.cache.forget(entry);
			return null;
		}
	}

	@Nullable
	public Tile provideAtChunkIfPresent(int chunkX, int chunkZ) {
		return this.provideIfPresent(this.chunkToTile(chunkX), this.chunkToTile(chunkZ));
	}

	@Override
	public Tile provide(int tileX, int tileZ) {
		CacheEntry<Entry> entry = this.computeEntry(tileX, tileZ);
		try {
			return entry.get().tile;
		} catch(RuntimeException | Error e) {
			// a tile that failed isn't kept, failing everyone who asks for it while they keep it from expiring: the
			// next one to ask makes it again
			this.cache.forget(entry);
			throw e;
		}
	}
	
	@Override
	public void queue(int tileX, int tileZ) {
		if(this.queue) {
			this.computeEntry(tileX, tileZ);
		}
	}

	@Override
	public void dropAtChunk(int chunkX, int chunkZ) {
		long packedTilePos = PosUtil.pack(this.chunkToTile(chunkX), this.chunkToTile(chunkZ));
		CacheEntry<Entry> entry = this.cache.get(packedTilePos);
		if(entry != null && entry.get().drop(chunkX, chunkZ)) {
			// out of the cache first, so nothing new picks the tile up while it's being closed
			this.cache.remove(packedTilePos);
			entry.get().tile.close();
		}
	}

	@Override
	public int chunkToTile(int chunkCoord) {
		return chunkCoord >> this.tileSize;
	}
	
	private CacheEntry<Entry> computeEntry(int tileX, int tileZ) {
		return this.cache.computeIfAbsent(PosUtil.pack(tileX, tileZ), (k) -> {
			return CacheEntry.supply(this.generator.generate(tileX, tileZ).thenApply(Entry::new));
		});
	}
	
	private class Entry {
		private AtomicInteger refCount;
		// the chunks done with the tile, a bit each: a chunk done twice (a mod running a chunk's features again, on a
		// copy) counts once, and doesn't have the tile closed while other chunks still need it
		private AtomicLongArray dropped;
		private int chunkCount;
		private Tile tile;
		
		public Entry(Tile tile) {
			int tileSize = tile.getChunkSize().size();
			this.refCount = new AtomicInteger();
			this.chunkCount = tileSize * tileSize;
			this.dropped = new AtomicLongArray((this.chunkCount + 63) >> 6);
			this.tile = tile;
		}
		
		// whether every chunk of the tile is done with it; true once only
		public boolean drop(int chunkX, int chunkZ) {
			Size size = this.tile.getChunkSize();
			int index = size.mask(chunkZ) * size.size() + size.mask(chunkX);
			long bit = 1L << (index & 63);
			if((this.dropped.getAndAccumulate(index >> 6, bit, (bits, chunk) -> bits | chunk) & bit) != 0L) {
				return false;
			}
			return this.refCount.incrementAndGet() == this.chunkCount;
		}
	}
}