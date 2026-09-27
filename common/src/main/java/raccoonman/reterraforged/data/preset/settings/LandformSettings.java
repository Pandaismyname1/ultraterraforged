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
		PresetCodecs.defaulted(Dunes.CODEC, "dunes", Dunes.makeDefault()).forGetter((o) -> o.dunes)
	).apply(instance, LandformSettings::new));

	public Buttes buttes;
	public Canyons canyons;
	public SeaCliffs seaCliffs;
	// lava in volcano craters, dark volcanic rock on their cones and old lava flows around them
	public boolean volcanicSurface;
	public Fjords fjords;
	public Atolls atolls;
	public Dunes dunes;

	public LandformSettings(Buttes buttes, Canyons canyons, SeaCliffs seaCliffs, boolean volcanicSurface, Fjords fjords, Atolls atolls, Dunes dunes) {
		this.buttes = buttes;
		this.canyons = canyons;
		this.seaCliffs = seaCliffs;
		this.volcanicSurface = volcanicSurface;
		this.fjords = fjords;
		this.atolls = atolls;
		this.dunes = dunes;
	}

	public LandformSettings copy() {
		return new LandformSettings(this.buttes.copy(), this.canyons.copy(), this.seaCliffs.copy(), this.volcanicSurface, this.fjords.copy(), this.atolls.copy(), this.dunes.copy());
	}

	public static LandformSettings makeDefault() {
		return new LandformSettings(Buttes.makeDefault(), Canyons.makeDefault(), SeaCliffs.makeDefault(), true, Fjords.makeDefault(), Atolls.makeDefault(), Dunes.makeDefault());
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
		return settings;
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
