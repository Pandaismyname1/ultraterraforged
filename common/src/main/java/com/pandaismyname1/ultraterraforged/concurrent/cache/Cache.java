package com.pandaismyname1.ultraterraforged.concurrent.cache;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

import com.pandaismyname1.ultraterraforged.concurrent.ThreadPools;
import com.pandaismyname1.ultraterraforged.concurrent.cache.map.LongMap;

public class Cache<V extends ExpiringEntry> implements AutoCloseable {
	public static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(ThreadPools.daemonFactory("UTF-CacheScheduler"));

    private LongMap<V> map;
    private long lifetimeMS;
    private volatile long timeout;
    private ScheduledFuture<?> poll;

    public Cache(int capacity, long expireTime, long pollInterval, TimeUnit unit, IntFunction<LongMap<V>> mapFunc) {
        this.timeout = 0L;
        this.map = mapFunc.apply(capacity);
        this.lifetimeMS = unit.toMillis(expireTime);
        this.poll = schedulePolls(this, unit.toMillis(pollInterval));
    }

    // The polls hold the cache weakly and stop once it's gone: a world's caches go with the world, and another world
    // starting no longer has to stop every cache's polls to let the old ones go (it stopped the new world's too, so
    // its tiles never expired).
    private static ScheduledFuture<?> schedulePolls(Cache<?> cache, long intervalMillis) {
    	WeakReference<Cache<?>> ref = new WeakReference<>(cache);
    	AtomicReference<ScheduledFuture<?>> polls = new AtomicReference<>();
    	polls.set(SCHEDULER.scheduleAtFixedRate(() -> {
    		Cache<?> live = ref.get();
    		if (live != null) {
    			live.poll();
    		} else if (polls.get() != null) {
    			polls.get().cancel(false);
    		}
    	}, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS));
    	return polls.get();
    }

    public void remove(long key) {
        this.map.remove(key, ExpiringEntry::close);
    }

    // takes out this very entry, not one that has since taken its place
    public void forget(V entry) {
        this.map.removeIf((other) -> other == entry);
    }

    public V get(long key) {
        return this.map.get(key);
    }

    public V computeIfAbsent(long key, LongFunction<V> func) {
        return this.map.computeIfAbsent(key, func);
    }

    public void poll() {
        this.timeout = System.currentTimeMillis() - this.lifetimeMS;
        this.map.removeIf((entry) -> entry.getTimestamp() < this.timeout);
    }

	public boolean isPolling() {
		return !this.poll.isDone();
	}

	@Override
	public void close() {
		this.poll.cancel(false);
	}
}
