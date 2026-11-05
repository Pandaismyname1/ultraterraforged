package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.RiverCache;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.Rivermap;

/**
 * Issue #2 (chunks stopped loading with many big rivers): a river map being made mustn't hold up lookups of other
 * river maps, and each map is made once however many threads ask for it at the same time.
 */
public class RiverCacheTest {

	@Test
	void slowRiversDontHoldUpOthers() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicInteger made = new AtomicInteger();
		RiverCache cache = new RiverCache((x, z, id) -> {
			made.incrementAndGet();
			if (x == 0) {
				started.countDown();
				try {
					release.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			return Mockito.mock(Rivermap.class);
		});
		ExecutorService threads = Executors.newFixedThreadPool(4);
		try {
			Future<Rivermap> slow = threads.submit(() -> cache.getRivers(0, 0));
			assertTrue(started.await(10, TimeUnit.SECONDS));
			Future<Rivermap> waiting = threads.submit(() -> cache.getRivers(0, 0));
			// another map, and one already made, while the first is still being made
			Rivermap other = threads.submit(() -> cache.getRivers(1, 0)).get(10, TimeUnit.SECONDS);
			assertSame(other, threads.submit(() -> cache.getRivers(1, 0)).get(10, TimeUnit.SECONDS));
			assertTrue(!slow.isDone() && !waiting.isDone());
			release.countDown();
			assertSame(slow.get(10, TimeUnit.SECONDS), waiting.get(10, TimeUnit.SECONDS));
			assertSame(slow.get(), cache.getRivers(0, 0));
			assertEquals(2, made.get());
		} finally {
			release.countDown();
			threads.shutdownNow();
		}
	}
}
