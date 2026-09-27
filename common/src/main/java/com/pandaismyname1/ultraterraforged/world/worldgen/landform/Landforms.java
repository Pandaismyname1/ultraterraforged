package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import java.util.ArrayList;
import java.util.List;

import com.pandaismyname1.ultraterraforged.data.preset.settings.CoastSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.LandformSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.OceanSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.Seed;

// the landforms a preset turns on, applied in order
public record Landforms(List<Landform> landforms) implements Landform {

	/**
	 * @param shoreline the continent edge value at which the land meets the sea
	 */
	public static Landforms make(Seed seed, LandformSettings settings, CoastSettings coasts, OceanSettings oceans, Levels levels, float shoreline) {
		List<Landform> landforms = new ArrayList<>();
		// each landform gets its own seed, whether or not the others are on
		int buttesSeed = seed.next();
		int canyonsSeed = seed.next();
		int seaCliffsSeed = seed.next();
		int fjordsSeed = seed.next();
		int atollsSeed = seed.next();
		int dunesSeed = seed.next();
		int torsSeed = seed.next();
		int saltFlatsSeed = seed.next();
		int alluvialFansSeed = seed.next();
		int glacialValleysSeed = seed.next();
		int drumlinsSeed = seed.next();
		int barrierIslandsSeed = seed.next();
		int karstSeed = seed.next();
		int deltasSeed = seed.next();
		int skerriesSeed = seed.next();
		int islandArcsSeed = seed.next();
		int riverIslandsSeed = seed.next();
		int oceansSeed = seed.next();
		// canyons cut first, so buttes can stand in them
		if (settings.canyons.enabled) {
			landforms.add(Canyons.make(canyonsSeed, settings.canyons, levels));
		}
		if (settings.karst.enabled) {
			landforms.add(Karst.make(karstSeed, settings.karst, levels));
		}
		// the sea floor, before the coast's features stand on it
		if (oceans.shelves.enabled) {
			landforms.add(Shelves.make(oceansSeed, oceans.shelves, shoreline, levels));
		}
		if (oceans.submarineCanyons.enabled) {
			landforms.add(SubmarineCanyons.make(oceansSeed + 1, oceans.submarineCanyons, shoreline, levels));
		}
		if (oceans.trenches.enabled) {
			landforms.add(Trenches.make(oceansSeed + 2, oceans.trenches, shoreline, levels));
		}
		if (oceans.seamounts.enabled) {
			landforms.add(Seamounts.make(oceansSeed + 3, oceans.seamounts, shoreline, levels));
		}
		if (oceans.ridges.enabled) {
			landforms.add(OceanRidges.make(oceansSeed + 4, oceans.ridges, shoreline, levels));
		}
		if (oceans.blueHoles.enabled) {
			landforms.add(BlueHoles.make(oceansSeed + 5, oceans.blueHoles, levels));
		}
		// before the dunes, which keep off them
		if (settings.saltFlats.enabled) {
			landforms.add(SaltFlats.make(saltFlatsSeed, settings.saltFlats, levels));
		}
		// before the buttes, so their flat tops stay flat
		if (settings.dunes.enabled) {
			landforms.add(Dunes.make(dunesSeed, settings.dunes, levels));
		}
		if (settings.buttes.enabled) {
			landforms.add(Buttes.make(buttesSeed, settings.buttes, levels));
		}
		if (settings.tors.enabled) {
			landforms.add(Tors.make(torsSeed, settings.tors, levels));
		}
		if (settings.glacialValleys.enabled) {
			landforms.add(GlacialValleys.make(glacialValleysSeed, settings.glacialValleys, levels));
		}
		// at the foot of the mountains, as the valleys leave them
		if (settings.alluvialFans.enabled) {
			landforms.add(AlluvialFans.make(alluvialFansSeed, settings.alluvialFans, levels));
		}
		if (settings.drumlins.enabled) {
			landforms.add(Drumlins.make(drumlinsSeed, settings.drumlins, levels));
		}
		SeaCliffs seaCliffs = null;
		if (settings.seaCliffs.enabled) {
			seaCliffs = SeaCliffs.make(seaCliffsSeed, settings.seaCliffs, shoreline, levels);
			landforms.add(seaCliffs);
		}
		// after the cliffs, so fjords cut through them
		if (settings.fjords.enabled) {
			landforms.add(Fjords.make(settings.fjords, shoreline, levels));
		}
		if (settings.atolls.enabled) {
			landforms.add(Atolls.make(atollsSeed, settings.atolls, shoreline, levels));
		}
		if (settings.barrierIslands.enabled) {
			landforms.add(BarrierIslands.make(barrierIslandsSeed, settings.barrierIslands, shoreline, levels, seaCliffs));
		}
		if (coasts.skerries.enabled) {
			landforms.add(Skerries.make(skerriesSeed, coasts.skerries, shoreline, levels));
		}
		if (coasts.islandArcs.enabled) {
			landforms.add(IslandArcs.make(islandArcsSeed, coasts.islandArcs, shoreline, levels));
		}
		if (oceans.coralReefs.enabled) {
			landforms.add(CoralReefs.make(oceansSeed + 6, oceans.coralReefs, shoreline, levels));
		}
		if (oceans.sandWaves.enabled) {
			landforms.add(SandWaves.make(oceansSeed + 7, oceans.sandWaves, levels));
		}
		// last, over the coast the others shaped
		if (settings.deltas.enabled) {
			landforms.add(Deltas.make(deltasSeed, settings.deltas, shoreline, levels));
		}
		if (coasts.riverIslands.enabled) {
			landforms.add(RiverIslands.make(riverIslandsSeed, coasts.riverIslands, levels));
		}
		return new Landforms(List.copyOf(landforms));
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		for (Landform landform : this.landforms) {
			landform.apply(cell, x, z, heightmap);
		}
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Landforms(this.landforms.stream().map((landform) -> landform.mapNoise(visitor)).toList());
	}
}
