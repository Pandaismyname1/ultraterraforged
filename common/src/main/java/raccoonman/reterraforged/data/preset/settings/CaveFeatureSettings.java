package raccoonman.reterraforged.data.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Caves shaped by the land above them: underground rivers and springs, karst caverns, lava tubes, sea caves and arches, caves along the rock layers, giant caverns, rock shelters, glacier caves, and cave mouths where they belong. Every field is optional in the file, so presets saved before a feature existed get it with its default
 * settings.
 */
public class CaveFeatureSettings {
	public static final Codec<CaveFeatureSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		PresetCodecs.defaulted(UndergroundRivers.CODEC, "undergroundRivers", UndergroundRivers.makeDefault()).forGetter((o) -> o.undergroundRivers),
		PresetCodecs.defaulted(Springs.CODEC, "springs", Springs.makeDefault()).forGetter((o) -> o.springs),
		PresetCodecs.defaulted(KarstCaves.CODEC, "karstCaves", KarstCaves.makeDefault()).forGetter((o) -> o.karstCaves),
		PresetCodecs.defaulted(LavaTubes.CODEC, "lavaTubes", LavaTubes.makeDefault()).forGetter((o) -> o.lavaTubes),
		PresetCodecs.defaulted(SeaCaves.CODEC, "seaCaves", SeaCaves.makeDefault()).forGetter((o) -> o.seaCaves),
		PresetCodecs.defaulted(LayerCaves.CODEC, "layerCaves", LayerCaves.makeDefault()).forGetter((o) -> o.layerCaves),
		PresetCodecs.defaulted(GiantCaverns.CODEC, "giantCaverns", GiantCaverns.makeDefault()).forGetter((o) -> o.giantCaverns),
		PresetCodecs.defaulted(RockShelters.CODEC, "rockShelters", RockShelters.makeDefault()).forGetter((o) -> o.rockShelters),
		PresetCodecs.defaulted(GlacierCaves.CODEC, "glacierCaves", GlacierCaves.makeDefault()).forGetter((o) -> o.glacierCaves),
		PresetCodecs.defaulted(CaveMouths.CODEC, "caveMouths", CaveMouths.makeDefault()).forGetter((o) -> o.caveMouths)
	).apply(instance, CaveFeatureSettings::new));

	public UndergroundRivers undergroundRivers;
	public Springs springs;
	public KarstCaves karstCaves;
	public LavaTubes lavaTubes;
	public SeaCaves seaCaves;
	public LayerCaves layerCaves;
	public GiantCaverns giantCaverns;
	public RockShelters rockShelters;
	public GlacierCaves glacierCaves;
	public CaveMouths caveMouths;

	public CaveFeatureSettings(UndergroundRivers undergroundRivers, Springs springs, KarstCaves karstCaves, LavaTubes lavaTubes, SeaCaves seaCaves, LayerCaves layerCaves, GiantCaverns giantCaverns, RockShelters rockShelters, GlacierCaves glacierCaves, CaveMouths caveMouths) {
		this.undergroundRivers = undergroundRivers;
		this.springs = springs;
		this.karstCaves = karstCaves;
		this.lavaTubes = lavaTubes;
		this.seaCaves = seaCaves;
		this.layerCaves = layerCaves;
		this.giantCaverns = giantCaverns;
		this.rockShelters = rockShelters;
		this.glacierCaves = glacierCaves;
		this.caveMouths = caveMouths;
	}

	public CaveFeatureSettings copy() {
		return new CaveFeatureSettings(this.undergroundRivers.copy(), this.springs.copy(), this.karstCaves.copy(), this.lavaTubes.copy(), this.seaCaves.copy(), this.layerCaves.copy(), this.giantCaverns.copy(), this.rockShelters.copy(), this.glacierCaves.copy(), this.caveMouths.copy());
	}

	public static CaveFeatureSettings makeDefault() {
		return new CaveFeatureSettings(UndergroundRivers.makeDefault(), Springs.makeDefault(), KarstCaves.makeDefault(), LavaTubes.makeDefault(), SeaCaves.makeDefault(), LayerCaves.makeDefault(), GiantCaverns.makeDefault(), RockShelters.makeDefault(), GlacierCaves.makeDefault(), CaveMouths.makeDefault());
	}

	// how the original TerraForged generated: none of them
	public static CaveFeatureSettings makeNone() {
		CaveFeatureSettings settings = makeDefault();
		settings.undergroundRivers.enabled = false;
		settings.springs.enabled = false;
		settings.karstCaves.enabled = false;
		settings.lavaTubes.enabled = false;
		settings.seaCaves.enabled = false;
		settings.layerCaves.enabled = false;
		settings.giantCaverns.enabled = false;
		settings.rockShelters.enabled = false;
		settings.glacierCaves.enabled = false;
		settings.caveMouths.enabled = false;
		return settings;
	}

	/**
	 * Long winding tunnels deep underground with water along their floor.
	 */
	public static class UndergroundRivers {
		private static final UndergroundRivers DEFAULT = new UndergroundRivers(true, 0.4F);

		public static final Codec<UndergroundRivers> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, UndergroundRivers::new));

		public boolean enabled;
		// how many there are
		public float frequency;

		public UndergroundRivers(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public UndergroundRivers copy() {
			return new UndergroundRivers(this.enabled, this.frequency);
		}

		public static UndergroundRivers makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Water welling out of small caves in hillsides.
	 */
	public static class Springs {
		private static final Springs DEFAULT = new Springs(true, 0.3F);

		public static final Codec<Springs> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, Springs::new));

		public boolean enabled;
		// how many hillsides have one
		public float frequency;

		public Springs(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public Springs copy() {
			return new Springs(this.enabled, this.frequency);
		}

		public static Springs makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Great chambers and passages under karst country.
	 */
	public static class KarstCaves {
		private static final KarstCaves DEFAULT = new KarstCaves(true, 1.0F);

		public static final Codec<KarstCaves> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "size", DEFAULT.size).forGetter((o) -> o.size)
		).apply(instance, KarstCaves::new));

		public boolean enabled;
		// scales the chambers
		public float size;

		public KarstCaves(boolean enabled, float size) {
			this.enabled = enabled;
			this.size = size;
		}

		public KarstCaves copy() {
			return new KarstCaves(this.enabled, this.size);
		}

		public static KarstCaves makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Long round tunnels running downhill from volcanoes, lined with basalt.
	 */
	public static class LavaTubes {
		private static final LavaTubes DEFAULT = new LavaTubes(true, 0.6F);

		public static final Codec<LavaTubes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, LavaTubes::new));

		public boolean enabled;
		// how many run from each volcano
		public float frequency;

		public LavaTubes(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public LavaTubes copy() {
			return new LavaTubes(this.enabled, this.frequency);
		}

		public static LavaTubes makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Caves cut into sea cliffs at the waterline, and arches through headlands.
	 */
	public static class SeaCaves {
		private static final SeaCaves DEFAULT = new SeaCaves(true, 0.5F);

		public static final Codec<SeaCaves> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, SeaCaves::new));

		public boolean enabled;
		// how many the cliffs have
		public float frequency;

		public SeaCaves(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public SeaCaves copy() {
			return new SeaCaves(this.enabled, this.frequency);
		}

		public static SeaCaves makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Wide low caves running level along the rock layers.
	 */
	public static class LayerCaves {
		private static final LayerCaves DEFAULT = new LayerCaves(true, 0.4F);

		public static final Codec<LayerCaves> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, LayerCaves::new));

		public boolean enabled;
		// how much of the rock they run through
		public float frequency;

		public LayerCaves(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public LayerCaves copy() {
			return new LayerCaves(this.enabled, this.frequency);
		}

		public static LayerCaves makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Rare huge chambers deep underground with pillars and a lake.
	 */
	public static class GiantCaverns {
		private static final GiantCaverns DEFAULT = new GiantCaverns(true, 0.3F);

		public static final Codec<GiantCaverns> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, GiantCaverns::new));

		public boolean enabled;
		// how many there are
		public float frequency;

		public GiantCaverns(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public GiantCaverns copy() {
			return new GiantCaverns(this.enabled, this.frequency);
		}

		public static GiantCaverns makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Overhangs worn into the foot of cliffs in badlands and canyons.
	 */
	public static class RockShelters {
		private static final RockShelters DEFAULT = new RockShelters(true, 0.6F);

		public static final Codec<RockShelters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, RockShelters::new));

		public boolean enabled;
		// how much of the cliff foot is worn in
		public float frequency;

		public RockShelters(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public RockShelters copy() {
			return new RockShelters(this.enabled, this.frequency);
		}

		public static RockShelters makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Tunnels of blue ice leading into the mountainside in glacial country.
	 */
	public static class GlacierCaves {
		private static final GlacierCaves DEFAULT = new GlacierCaves(true, 0.5F);

		public static final Codec<GlacierCaves> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, GlacierCaves::new));

		public boolean enabled;
		// how many there are
		public float frequency;

		public GlacierCaves(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public GlacierCaves copy() {
			return new GlacierCaves(this.enabled, this.frequency);
		}

		public static GlacierCaves makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Cave entrances in hillsides and cliffs rather than holes in flat fields.
	 */
	public static class CaveMouths {
		private static final CaveMouths DEFAULT = new CaveMouths(true, 1.0F);

		public static final Codec<CaveMouths> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "strength", DEFAULT.strength).forGetter((o) -> o.strength)
		).apply(instance, CaveMouths::new));

		public boolean enabled;
		// how strongly entrances keep to slopes
		public float strength;

		public CaveMouths(boolean enabled, float strength) {
			this.enabled = enabled;
			this.strength = strength;
		}

		public CaveMouths copy() {
			return new CaveMouths(this.enabled, this.strength);
		}

		public static CaveMouths makeDefault() {
			return DEFAULT.copy();
		}
	}
}
