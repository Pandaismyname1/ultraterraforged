package com.pandaismyname1.ultraterraforged.world.worldgen;

import java.util.function.Supplier;

/**
 * Whether the terrain looked up on this thread may be a quick guess. It isn't, unless it's looked up inside
 * {@link #approximately}: everywhere else a column has the terrain its chunk is generated with (its tile's cells),
 * whichever thread asks and whenever, so structure starts worked out ahead of the players on another thread sit where
 * the chunk worker would put them. A pass that only looks (a locate) tries many places nobody has been near, where
 * making each one's tile would take a while, and is happy with the heightmap without the tiles' filters.
 */
public final class WorldGenFlags {
	private static final ThreadLocal<Boolean> APPROXIMATE = ThreadLocal.withInitial(() -> false);

	private WorldGenFlags() {
	}

	public static boolean approximateLookups() {
		return APPROXIMATE.get();
	}

	public static <T> T approximately(Supplier<T> lookups) {
		boolean previous = APPROXIMATE.get();
		APPROXIMATE.set(true);
		try {
			return lookups.get();
		} finally {
			APPROXIMATE.set(previous);
		}
	}
}
