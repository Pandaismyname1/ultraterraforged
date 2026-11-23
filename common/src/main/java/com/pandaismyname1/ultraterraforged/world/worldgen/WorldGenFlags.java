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
	// how many passes that only look this thread is inside: they can nest (a locate that searches for biomes)
	private static final ThreadLocal<int[]> APPROXIMATE = ThreadLocal.withInitial(() -> new int[1]);

	private WorldGenFlags() {
	}

	public static boolean approximateLookups() {
		return APPROXIMATE.get()[0] > 0;
	}

	// for mixins wrapping a vanilla method without MixinExtras, which Forge 1.20.1 doesn't ship: enter at its head,
	// leave at each return
	public static void enterApproximate() {
		APPROXIMATE.get()[0]++;
	}

	public static void leaveApproximate() {
		int[] depth = APPROXIMATE.get();
		if(depth[0] > 0) {
			depth[0]--;
		}
	}

	public static <T> T approximately(Supplier<T> lookups) {
		enterApproximate();
		try {
			return lookups.get();
		} finally {
			leaveApproximate();
		}
	}
}
