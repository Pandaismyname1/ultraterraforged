package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domain;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Line;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.Network;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.River;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.LakePopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.RiverPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.WetlandPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Works out where the water of each river, lake and wetland lies, from the land it runs through, so rivers flow a few
 * blocks below their banks at any height rather than all cutting down to the sea. Going from a river's source to its
 * mouth, the water drops wherever the land beside the river does, and cuts a gorge through high ground in its way; but
 * rivers are laid out without regard to the land, so where the land rises more than a gorge's depth above the water,
 * the water climbs with it, in rapids. It never stands above the land beside it, and ends at the sea, or at the level
 * of the river it flows into, stepping up or down to meet it. Lakes at a river's source hold its first water; wetlands take the level of their stretch
 * of river.
 *
 * The land is measured where the water really lies, and as the lakes leave it (issues #5 and #6, where rivers ran on
 * embankments above the land and lakes left their water high above their valleys):
 * - places in the world are taken into each river's and lake's space the way {@link Rivermap#apply} and Network.carve
 *   take them, as the warps that move rivers and lakes about can't be undone exactly; a lake's own warp moves it
 *   hundreds of blocks from where its river puts it.
 * - a lake's valley brings the land around it down towards the lake's water, so a river crossing it has to lie lower
 *   than the land it crossed before, and meets the lake's water where it runs into the lake; and a lake reaching into another's (their warp can stretch lakes out into long
 *   strips) has to lie below the other's bed. The lakes are settled first, from the land around them, then the rivers.
 * - a lake's water lies below its shore all round.
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
	// the world around rivers and lakes is gone over on a grid this fine for the places beside their water
	private static final float GRID = 8.0F;
	// how far from where undoing the warps puts a river's channel it can really lie
	private static final float SLACK = 48.0F;
	// how far a lake's own warp moves it from where its river puts it, see GenWarp
	private static final float LAKE_SHIFT = 360.0F;
	// lakes reaching into each other are settled again at most this many times, each lower than the last
	private static final int RESETTLE = 4;

	private WaterLevels() {
	}

	/**
	 * The rivers must have their frames, from {@link RiverRoutes#route}.
	 *
	 * @param gorgeDepth how many blocks a river may cut below the land beside it
	 * @param riverWarp the rivermap's warp, which takes the world into the space its rivers are laid out in
	 * @param lakeWarp which moves the lakes about in it
	 */
	public static void level(Network[] networks, int gorgeDepth, Heightmap heightmap, Domain riverWarp, Domain lakeWarp) {
		Levels levels = heightmap.levels();
		float gorge = gorgeDepth * levels.unit;
		Survey survey = new Survey(heightmap, levels, riverWarp, lakeWarp);
		for (Network network : networks) {
			survey.measure(network, new RiverPopulator[] { network.riverCarver() });
		}
		for (Network network : networks) {
			settleLakes(network, gorge, survey, levels);
		}
		for (int i = 0; i < RESETTLE && survey.reshore(); i++) {
			for (Network network : networks) {
				settleLakes(network, gorge, survey, levels);
			}
		}
		for (Network network : networks) {
			level(network, new RiverPopulator[] { network.riverCarver() }, levels.water, gorge, survey, levels);
		}
	}

	// the lakes at a river's source take its first water, from the land around them and beside the river's first stretch
	private static void settleLakes(Network network, float gorge, Survey survey, Levels levels) {
		float level = height(step(Integer.MAX_VALUE, survey.lowest(network.riverCarver(), false)[0], gorge, levels), levels);
		for (LakePopulator lake : network.lakes()) {
			lake.setWaterLevel(level);
		}
		for (Network child : network.children()) {
			settleLakes(child, gorge, survey, levels);
		}
	}

	// levels a river ending at the given water level, then the forks flowing into it
	private static void level(Network network, RiverPopulator[] chain, float mouth, float gorge, Survey survey, Levels levels) {
		RiverPopulator carver = network.riverCarver();
		float[] lowest = survey.lowest(carver, true);
		float[] water = new float[lowest.length];
		// the top water block, stepping down where the land beside the river drops below it, and up where the river
		// would otherwise cut deeper than a gorge
		int level = Integer.MAX_VALUE;
		for (int i = 0; i < lowest.length; i++) {
			level = step(level, lowest[i], gorge, levels);
			water[i] = height(level, levels);
		}
		// it meets the water it flows into at that water's level, stepping up or down to it
		water[water.length - 1] = mouth;
		carver.setWaterLevels(water);

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
			level(child, append(chain, child.riverCarver()), carver.waterLevelAt(junction < 0.0F ? 1.0F : junction), gorge, survey, levels);
		}
	}

	// the top water block of the next stretch of a river, from the last one's and the lowest land beside this one; never
	// below the sea, which fills anything lower
	private static int step(int level, float lowest, float gorge, Levels levels) {
		int highest = (int) Math.floor(lowest * levels.worldHeight) - MARGIN;
		int deepest = (int) Math.ceil((lowest - gorge) * levels.worldHeight);
		if (level > highest) {
			level = Math.floorDiv(highest, TERRACE) * TERRACE;
		}
		if (level < deepest) {
			level = Math.min(highest, Math.floorDiv(deepest + TERRACE - 1, TERRACE) * TERRACE);
		}
		return Math.max(level, levels.waterY);
	}

	// a height whose block is the given one, safely inside it
	private static float height(int y, Levels levels) {
		return (y + 0.01F) / levels.worldHeight;
	}

	private static int samples(River river) {
		return Math.max(2, (int) Math.ceil(river.length / SPACING) + 1);
	}

	private static RiverPopulator[] append(RiverPopulator[] chain, RiverPopulator carver) {
		RiverPopulator[] longer = Arrays.copyOf(chain, chain.length + 1);
		longer[chain.length] = carver;
		return longer;
	}

	/**
	 * The land beside the water of the rivers and lakes of a rivermap.
	 */
	private static final class Survey {
		private final Heightmap heightmap;
		private final Levels levels;
		private final Domain riverWarp;
		private final Domain lakeWarp;
		private final Map<RiverPopulator, Beside> rivers = new IdentityHashMap<>();
		private final List<LakeSite> lakes = new ArrayList<>();
		// the ground on the grid, measured once: forks, the rivers they flow into and the shores of lakes share places
		private final Long2FloatOpenHashMap grid = new Long2FloatOpenHashMap();

		Survey(Heightmap heightmap, Levels levels, Domain riverWarp, Domain lakeWarp) {
			this.heightmap = heightmap;
			this.levels = levels;
			this.riverWarp = riverWarp;
			this.lakeWarp = lakeWarp;
		}

		// finds the places beside the water of a river and its forks, and the land around their lakes
		void measure(Network network, RiverPopulator[] chain) {
			RiverPopulator carver = network.riverCarver();
			Beside beside = this.beside(chain);
			for (LakePopulator lake : network.lakes()) {
				LakeSite site = this.lake(lake, chain, carver.innerFrame);
				beside.lakes.add(site);
				this.lakes.add(site);
			}
			this.rivers.put(carver, beside);
			for (Network child : network.children()) {
				this.measure(child, append(chain, child.riverCarver()));
			}
		}

		/**
		 * The lowest land beside each stretch of a river's water, see {@link RiverPopulator#stretch}; the first also
		 * around the lakes at its source.
		 *
		 * @param lakes whether as the lakes' valleys leave it; their water must be settled
		 */
		float[] lowest(RiverPopulator carver, boolean lakes) {
			Beside beside = this.rivers.get(carver);
			float[] lowest = new float[beside.samples];
			Arrays.fill(lowest, Float.MAX_VALUE);
			for (int i = 0; i < beside.ground.size(); i++) {
				float x = beside.x.getFloat(i);
				float z = beside.z.getFloat(i);
				float natural = beside.ground.getFloat(i);
				float ground = natural;
				if (lakes) {
					for (LakeSite site : this.lakes) {
						if (site.reaches(x, z)) {
							float distance2 = this.lakeDistance2(site, x, z);
							ground = Math.min(ground, site.lake.valleyGround(natural, distance2));
							// the lake carves its bed under the river's water; the river meets the lake's
							if (site.lake.holds(distance2)) {
								ground = Math.min(ground, site.lake.waterLevel() + MARGIN * this.levels.unit);
							}
						}
					}
				}
				int stretch = beside.stretch.getInt(i);
				lowest[stretch] = Math.min(lowest[stretch], ground);
			}
			for (int i = 0; i < lowest.length; i++) {
				// nowhere found beside the water there; measured as before
				if (lowest[i] == Float.MAX_VALUE) {
					lowest[i] = this.estimate(carver, i / (float) (lowest.length - 1));
				}
			}
			for (LakeSite site : beside.lakes) {
				lowest[0] = Math.min(lowest[0], site.shore);
			}
			return lowest;
		}

		// the places beside a river's water, found the way Network.carve finds them, and the land there
		private Beside beside(RiverPopulator[] chain) {
			RiverPopulator carver = chain[chain.length - 1];
			River river = carver.river;
			int samples = samples(river);
			float side = carver.config.bankWidth + BEYOND_BANKS;
			float reach = side + SLACK;
			Beside beside = new Beside(samples);
			LongSet visited = new LongOpenHashSet();
			for (int i = 0; i < samples; i++) {
				long estimate = RiverRoutes.channelInWorld(carver, i / (float) (samples - 1));
				float centerX = PosUtil.unpackLeftf(estimate);
				float centerZ = PosUtil.unpackRightf(estimate);
				for (int gz = NoiseUtil.floor((centerZ - reach) / GRID); gz <= NoiseUtil.floor((centerZ + reach) / GRID); gz++) {
					for (int gx = NoiseUtil.floor((centerX - reach) / GRID); gx <= NoiseUtil.floor((centerX + reach) / GRID); gx++) {
						if (!visited.add(PosUtil.pack(gx, gz))) {
							continue;
						}
						float x = gx * GRID;
						float z = gz * GRID;
						long space = this.toRiver(chain, this.toRivermap(x, z));
						float rx = PosUtil.unpackLeftf(space);
						float rz = PosUtil.unpackRightf(space);
						float t = Line.distanceOnLine(rx, rz, river.x1, river.z1, river.x2, river.z2);
						if (carver.distance2(rx, rz, t) > side * side) {
							continue;
						}
						beside.add(RiverPopulator.stretch(t, samples), x, z, this.ground(gx, gz));
					}
				}
			}
			return beside;
		}

		// where a lake really lies, and the lowest land on its shore
		private LakeSite lake(LakePopulator lake, RiverPopulator[] chain, RiverRoutes.Frame frame) {
			long estimate = frame.toWorld(lake.center().x(), lake.center().y());
			float centerX = PosUtil.unpackLeftf(estimate);
			float centerZ = PosUtil.unpackRightf(estimate);
			float inner = lake.radius();
			float outer = lake.shoreRadius();
			float reach = outer + LAKE_SHIFT;
			LakeSite site = new LakeSite(lake, chain);
			for (int gz = NoiseUtil.floor((centerZ - reach) / GRID); gz <= NoiseUtil.floor((centerZ + reach) / GRID); gz++) {
				for (int gx = NoiseUtil.floor((centerX - reach) / GRID); gx <= NoiseUtil.floor((centerX + reach) / GRID); gx++) {
					float x = gx * GRID;
					float z = gz * GRID;
					float distance2 = this.lakeDistance2(lake, chain, x, z);
					if (distance2 > inner * inner && distance2 <= outer * outer) {
						site.add(x, z, this.ground(gx, gz));
					}
				}
			}
			// nowhere found around it; measured as before
			if (site.shore == Float.MAX_VALUE) {
				site.shore = this.shore(lake, frame);
			}
			float valley = lake.valleyRadius() + LAKE_SHIFT + SLACK;
			site.reach(centerX - valley, centerZ - valley, centerX + valley, centerZ + valley);
			return site;
		}

		/**
		 * Measures the shores of the lakes again, as the other lakes leave them, with the water the lakes have now.
		 *
		 * @return whether any shore is lower than it was
		 */
		boolean reshore() {
			boolean lower = false;
			for (LakeSite site : this.lakes) {
				float shore = site.shore;
				for (int i = 0; i < site.ground.size(); i++) {
					float x = site.x.getFloat(i);
					float z = site.z.getFloat(i);
					float natural = site.ground.getFloat(i);
					for (LakeSite other : this.lakes) {
						if (other != site && other.reaches(x, z)) {
							shore = Math.min(shore, other.lake.carvedGround(natural, this.lakeDistance2(other, x, z)));
						}
					}
				}
				// by more than a rounding error, so this ends
				if (shore < site.shore - 0.5F / this.levels.worldHeight) {
					site.shore = shore;
					lower = true;
				}
			}
			return lower;
		}

		private float lakeDistance2(LakeSite site, float x, float z) {
			return this.lakeDistance2(site.lake, site.chain, x, z);
		}

		// how far a place in the world is from the middle of a lake, squared, in the lake's space: Rivermap.apply moves
		// lakes by the lake warp of the place in the rivermap's space
		private float lakeDistance2(LakePopulator lake, RiverPopulator[] chain, float x, float z) {
			long rivermap = this.toRivermap(x, z);
			float rx = PosUtil.unpackLeftf(rivermap);
			float rz = PosUtil.unpackRightf(rivermap);
			long space = this.toRiver(chain, rivermap);
			float lx = PosUtil.unpackLeftf(space) + this.lakeWarp.getOffsetX(rx, rz, 0);
			float lz = PosUtil.unpackRightf(space) + this.lakeWarp.getOffsetZ(rx, rz, 0);
			return lake.distance2(lx, lz);
		}

		private long toRivermap(float x, float z) {
			return PosUtil.packf(this.riverWarp.getX(x, z, 0), this.riverWarp.getZ(x, z, 0));
		}

		// a place in the rivermap's space, in the space of the last river of the chain, moved by each river from the main
		// one down
		private long toRiver(RiverPopulator[] chain, long rivermap) {
			long space = rivermap;
			for (RiverPopulator carver : chain) {
				space = RiverRoutes.carve(carver, PosUtil.unpackLeftf(space), PosUtil.unpackRightf(space));
			}
			return space;
		}

		private float ground(float x, float z) {
			return this.heightmap.sampleGround(x, z).height;
		}

		private float ground(int gridX, int gridZ) {
			long key = PosUtil.pack(gridX, gridZ);
			if (this.grid.containsKey(key)) {
				return this.grid.get(key);
			}
			float ground = this.ground(gridX * GRID, gridZ * GRID);
			this.grid.put(key, ground);
			return ground;
		}

		// the lowest land at a river's middle and either side of it, where undoing the warps puts them
		private float estimate(RiverPopulator carver, float t) {
			River river = carver.river;
			long position = RiverRoutes.channel(carver, t);
			float x = PosUtil.unpackLeftf(position);
			float z = PosUtil.unpackRightf(position);
			float side = carver.config.bankWidth + BEYOND_BANKS;
			float low = Float.MAX_VALUE;
			for (float offset : new float[] { 0.0F, side, -side }) {
				long world = carver.frame.toWorld(x + river.normX * offset, z + river.normZ * offset);
				low = Math.min(low, this.ground(PosUtil.unpackLeftf(world), PosUtil.unpackRightf(world)));
			}
			return low;
		}

		// the lowest ground around a lake's shore, where undoing the warps puts it
		private float shore(LakePopulator lake, RiverRoutes.Frame frame) {
			float radius = lake.radius() + 16.0F;
			float lowest = Float.MAX_VALUE;
			for (int i = 0; i < 8; i++) {
				double angle = i * Math.PI / 4.0D;
				long world = frame.toWorld(lake.center().x() + (float) Math.cos(angle) * radius, lake.center().y() + (float) Math.sin(angle) * radius);
				lowest = Math.min(lowest, this.ground(PosUtil.unpackLeftf(world), PosUtil.unpackRightf(world)));
			}
			return lowest;
		}
	}

	// the places beside a river's water: which stretch of it each is beside, and the land there
	private static final class Beside {
		private final int samples;
		private final IntArrayList stretch = new IntArrayList();
		private final FloatArrayList x = new FloatArrayList();
		private final FloatArrayList z = new FloatArrayList();
		private final FloatArrayList ground = new FloatArrayList();
		private final List<LakeSite> lakes = new ArrayList<>();

		Beside(int samples) {
			this.samples = samples;
		}

		void add(int stretch, float x, float z, float ground) {
			this.stretch.add(stretch);
			this.x.add(x);
			this.z.add(z);
			this.ground.add(ground);
		}
	}

	/**
	 * A lake: the places on its shore and the land there, the lowest of it, and the part of the world its valley can reach.
	 */
	private static final class LakeSite {
		private final LakePopulator lake;
		private final RiverPopulator[] chain;
		private final FloatArrayList x = new FloatArrayList();
		private final FloatArrayList z = new FloatArrayList();
		private final FloatArrayList ground = new FloatArrayList();
		private float shore = Float.MAX_VALUE;
		private float minX, minZ, maxX, maxZ;

		LakeSite(LakePopulator lake, RiverPopulator[] chain) {
			this.lake = lake;
			this.chain = chain;
		}

		void add(float x, float z, float ground) {
			this.x.add(x);
			this.z.add(z);
			this.ground.add(ground);
			this.shore = Math.min(this.shore, ground);
		}

		void reach(float minX, float minZ, float maxX, float maxZ) {
			this.minX = minX;
			this.minZ = minZ;
			this.maxX = maxX;
			this.maxZ = maxZ;
		}

		boolean reaches(float x, float z) {
			return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
		}
	}
}
