package raccoonman.reterraforged.data.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Distinct landforms added on top of the terrain types, like buttes in badlands. Every field is optional in the file,
 * so presets saved before a landform existed get it with its default settings.
 */
public class LandformSettings {
	public static final Codec<LandformSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		PresetCodecs.defaulted(Buttes.CODEC, "buttes", Buttes.makeDefault()).forGetter((o) -> o.buttes),
		PresetCodecs.defaulted(Canyons.CODEC, "canyons", Canyons.makeDefault()).forGetter((o) -> o.canyons),
		PresetCodecs.defaulted(SeaCliffs.CODEC, "seaCliffs", SeaCliffs.makeDefault()).forGetter((o) -> o.seaCliffs),
		PresetCodecs.defaulted(Codec.BOOL, "volcanicSurface", true).forGetter((o) -> o.volcanicSurface),
		PresetCodecs.defaulted(Fjords.CODEC, "fjords", Fjords.makeDefault()).forGetter((o) -> o.fjords),
		PresetCodecs.defaulted(Atolls.CODEC, "atolls", Atolls.makeDefault()).forGetter((o) -> o.atolls),
		PresetCodecs.defaulted(Dunes.CODEC, "dunes", Dunes.makeDefault()).forGetter((o) -> o.dunes),
		PresetCodecs.defaulted(Tors.CODEC, "tors", Tors.makeDefault()).forGetter((o) -> o.tors),
		PresetCodecs.defaulted(SaltFlats.CODEC, "saltFlats", SaltFlats.makeDefault()).forGetter((o) -> o.saltFlats),
		PresetCodecs.defaulted(AlluvialFans.CODEC, "alluvialFans", AlluvialFans.makeDefault()).forGetter((o) -> o.alluvialFans),
		PresetCodecs.defaulted(GlacialValleys.CODEC, "glacialValleys", GlacialValleys.makeDefault()).forGetter((o) -> o.glacialValleys),
		PresetCodecs.defaulted(Drumlins.CODEC, "drumlins", Drumlins.makeDefault()).forGetter((o) -> o.drumlins),
		PresetCodecs.defaulted(BarrierIslands.CODEC, "barrierIslands", BarrierIslands.makeDefault()).forGetter((o) -> o.barrierIslands),
		PresetCodecs.defaulted(Karst.CODEC, "karst", Karst.makeDefault()).forGetter((o) -> o.karst),
		PresetCodecs.defaulted(Deltas.CODEC, "deltas", Deltas.makeDefault()).forGetter((o) -> o.deltas)
	).apply(instance, LandformSettings::new));

	public Buttes buttes;
	public Canyons canyons;
	public SeaCliffs seaCliffs;
	// lava in volcano craters, dark volcanic rock on their cones and old lava flows around them
	public boolean volcanicSurface;
	public Fjords fjords;
	public Atolls atolls;
	public Dunes dunes;
	public Tors tors;
	public SaltFlats saltFlats;
	public AlluvialFans alluvialFans;
	public GlacialValleys glacialValleys;
	public Drumlins drumlins;
	public BarrierIslands barrierIslands;
	public Karst karst;
	public Deltas deltas;

	public LandformSettings(Buttes buttes, Canyons canyons, SeaCliffs seaCliffs, boolean volcanicSurface, Fjords fjords, Atolls atolls, Dunes dunes, Tors tors, SaltFlats saltFlats, AlluvialFans alluvialFans, GlacialValleys glacialValleys, Drumlins drumlins, BarrierIslands barrierIslands, Karst karst, Deltas deltas) {
		this.buttes = buttes;
		this.canyons = canyons;
		this.seaCliffs = seaCliffs;
		this.volcanicSurface = volcanicSurface;
		this.fjords = fjords;
		this.atolls = atolls;
		this.dunes = dunes;
		this.tors = tors;
		this.saltFlats = saltFlats;
		this.alluvialFans = alluvialFans;
		this.glacialValleys = glacialValleys;
		this.drumlins = drumlins;
		this.barrierIslands = barrierIslands;
		this.karst = karst;
		this.deltas = deltas;
	}

	public LandformSettings copy() {
		return new LandformSettings(this.buttes.copy(), this.canyons.copy(), this.seaCliffs.copy(), this.volcanicSurface, this.fjords.copy(), this.atolls.copy(), this.dunes.copy(), this.tors.copy(), this.saltFlats.copy(), this.alluvialFans.copy(), this.glacialValleys.copy(), this.drumlins.copy(), this.barrierIslands.copy(), this.karst.copy(), this.deltas.copy());
	}

	public static LandformSettings makeDefault() {
		return new LandformSettings(Buttes.makeDefault(), Canyons.makeDefault(), SeaCliffs.makeDefault(), true, Fjords.makeDefault(), Atolls.makeDefault(), Dunes.makeDefault(), Tors.makeDefault(), SaltFlats.makeDefault(), AlluvialFans.makeDefault(), GlacialValleys.makeDefault(), Drumlins.makeDefault(), BarrierIslands.makeDefault(), Karst.makeDefault(), Deltas.makeDefault());
	}

	// how the original TerraForged generated: none of the added landforms
	public static LandformSettings makeNone() {
		LandformSettings settings = makeDefault();
		settings.buttes.enabled = false;
		settings.canyons.enabled = false;
		settings.seaCliffs.enabled = false;
		settings.volcanicSurface = false;
		settings.fjords.enabled = false;
		settings.atolls.enabled = false;
		settings.dunes.enabled = false;
		settings.tors.enabled = false;
		settings.saltFlats.enabled = false;
		settings.alluvialFans.enabled = false;
		settings.glacialValleys.enabled = false;
		settings.drumlins.enabled = false;
		settings.barrierIslands.enabled = false;
		settings.karst.enabled = false;
		settings.deltas.enabled = false;
		return settings;
	}

	/**
	 * Dead flat basins of pale salt crust on the low ground of hot, dry deserts.
	 */
	public static class SaltFlats {
		private static final SaltFlats DEFAULT = new SaltFlats(true, 0.5F);

		public static final Codec<SaltFlats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "coverage", DEFAULT.coverage).forGetter((o) -> o.coverage)
		).apply(instance, SaltFlats::new));

		public boolean enabled;
		// share of the low, flat desert ground covered
		public float coverage;

		public SaltFlats(boolean enabled, float coverage) {
			this.enabled = enabled;
			this.coverage = coverage;
		}

		public SaltFlats copy() {
			return new SaltFlats(this.enabled, this.coverage);
		}

		public static SaltFlats makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Cones of gravel and sand spread out at the foot of mountains, where steep ground meets flat land.
	 */
	public static class AlluvialFans {
		private static final AlluvialFans DEFAULT = new AlluvialFans(true, 0.6F, 1.0F);

		public static final Codec<AlluvialFans> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "density", DEFAULT.density).forGetter((o) -> o.density),
			PresetCodecs.defaulted(Codec.FLOAT, "size", DEFAULT.size).forGetter((o) -> o.size)
		).apply(instance, AlluvialFans::new));

		public boolean enabled;
		// share of the suitable spots at the foot of mountains that get one
		public float density;
		// scales how far they spread
		public float size;

		public AlluvialFans(boolean enabled, float density, float size) {
			this.enabled = enabled;
			this.density = density;
			this.size = size;
		}

		public AlluvialFans copy() {
			return new AlluvialFans(this.enabled, this.density, this.size);
		}

		public static AlluvialFans makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * U-shaped valleys with flat floors and steep walls in cold mountains, with cirques: round bowls under the peaks holding small lakes.
	 */
	public static class GlacialValleys {
		private static final GlacialValleys DEFAULT = new GlacialValleys(true, 1.0F, 0.5F);

		public static final Codec<GlacialValleys> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "strength", DEFAULT.strength).forGetter((o) -> o.strength),
			PresetCodecs.defaulted(Codec.FLOAT, "cirques", DEFAULT.cirques).forGetter((o) -> o.cirques)
		).apply(instance, GlacialValleys::new));

		public boolean enabled;
		// how strongly the valleys are worn into U shapes
		public float strength;
		// share of the suitable high slopes that get a cirque
		public float cirques;

		public GlacialValleys(boolean enabled, float strength, float cirques) {
			this.enabled = enabled;
			this.strength = strength;
			this.cirques = cirques;
		}

		public GlacialValleys copy() {
			return new GlacialValleys(this.enabled, this.strength, this.cirques);
		}

		public static GlacialValleys makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Swarms of smooth oval hills, all lying the same way, and long low ridges of rubble on cold lowlands.
	 */
	public static class Drumlins {
		private static final Drumlins DEFAULT = new Drumlins(true, 0.5F, 8);

		public static final Codec<Drumlins> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "density", DEFAULT.density).forGetter((o) -> o.density),
			PresetCodecs.defaulted(Codec.INT, "height", DEFAULT.height).forGetter((o) -> o.height)
		).apply(instance, Drumlins::new));

		public boolean enabled;
		// how thickly they cover the cold lowlands
		public float density;
		// the tallest hills rise this many blocks
		public int height;

		public Drumlins(boolean enabled, float density, int height) {
			this.enabled = enabled;
			this.density = density;
			this.height = height;
		}

		public Drumlins copy() {
			return new Drumlins(this.enabled, this.density, this.height);
		}

		public static Drumlins makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Long thin sandy islands off flat coasts, with a calm shallow lagoon behind them.
	 */
	public static class BarrierIslands {
		private static final BarrierIslands DEFAULT = new BarrierIslands(true, 0.4F);

		public static final Codec<BarrierIslands> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, BarrierIslands::new));

		public boolean enabled;
		// roughly the share of flat coastline with them
		public float frequency;

		public BarrierIslands(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public BarrierIslands copy() {
			return new BarrierIslands(this.enabled, this.frequency);
		}

		public static BarrierIslands makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Steep limestone tower hills, sinkholes and cenotes in warm, wet lands.
	 */
	public static class Karst {
		private static final Karst DEFAULT = new Karst(true, 0.5F, 40);

		public static final Codec<Karst> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "coverage", DEFAULT.coverage).forGetter((o) -> o.coverage),
			PresetCodecs.defaulted(Codec.INT, "height", DEFAULT.height).forGetter((o) -> o.height)
		).apply(instance, Karst::new));

		public boolean enabled;
		// share of the warm, wet lowland covered
		public float coverage;
		// the tallest towers rise this many blocks
		public int height;

		public Karst(boolean enabled, float coverage, int height) {
			this.enabled = enabled;
			this.coverage = coverage;
			this.height = height;
		}

		public Karst copy() {
			return new Karst(this.enabled, this.coverage, this.height);
		}

		public static Karst makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Where big rivers meet the sea on flat coasts: a fan of branching channels between marshy islands and sandbars.
	 */
	public static class Deltas {
		private static final Deltas DEFAULT = new Deltas(true, 1.0F);

		public static final Codec<Deltas> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "size", DEFAULT.size).forGetter((o) -> o.size)
		).apply(instance, Deltas::new));

		public boolean enabled;
		// scales how far they spread
		public float size;

		public Deltas(boolean enabled, float size) {
			this.enabled = enabled;
			this.size = size;
		}

		public Deltas copy() {
			return new Deltas(this.enabled, this.size);
		}

		public static Deltas makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Outcrops of bare rock on hilltops: blocky stacks of rock slabs with boulders strewn around them.
	 */
	public static class Tors {
		private static final Tors DEFAULT = new Tors(true, 0.35F, 8);

		public static final Codec<Tors> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "density", DEFAULT.density).forGetter((o) -> o.density),
			PresetCodecs.defaulted(Codec.INT, "height", DEFAULT.height).forGetter((o) -> o.height)
		).apply(instance, Tors::new));

		public boolean enabled;
		// share of possible spots that get one; only hilltops can hold one
		public float density;
		// the tallest stacks rise this many blocks above the hilltop
		public int height;

		public Tors(boolean enabled, float density, int height) {
			this.enabled = enabled;
			this.density = density;
			this.height = height;
		}

		public Tors copy() {
			return new Tors(this.enabled, this.density, this.height);
		}

		public static Tors makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Fields of sand dunes on the flat land of hot deserts, with long, wavy crests across the wind.
	 */
	public static class Dunes {
		private static final Dunes DEFAULT = new Dunes(true, 12, 0.6F);

		public static final Codec<Dunes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.INT, "height", DEFAULT.height).forGetter((o) -> o.height),
			PresetCodecs.defaulted(Codec.FLOAT, "coverage", DEFAULT.coverage).forGetter((o) -> o.coverage)
		).apply(instance, Dunes::new));

		public boolean enabled;
		// the tallest crests rise this many blocks above the ground
		public int height;
		// share of the flat desert covered in dune fields
		public float coverage;

		public Dunes(boolean enabled, int height, float coverage) {
			this.enabled = enabled;
			this.height = height;
			this.coverage = coverage;
		}

		public Dunes copy() {
			return new Dunes(this.enabled, this.height, this.coverage);
		}

		public static Dunes makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Rings of coral reef in warm seas, with sandy islets and a shallow lagoon.
	 */
	public static class Atolls {
		private static final Atolls DEFAULT = new Atolls(true, 0.35F, 1.0F);

		public static final Codec<Atolls> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency),
			PresetCodecs.defaulted(Codec.FLOAT, "size", DEFAULT.size).forGetter((o) -> o.size)
		).apply(instance, Atolls::new));

		public boolean enabled;
		// share of possible spots in warm, open sea that get one
		public float frequency;
		public float size;

		public Atolls(boolean enabled, float frequency, float size) {
			this.enabled = enabled;
			this.frequency = frequency;
			this.size = size;
		}

		public Atolls copy() {
			return new Atolls(this.enabled, this.frequency, this.size);
		}

		public static Atolls makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Drowned valleys on cold and rainy coasts: long, branching arms of the sea between steep ridges.
	 */
	public static class Fjords {
		private static final Fjords DEFAULT = new Fjords(true, 40, 1.0F);

		public static final Codec<Fjords> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.INT, "depth", DEFAULT.depth).forGetter((o) -> o.depth),
			PresetCodecs.defaulted(Codec.FLOAT, "reach", DEFAULT.reach).forGetter((o) -> o.reach)
		).apply(instance, Fjords::new));

		public boolean enabled;
		// how many blocks high ground at the coast sinks; the more, the further the sea floods up the valleys
		public int depth;
		// how far inland the land sinks
		public float reach;

		public Fjords(boolean enabled, int depth, float reach) {
			this.enabled = enabled;
			this.depth = depth;
			this.reach = reach;
		}

		public Fjords copy() {
			return new Fjords(this.enabled, this.depth, this.reach);
		}

		public static Fjords makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Stretches of coast that end in sheer cliffs over the sea, with a rocky shelf and sea stacks offshore.
	 */
	public static class SeaCliffs {
		private static final SeaCliffs DEFAULT = new SeaCliffs(true, 0.35F, 22, true, 0.5F);

		public static final Codec<SeaCliffs> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency),
			PresetCodecs.defaulted(Codec.INT, "height", DEFAULT.height).forGetter((o) -> o.height),
			PresetCodecs.defaulted(Codec.BOOL, "seaStacks", DEFAULT.seaStacks).forGetter((o) -> o.seaStacks),
			PresetCodecs.defaulted(Codec.FLOAT, "gravelBeaches", DEFAULT.gravelBeaches).forGetter((o) -> o.gravelBeaches)
		).apply(instance, SeaCliffs::new));

		public boolean enabled;
		// roughly the share of coastline with cliffs
		public float frequency;
		// the tallest cliffs rise this many blocks above the sea
		public int height;
		public boolean seaStacks;
		// share of the cliff coast with a narrow gravel beach at the foot of the cliffs
		public float gravelBeaches;

		public SeaCliffs(boolean enabled, float frequency, int height, boolean seaStacks, float gravelBeaches) {
			this.enabled = enabled;
			this.frequency = frequency;
			this.height = height;
			this.seaStacks = seaStacks;
			this.gravelBeaches = gravelBeaches;
		}

		public SeaCliffs copy() {
			return new SeaCliffs(this.enabled, this.frequency, this.height, this.seaStacks, this.gravelBeaches);
		}

		public static SeaCliffs makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Winding, branching dry canyons with stepped walls, cut into badlands and plateaus.
	 */
	public static class Canyons {
		private static final Canyons DEFAULT = new Canyons(true, 45, 1.0F, 1.0F);

		public static final Codec<Canyons> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.INT, "depth", DEFAULT.depth).forGetter((o) -> o.depth),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency),
			PresetCodecs.defaulted(Codec.FLOAT, "width", DEFAULT.width).forGetter((o) -> o.width)
		).apply(instance, Canyons::new));

		public boolean enabled;
		// how deep the main canyons cut, in blocks
		public int depth;
		// how close together they run
		public float frequency;
		public float width;

		public Canyons(boolean enabled, int depth, float frequency, float width) {
			this.enabled = enabled;
			this.depth = depth;
			this.frequency = frequency;
			this.width = width;
		}

		public Canyons copy() {
			return new Canyons(this.enabled, this.depth, this.frequency, this.width);
		}

		public static Canyons makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Flat-topped towers of rock with sheer sides, from narrow buttes to broad mesas, standing out of badlands and
	 * plateau terrain.
	 */
	public static class Buttes {
		private static final Buttes DEFAULT = new Buttes(true, 0.45F, 45, 1.0F);

		public static final Codec<Buttes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "density", DEFAULT.density).forGetter((o) -> o.density),
			PresetCodecs.defaulted(Codec.INT, "height", DEFAULT.height).forGetter((o) -> o.height),
			PresetCodecs.defaulted(Codec.FLOAT, "size", DEFAULT.size).forGetter((o) -> o.size)
		).apply(instance, Buttes::new));

		public boolean enabled;
		// share of possible spots that get one
		public float density;
		// the tallest ones rise this many blocks above the ground around them
		public int height;
		// scales their width
		public float size;

		public Buttes(boolean enabled, float density, int height, float size) {
			this.enabled = enabled;
			this.density = density;
			this.height = height;
			this.size = size;
		}

		public Buttes copy() {
			return new Buttes(this.enabled, this.density, this.height, this.size);
		}

		public static Buttes makeDefault() {
			return DEFAULT.copy();
		}
	}
}
