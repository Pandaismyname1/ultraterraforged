package com.pandaismyname1.ultraterraforged.compat.streams;

import java.util.Arrays;
import java.util.List;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.flow.StreamFlowGrid;
import net.minecraft.server.level.ServerLevel;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.RiverFlow;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.Rivermap;

/**
 * The currents of UltraTerraForged's rivers in a chunk, as a layer of Streams Reflowing's flow grid: for each column of
 * river water, the way the river runs and how fast, from its bed up to its surface. Streams calls this on its own
 * worker threads, once per chunk, and keeps the grid.
 */
public final class RiverCurrents implements StreamFlowGrid.LayerProducer {
	// Streams' speeds run from 0 to 2; 1 is its default
	private static final double MAIN_SPEED = 1.0D;
	private static final double FORK_SPEED = 0.7D;

	private RiverCurrents() {
	}

	static void register() {
		StreamFlowGrid.registerLayerProducer(new RiverCurrents());
	}

	@Override
	public List<StreamFlowGrid.Layer> layers(ServerLevel level, RiverEngine engine, int chunkX, int chunkZ) {
		GeneratorContext context = StreamsCompat.context(level);
		if (context == null) {
			return List.of();
		}
		Levels levels = context.levels;
		Heightmap heightmap = context.localHeightmap.get();
		Cell cell = new Cell();
		Rivermap rivermap = null;
		byte[] angle = new byte[256];
		byte[] speed = new byte[256];
		short[] lo = new short[256];
		short[] hi = new short[256];
		Arrays.fill(angle, StreamFlowGrid.NONE);
		boolean any = false;
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				int x = (chunkX << 4) + dx;
				int z = (chunkZ << 4) + dz;
				context.lookup.apply(cell.reset(), x, z, true);
				if (cell.terrain == null || !cell.terrain.isRiver()) {
					continue;
				}
				rivermap = Rivermap.get(cell, rivermap, heightmap);
				RiverFlow.Sample sample = RiverFlow.sample(rivermap, x, z);
				if (sample == null) {
					continue;
				}
				int bed = levels.scale(cell.height);
				// raised rivers have their own surface; the others are at sea level
				int surface = cell.waterLevel > 0.0F ? levels.scale(cell.waterLevel) : levels.waterY;
				if (surface < bed) {
					continue;
				}
				int i = dz << 4 | dx;
				angle[i] = StreamFlowGrid.encode(sample.dirX(), sample.dirZ());
				speed[i] = StreamFlowGrid.packSpeed(sample.main() ? MAIN_SPEED : FORK_SPEED);
				lo[i] = (short) bed;
				hi[i] = (short) surface;
				any = true;
			}
		}
		return any ? List.of(new StreamFlowGrid.Layer(angle, speed, lo, hi)) : List.of();
	}
}
