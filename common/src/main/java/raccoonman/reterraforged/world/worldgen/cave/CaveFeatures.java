package raccoonman.reterraforged.world.worldgen.cave;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.chunk.ChunkAccess;
import raccoonman.reterraforged.data.preset.settings.CaveFeatureSettings;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;

/**
 * Caves shaped by the land above them, carved into each chunk once its surface is built: underground rivers and
 * springs, karst caves, lava tubes, sea caves and arches, caves along the rock layers, giant caverns, rock shelters and
 * glacier caves. The biomes underground are left as they are. Cave mouths on slopes are made in the noise router
 * instead, see PresetNoiseRouterData.
 */
public final class CaveFeatures {
	private final GeneratorContext context;
	private final List<Feature> features;

	/**
	 * One kind of cave, carved into the chunks it reaches.
	 */
	interface Feature {
		void carve(CaveCarving carving);
	}

	public CaveFeatures(GeneratorContext context) {
		this.context = context;
		CaveFeatureSettings settings = context.preset.caveFeatures();
		int seed = context.seed.root() + 90431;
		List<Feature> features = new ArrayList<>();
		if (settings.giantCaverns.enabled && settings.giantCaverns.frequency > 0.0F) {
			features.add(new GiantCaverns(seed + 1, settings.giantCaverns.frequency));
		}
		if (settings.karstCaves.enabled) {
			features.add(new KarstCaves(seed + 2, settings.karstCaves.size));
		}
		if (settings.layerCaves.enabled && settings.layerCaves.frequency > 0.0F) {
			features.add(new LayerCaves(seed + 3, settings.layerCaves.frequency));
		}
		if (settings.undergroundRivers.enabled && settings.undergroundRivers.frequency > 0.0F) {
			features.add(new UndergroundRivers(seed + 4, settings.undergroundRivers.frequency));
		}
		if (settings.lavaTubes.enabled && settings.lavaTubes.frequency > 0.0F) {
			features.add(new LavaTubes(seed + 5, settings.lavaTubes.frequency));
		}
		if (settings.seaCaves.enabled && settings.seaCaves.frequency > 0.0F) {
			features.add(new SeaCaves(seed + 6, settings.seaCaves.frequency));
		}
		if (settings.glacierCaves.enabled && settings.glacierCaves.frequency > 0.0F) {
			features.add(new GlacierCaves(seed + 7, settings.glacierCaves.frequency));
		}
		if (settings.rockShelters.enabled && settings.rockShelters.frequency > 0.0F) {
			features.add(new RockShelters(seed + 8, settings.rockShelters.frequency));
		}
		// last, so their water isn't carved away
		if (settings.springs.enabled && settings.springs.frequency > 0.0F) {
			features.add(new Springs(seed + 9, settings.springs.frequency));
		}
		this.features = List.copyOf(features);
	}

	public void carve(ChunkAccess chunk) {
		if (this.features.isEmpty()) {
			return;
		}
		CaveCarving carving = new CaveCarving(chunk, this.context);
		for (Feature feature : this.features) {
			feature.carve(carving);
		}
	}

	// 0 to 1, fixed for each grid cell and purpose
	static float random(int seed, int gridX, int gridZ, int purpose) {
		return raccoonman.reterraforged.world.worldgen.landform.Landform.random(seed, gridX, gridZ, purpose);
	}

	// samples along a path of points, every step blocks, calling the action at each
	static void along(float[] xs, float[] ys, float[] zs, float step, PointAction action) {
		for (int i = 0; i + 1 < xs.length; i++) {
			float dx = xs[i + 1] - xs[i];
			float dy = ys[i + 1] - ys[i];
			float dz = zs[i + 1] - zs[i];
			float length = (float) Math.sqrt(dx * dx + dz * dz + dy * dy);
			int count = Math.max(1, (int) Math.ceil(length / step));
			for (int j = 0; j < count; j++) {
				float t = j / (float) count;
				action.at(xs[i] + dx * t, ys[i] + dy * t, zs[i] + dz * t, i + t);
			}
		}
		int last = xs.length - 1;
		action.at(xs[last], ys[last], zs[last], last);
	}

	interface PointAction {
		void at(float x, float y, float z, float index);
	}
}
