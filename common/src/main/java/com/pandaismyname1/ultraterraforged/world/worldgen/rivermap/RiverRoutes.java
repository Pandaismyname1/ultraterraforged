package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domain;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Line;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.Network;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.River;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.RiverWarp;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.RiverPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

/**
 * Lets rivers follow the land. Rivers are laid out as straight lines, which cross hills and ridges as readily as
 * valleys; this moves each river sideways along its length, up to a few hundred blocks, onto the lowest ground it can
 * reach, going around rises rather than over them, so it seldom has to climb or cut a gorge. The way is found by
 * dynamic programming over the ground on either side of the line, measured every few dozen blocks.
 *
 * Each river's forks, lakes and wetlands are laid out in its own space, so they move along with it; a fork's way is
 * found in its parent's space, keeping its mouth on the parent. The spaces are chained: {@link Frame}s undo them.
 */
public final class RiverRoutes {
	// the ground is measured this often along a river
	private static final float SPACING = 48.0F;
	// and at sideways offsets this far apart
	private static final float STEP = 16.0F;
	// up to this many either side
	private static final int MAX_STEPS = 24;
	// a river may wander sideways no more than this share of its length
	private static final float MAX_WANDER = 0.12F;
	// and turn no more sharply than this many steps per sample
	private static final int MAX_TURN = 2;
	// the costs of the way, per sample, in blocks of height
	// every block of ground above the lowest reachable
	private static final float HEIGHT_COST = 1.0F;
	// every block climbed going downstream
	private static final float CLIMB_COST = 10.0F;
	// every step sideways
	private static final float TURN_COST = 0.4F;
	// every step away from the straight line
	private static final float WANDER_COST = 0.15F;
	// every sample out at sea, before the last stretch of a river, where it meets the sea
	private static final float SEA_COST = 60.0F;
	private static final float MOUTH = 0.92F;

	/**
	 * Maps a place in a river's space to the world.
	 */
	public interface Frame {
		long toWorld(float x, float z);
	}

	private RiverRoutes() {
	}

	/**
	 * Finds the way of every river, and gives every river its frame.
	 *
	 * @param route whether rivers follow the land; if not, they only get their frames
	 */
	public static void route(Network[] networks, Domain warp, Heightmap heightmap, boolean route) {
		Frame world = (x, z) -> unwarp(x, z, warp);
		for (Network network : networks) {
			route(network, world, heightmap, route);
		}
	}

	private static void route(Network network, Frame frame, Heightmap heightmap, boolean route) {
		RiverPopulator carver = network.riverCarver();
		carver.frame = frame;
		if (route) {
			// a fork keeps its mouth on its parent
			carver.setRoute(findRoute(carver, frame, heightmap, carver.junction >= 0.0F));
		}
		// the forks, lakes and wetlands of this river lie in the space it moves them into
		Frame inner = (x, z) -> {
			long outer = unroute(carver, x, z);
			return frame.toWorld(PosUtil.unpackLeftf(outer), PosUtil.unpackRightf(outer));
		};
		carver.innerFrame = inner;
		for (Network child : network.children()) {
			route(child, inner, heightmap, route);
		}
	}

	private static float[] findRoute(RiverPopulator carver, Frame frame, Heightmap heightmap, boolean fixedMouth) {
		River river = carver.river;
		int samples = Math.max(2, (int) Math.ceil(river.length / SPACING) + 1);
		int steps = Math.min(MAX_STEPS, (int) (river.length * MAX_WANDER / STEP));
		if (steps < 1) {
			return null;
		}
		int width = steps * 2 + 1;
		float worldHeight = heightmap.levels().worldHeight;
		float sea = heightmap.levels().water * worldHeight;
		// the ground under each place the river could run through, in blocks
		float[][] ground = new float[samples][width];
		for (int i = 0; i < samples; i++) {
			float t = i / (float) (samples - 1);
			float lineX = river.x1 + river.dx * t;
			float lineZ = river.z1 + river.dz * t;
			for (int k = 0; k < width; k++) {
				float offset = (k - steps) * STEP;
				long channel = unroute(carver, lineX + river.normX * offset, lineZ + river.normZ * offset);
				long world = frame.toWorld(PosUtil.unpackLeftf(channel), PosUtil.unpackRightf(channel));
				// the sea is no lower than its surface, and no place for a river to run along
				float height = heightmap.sampleGround(PosUtil.unpackLeftf(world), PosUtil.unpackRightf(world)).height * worldHeight;
				ground[i][k] = height < sea ? sea + (t < MOUTH ? SEA_COST : 0.0F) : height;
			}
		}
		// the cheapest way to each place, from the source
		float[][] cost = new float[samples][width];
		int[][] from = new int[samples][width];
		for (int i = 0; i < samples; i++) {
			float lowest = Float.MAX_VALUE;
			for (int k = 0; k < width; k++) {
				lowest = Math.min(lowest, ground[i][k]);
			}
			for (int k = 0; k < width; k++) {
				float here = HEIGHT_COST * (ground[i][k] - lowest) + WANDER_COST * Math.abs(k - steps);
				if (i == 0) {
					cost[i][k] = here;
					continue;
				}
				float best = Float.MAX_VALUE;
				int bestFrom = k;
				for (int j = Math.max(0, k - MAX_TURN); j <= Math.min(width - 1, k + MAX_TURN); j++) {
					float step = cost[i - 1][j] + TURN_COST * Math.abs(k - j) + CLIMB_COST * Math.max(0.0F, ground[i][k] - ground[i - 1][j]);
					if (step < best) {
						best = step;
						bestFrom = j;
					}
				}
				cost[i][k] = best + here;
				from[i][k] = bestFrom;
			}
		}
		// back from the cheapest end, or the middle for a fork, which has to meet its parent
		int end = steps;
		if (!fixedMouth) {
			for (int k = 0; k < width; k++) {
				if (cost[samples - 1][k] < cost[samples - 1][end]) {
					end = k;
				}
			}
		}
		float[] offsets = new float[samples];
		for (int i = samples - 1, k = end; i >= 0; k = from[i][k], i--) {
			offsets[i] = (k - steps) * STEP;
		}
		// rounding off the corners
		for (int pass = 0; pass < 2; pass++) {
			float[] smooth = offsets.clone();
			for (int i = 1; i < samples - 1; i++) {
				smooth[i] = (offsets[i - 1] + offsets[i] * 2.0F + offsets[i + 1]) * 0.25F;
			}
			offsets = smooth;
		}
		if (fixedMouth) {
			offsets[samples - 1] = 0.0F;
		}
		return offsets;
	}

	/**
	 * The place in a river's own space that it carves at the given place in its inner space, where its line is straight:
	 * undoes its winding and its way over the land, which depend on the place they move, so it's found by repeatedly
	 * stepping back by the difference.
	 */
	public static long unroute(RiverPopulator carver, float x, float z) {
		float qx = x;
		float qz = z;
		for (int i = 0; i < 8; i++) {
			long moved = carve(carver, qx, qz);
			float errorX = x - PosUtil.unpackLeftf(moved);
			float errorZ = z - PosUtil.unpackRightf(moved);
			qx += errorX;
			qz += errorZ;
			if (errorX * errorX + errorZ * errorZ < 0.01F) {
				break;
			}
		}
		return PosUtil.packf(qx, qz);
	}

	// where a river moves a place in its own space to, as Network.carve does
	private static long carve(RiverPopulator carver, float x, float z) {
		River river = carver.river;
		RiverWarp warp = carver.warp;
		float t = Line.distanceOnLine(x, z, river.x1, river.z1, river.x2, river.z2);
		if (warp.test(t)) {
			long offset = warp.getOffset(x, z, t, river);
			x += PosUtil.unpackLeftf(offset);
			z += PosUtil.unpackRightf(offset);
			t = Line.distanceOnLine(x, z, river.x1, river.z1, river.x2, river.z2);
		}
		float route = carver.routeOffset(t);
		return PosUtil.packf(x - river.normX * route, z - river.normZ * route);
	}

	/**
	 * Where the middle of a river's channel lies in the world, at a point along its straight line.
	 */
	public static long channelInWorld(RiverPopulator carver, float t) {
		long channel = channel(carver, t);
		return carver.frame.toWorld(PosUtil.unpackLeftf(channel), PosUtil.unpackRightf(channel));
	}

	/**
	 * Where the middle of a river's channel lies in its own space, at a point along its straight line.
	 */
	public static long channel(RiverPopulator carver, float t) {
		River river = carver.river;
		return unroute(carver, river.x1 + river.dx * t, river.z1 + river.dz * t);
	}

	// a place in the rivers' space, found in the world by undoing the rivermap's warp
	private static long unwarp(float x, float z, Domain warp) {
		float worldX = x;
		float worldZ = z;
		for (int i = 0; i < 3; i++) {
			float offsetX = warp.getOffsetX(worldX, worldZ, 0);
			float offsetZ = warp.getOffsetZ(worldX, worldZ, 0);
			worldX = x - offsetX;
			worldZ = z - offsetZ;
		}
		return PosUtil.packf(worldX, worldZ);
	}
}
