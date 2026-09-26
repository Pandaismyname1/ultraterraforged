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

	public static Landforms make(Seed seed, LandformSettings settings, Levels levels) {
		List<Landform> landforms = new ArrayList<>();
		// each landform gets its own seed, whether or not the others are on
		int buttesSeed = seed.next();
		int canyonsSeed = seed.next();
		// canyons cut first, so buttes can stand in them
		if (settings.canyons.enabled) {
			landforms.add(Canyons.make(canyonsSeed, settings.canyons, levels));
		}
		if (settings.buttes.enabled) {
			landforms.add(Buttes.make(buttesSeed, settings.buttes, levels));
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
