package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.Network;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.River;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.LakePopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.RiverPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.WetlandPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

/**
 * Works out where the water of each river, lake and wetland lies, from the land it runs through, so rivers flow a few
 * blocks below their banks at any height rather than all cutting down to the sea. Going from a river's source to its
 * mouth, the water drops wherever the land beside the river does, and cuts a gorge through high ground in its way; but
 * rivers are laid out without regard to the land, so where the land rises more than a gorge's depth above the water,
 * the water climbs with it, in rapids. It never stands above the land beside it, and ends at the sea, or at the level
 * of the river it flows into, stepping up or down to meet it. Lakes at a river's source hold its first water; wetlands take the level of their stretch
 * of river.
 */
public final class WaterLevels {
	// the ground beside a river is measured this often along it
	private static final float SPACING = 16.0F;
	// the water lies this many blocks below the lowest land beside the river
	private static final int MARGIN = 2;
	// the land beside a river is measured this far out past its banks
	private static final float BEYOND_BANKS = 4.0F;
	// wetlands only lie where their river drops no more than this many blocks along them
	private static final int WETLAND_DROP = 2;
	// the water drops, or climbs, this many blocks at a time, so rivers run in level pools with small waterfalls between
	// them rather than a one block step every few blocks
	private static final int TERRACE = 3;

	private WaterLevels() {
	}

	/**
	 * The rivers must have their frames, from {@link RiverRoutes#route}.
	 *
	 * @param gorgeDepth how many blocks a river may cut below the land beside it
	 */
	public static void level(Network[] networks, int gorgeDepth, Heightmap heightmap) {
		Levels levels = heightmap.levels();
		for (Network network : networks) {
			level(network, levels.water, gorgeDepth * levels.unit, heightmap, levels);
		}
	}

	// levels a river ending at the given water level, then the forks flowing into it
	private static void level(Network network, float mouth, float gorge, Heightmap heightmap, Levels levels) {
		RiverPopulator carver = network.riverCarver();
		River river = carver.river;
		int samples = Math.max(2, (int) Math.ceil(river.length / SPACING) + 1);
		float side = carver.config.bankWidth + BEYOND_BANKS;
		float[] lowest = new float[samples];
		for (int i = 0; i < samples; i++) {
			float t = i / (float) (samples - 1);
			long position = RiverRoutes.channel(carver, t);
			float x = PosUtil.unpackLeftf(position);
			float z = PosUtil.unpackRightf(position);
			float low = Float.MAX_VALUE;
			for (float offset : new float[] { 0.0F, side, -side }) {
				low = Math.min(low, ground(x + river.normX * offset, z + river.normZ * offset, carver.frame, heightmap));
			}
			lowest[i] = low;
		}
		// a lake at the source holds the river's first water, so it has to fit inside the lake's shore too
		for (LakePopulator lake : network.lakes()) {
			lowest[0] = Math.min(lowest[0], shore(lake, carver.innerFrame, heightmap));
		}
		float[] water = new float[samples];
		// the top water block, stepping down where the land beside the river drops below it, and up where the river
		// would otherwise cut deeper than a gorge
		int level = Integer.MAX_VALUE;
		for (int i = 0; i < samples; i++) {
			int highest = (int) Math.floor(lowest[i] * levels.worldHeight) - MARGIN;
			int deepest = (int) Math.ceil((lowest[i] - gorge) * levels.worldHeight);
			if (level > highest) {
				level = Math.floorDiv(highest, TERRACE) * TERRACE;
			}
			if (level < deepest) {
				level = Math.min(highest, Math.floorDiv(deepest + TERRACE - 1, TERRACE) * TERRACE);
			}
			// nor below the sea; the sea fills anything lower
			level = Math.max(level, levels.waterY);
			water[i] = height(level, levels);
		}
		// it meets the water it flows into at that water's level, stepping up or down to it
		water[samples - 1] = mouth;
		carver.setWaterLevels(water);

		for (LakePopulator lake : network.lakes()) {
			lake.setWaterLevel(water[0]);
		}
		for (WetlandPopulator wetland : network.wetlands()) {
			float upstream = carver.waterLevelAt(wetland.startT);
			float downstream = carver.waterLevelAt(wetland.endT);
			if (upstream - downstream > WETLAND_DROP * levels.unit) {
				wetland.disable();
			} else {
				wetland.setWaterLevel(downstream);
			}
		}
		for (Network child : network.children()) {
			float junction = child.riverCarver().junction;
			level(child, carver.waterLevelAt(junction < 0.0F ? 1.0F : junction), gorge, heightmap, levels);
		}
	}

	// a height whose block is the given one, safely inside it
	private static float height(int y, Levels levels) {
		return (y + 0.01F) / levels.worldHeight;
	}

	// the lowest ground around a lake's shore
	private static float shore(LakePopulator lake, RiverRoutes.Frame frame, Heightmap heightmap) {
		float radius = lake.radius() + 16.0F;
		float lowest = Float.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4.0D;
			float x = lake.center().x() + (float) Math.cos(angle) * radius;
			float z = lake.center().y() + (float) Math.sin(angle) * radius;
			lowest = Math.min(lowest, ground(x, z, frame, heightmap));
		}
		return lowest;
	}

	// the ground at a point in a river's space
	private static float ground(float x, float z, RiverRoutes.Frame frame, Heightmap heightmap) {
		long world = frame.toWorld(x, z);
		return heightmap.sampleGround(PosUtil.unpackLeftf(world), PosUtil.unpackRightf(world)).height;
	}
}
