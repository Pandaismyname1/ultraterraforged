package com.pandaismyname1.ultraterraforged.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPools {
	public static final ExecutorService WORLD_GEN = Executors.newFixedThreadPool(availableProcessors(), daemonFactory("RTF-WorldGen"));
	
	public static int availableProcessors() {
		return Math.max(2, Runtime.getRuntime().availableProcessors());
	}

	// worker threads must be daemons, otherwise they keep the jvm alive after the game, server or datagen exits
	public static ThreadFactory daemonFactory(String name) {
		AtomicInteger counter = new AtomicInteger();
		return (runnable) -> {
			Thread thread = new Thread(runnable, name + "-" + counter.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		};
	}
}
