package raccoonman.reterraforged.world.worldgen.landform;

import java.util.ArrayList;
import java.util.List;

import raccoonman.reterraforged.data.preset.settings.LandformSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.util.Seed;

// the landforms a preset turns on, applied in order
public record Landforms(List<Landform> landforms) implements Landform {

	/**
	 * @param shoreline the continent edge value at which the land meets the sea
	 */
	public static Landforms make(Seed seed, LandformSettings settings, Levels levels, float shoreline) {
		List<Landform> landforms = new ArrayList<>();
		// each landform gets its own seed, whether or not the others are on
		int buttesSeed = seed.next();
		int canyonsSeed = seed.next();
		int seaCliffsSeed = seed.next();
		int fjordsSeed = seed.next();
		int atollsSeed = seed.next();
		int dunesSeed = seed.next();
		int torsSeed = seed.next();
		// canyons cut first, so buttes can stand in them
		if (settings.canyons.enabled) {
			landforms.add(Canyons.make(canyonsSeed, settings.canyons, levels));
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
		if (settings.seaCliffs.enabled) {
			landforms.add(SeaCliffs.make(seaCliffsSeed, settings.seaCliffs, shoreline, levels));
		}
		// after the cliffs, so fjords cut through them
		if (settings.fjords.enabled) {
			landforms.add(Fjords.make(settings.fjords, shoreline, levels));
		}
		if (settings.atolls.enabled) {
			landforms.add(Atolls.make(atollsSeed, settings.atolls, shoreline, levels));
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
