package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * What a landform found in each cell of its grid, like where a tor stands, remembered per thread for the cells around
 * it. The sites belong to one world, so each landform needs its own cache.
 */
public record SiteCache<T>(ThreadLocal<Long2ObjectOpenHashMap<T>> sites) {
	private static final int LIMIT = 4096;

	public interface Finder<T> {
		T find(int gridX, int gridZ);
	}

	public static <T> SiteCache<T> make() {
		return new SiteCache<>(ThreadLocal.withInitial(Long2ObjectOpenHashMap::new));
	}

	public T get(int gridX, int gridZ, Finder<T> finder) {
		Long2ObjectOpenHashMap<T> sites = this.sites.get();
		long key = ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
		T site = sites.get(key);
		if (site != null) {
			return site;
		}
		if (sites.size() > LIMIT) {
			sites.clear();
		}
		site = finder.find(gridX, gridZ);
		sites.put(key, site);
		return site;
	}
}
