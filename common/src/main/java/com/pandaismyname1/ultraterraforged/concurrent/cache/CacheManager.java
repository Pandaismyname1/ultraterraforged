package com.pandaismyname1.ultraterraforged.concurrent.cache;

import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import com.pandaismyname1.ultraterraforged.concurrent.cache.map.LongMap;
import com.pandaismyname1.ultraterraforged.concurrent.cache.map.StampedBoundLongMap;

public class CacheManager {

	public static <V extends ExpiringEntry> Cache<V> createCache(int capacity, long expireTime, long pollInterval, TimeUnit unit) {
		return createCache(capacity, expireTime, pollInterval, unit, StampedBoundLongMap::new);
	}

	public static <V extends ExpiringEntry> Cache<V> createCache(int capacity, long expireTime, long pollInterval, TimeUnit unit, IntFunction<LongMap<V>> mapFunc) {
		return new Cache<>(capacity, expireTime, pollInterval, unit, mapFunc);
	}
}
