package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import org.jetbrains.annotations.Nullable;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Line;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.Network;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.River;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.RiverWarp;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.RiverPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

/**
 * Which way the water of a river runs at a place in it, for mods that move water along (Streams Reflowing's currents,
 * compat/streams). Rivers run from their source to their mouth, into the sea or into the river they fork from.
 */
public final class RiverFlow {
	// how far up and down the river, in blocks, its direction is taken from
	private static final float STEP = 8.0F;
	// a place further from a river's middle than this many times its bank width isn't in it
	private static final float REACH = 2.0F;

	/**
	 * @param dirX the way the water runs, a unit vector in the world
	 * @param main whether it's a main river rather than a fork
	 */
	public record Sample(float dirX, float dirZ, boolean main) {
	}

	private RiverFlow() {
	}

	/**
	 * The river carving the given place in the world, the one whose middle is closest for its size, and the way its
	 * water runs there; null if no river is near.
	 */
	@Nullable
	public static Sample sample(Rivermap rivermap, float x, float z) {
		float rx = rivermap.riverX(x, z);
		float rz = rivermap.riverZ(x, z);
		Nearest nearest = new Nearest();
		for (Network network : rivermap.networks()) {
			if (network.contains(rx, rz)) {
				find(network, rx, rz, nearest);
			}
		}
		RiverPopulator carver = nearest.carver;
		if (carver == null) {
			return null;
		}
		River river = carver.river;
		float dirX = river.ndx;
		float dirZ = river.ndz;
		// the frames (set when rivers follow the land or are raised) give the channel's real way through the world;
		// without them the river's own line, as the rivermap's warp bends it only gently
		if (carver.frame != null) {
			float dt = Math.min(0.05F, STEP / Math.max(river.length, 1.0F));
			long up = RiverRoutes.channelInWorld(carver, NoiseUtil.clamp(nearest.t - dt, 0.0F, 1.0F));
			long down = RiverRoutes.channelInWorld(carver, NoiseUtil.clamp(nearest.t + dt, 0.0F, 1.0F));
			float dx = PosUtil.unpackLeftf(down) - PosUtil.unpackLeftf(up);
			float dz = PosUtil.unpackRightf(down) - PosUtil.unpackRightf(up);
			float length = (float) Math.sqrt(dx * dx + dz * dz);
			if (length > 1.0E-3F) {
				dirX = dx / length;
				dirZ = dz / length;
			}
		}
		return new Sample(dirX, dirZ, carver.main);
	}

	// as Network.carve finds the place in each river's space, and its forks' in theirs
	private static void find(Network network, float x, float z, Nearest nearest) {
		RiverPopulator carver = network.riverCarver();
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
		x -= river.normX * route;
		z -= river.normZ * route;
		float width = Math.max(carver.config.bankWidth, 1.0F);
		float score = carver.distance2(x, z, t) / (width * width);
		// later rivers win ties, as they carve over the earlier ones
		if (score < REACH * REACH && score <= nearest.score) {
			nearest.carver = carver;
			nearest.t = t;
			nearest.score = score;
		}
		for (Network child : network.children()) {
			find(child, x, z, nearest);
		}
	}

	private static class Nearest {
		private RiverPopulator carver;
		private float t;
		private float score = Float.MAX_VALUE;
	}
}
