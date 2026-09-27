package raccoonman.reterraforged.data.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The shape of the coast: peninsulas, headlands and bays, islands off the coast, spits and tombolos, skerries, volcanic island arcs, and islands in lakes and rivers. Every field is optional in the file, so presets saved before a feature existed get it with its default
 * settings.
 */
public class CoastSettings {
	public static final Codec<CoastSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		PresetCodecs.defaulted(Headlands.CODEC, "headlands", Headlands.makeDefault()).forGetter((o) -> o.headlands),
		PresetCodecs.defaulted(Peninsulas.CODEC, "peninsulas", Peninsulas.makeDefault()).forGetter((o) -> o.peninsulas),
		PresetCodecs.defaulted(CoastalIslands.CODEC, "coastalIslands", CoastalIslands.makeDefault()).forGetter((o) -> o.coastalIslands),
		PresetCodecs.defaulted(Spits.CODEC, "spits", Spits.makeDefault()).forGetter((o) -> o.spits),
		PresetCodecs.defaulted(Skerries.CODEC, "skerries", Skerries.makeDefault()).forGetter((o) -> o.skerries),
		PresetCodecs.defaulted(IslandArcs.CODEC, "islandArcs", IslandArcs.makeDefault()).forGetter((o) -> o.islandArcs),
		PresetCodecs.defaulted(RiverIslands.CODEC, "riverIslands", RiverIslands.makeDefault()).forGetter((o) -> o.riverIslands)
	).apply(instance, CoastSettings::new));

	public Headlands headlands;
	public Peninsulas peninsulas;
	public CoastalIslands coastalIslands;
	public Spits spits;
	public Skerries skerries;
	public IslandArcs islandArcs;
	public RiverIslands riverIslands;

	public CoastSettings(Headlands headlands, Peninsulas peninsulas, CoastalIslands coastalIslands, Spits spits, Skerries skerries, IslandArcs islandArcs, RiverIslands riverIslands) {
		this.headlands = headlands;
		this.peninsulas = peninsulas;
		this.coastalIslands = coastalIslands;
		this.spits = spits;
		this.skerries = skerries;
		this.islandArcs = islandArcs;
		this.riverIslands = riverIslands;
	}

	public CoastSettings copy() {
		return new CoastSettings(this.headlands.copy(), this.peninsulas.copy(), this.coastalIslands.copy(), this.spits.copy(), this.skerries.copy(), this.islandArcs.copy(), this.riverIslands.copy());
	}

	public static CoastSettings makeDefault() {
		return new CoastSettings(Headlands.makeDefault(), Peninsulas.makeDefault(), CoastalIslands.makeDefault(), Spits.makeDefault(), Skerries.makeDefault(), IslandArcs.makeDefault(), RiverIslands.makeDefault());
	}

	// how the original TerraForged generated: none of them
	public static CoastSettings makeNone() {
		CoastSettings settings = makeDefault();
		settings.headlands.enabled = false;
		settings.peninsulas.enabled = false;
		settings.coastalIslands.enabled = false;
		settings.spits.enabled = false;
		settings.skerries.enabled = false;
		settings.islandArcs.enabled = false;
		settings.riverIslands.enabled = false;
		return settings;
	}

	/**
	 * A ragged coastline: points reaching out into the sea between sheltered bays.
	 */
	public static class Headlands {
		private static final Headlands DEFAULT = new Headlands(true, 1.0F);

		public static final Codec<Headlands> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "strength", DEFAULT.strength).forGetter((o) -> o.strength)
		).apply(instance, Headlands::new));

		public boolean enabled;
		// how far the points reach out and the bays cut in
		public float strength;

		public Headlands(boolean enabled, float strength) {
			this.enabled = enabled;
			this.strength = strength;
		}

		public Headlands copy() {
			return new Headlands(this.enabled, this.strength);
		}

		public static Headlands makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Long arms of land reaching out from the coast into the sea.
	 */
	public static class Peninsulas {
		private static final Peninsulas DEFAULT = new Peninsulas(true, 0.5F, 1.0F);

		public static final Codec<Peninsulas> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency),
			PresetCodecs.defaulted(Codec.FLOAT, "size", DEFAULT.size).forGetter((o) -> o.size)
		).apply(instance, Peninsulas::new));

		public boolean enabled;
		// share of the suitable places along the coast that get one
		public float frequency;
		// scales how long and broad they are
		public float size;

		public Peninsulas(boolean enabled, float frequency, float size) {
			this.enabled = enabled;
			this.frequency = frequency;
			this.size = size;
		}

		public Peninsulas copy() {
			return new Peninsulas(this.enabled, this.frequency, this.size);
		}

		public static Peninsulas makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Islands a little way off the coast, some tied to the mainland by a sandbar.
	 */
	public static class CoastalIslands {
		private static final CoastalIslands DEFAULT = new CoastalIslands(true, 0.5F, 1.0F, 0.35F);

		public static final Codec<CoastalIslands> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency),
			PresetCodecs.defaulted(Codec.FLOAT, "size", DEFAULT.size).forGetter((o) -> o.size),
			PresetCodecs.defaulted(Codec.FLOAT, "tombolos", DEFAULT.tombolos).forGetter((o) -> o.tombolos)
		).apply(instance, CoastalIslands::new));

		public boolean enabled;
		// share of the suitable places off the coast that get one
		public float frequency;
		// scales how big they are
		public float size;
		// share of the islands tied to the mainland by a sandbar
		public float tombolos;

		public CoastalIslands(boolean enabled, float frequency, float size, float tombolos) {
			this.enabled = enabled;
			this.frequency = frequency;
			this.size = size;
			this.tombolos = tombolos;
		}

		public CoastalIslands copy() {
			return new CoastalIslands(this.enabled, this.frequency, this.size, this.tombolos);
		}

		public static CoastalIslands makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Narrow bars of sand curving out from the coast into the sea.
	 */
	public static class Spits {
		private static final Spits DEFAULT = new Spits(true, 0.4F);

		public static final Codec<Spits> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, Spits::new));

		public boolean enabled;
		// share of the suitable places along the coast that get one
		public float frequency;

		public Spits(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public Spits copy() {
			return new Spits(this.enabled, this.frequency);
		}

		public static Spits makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Swarms of small bare rocky islets off cold coasts.
	 */
	public static class Skerries {
		private static final Skerries DEFAULT = new Skerries(true, 0.5F);

		public static final Codec<Skerries> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "density", DEFAULT.density).forGetter((o) -> o.density)
		).apply(instance, Skerries::new));

		public boolean enabled;
		// how thickly they crowd the sea
		public float density;

		public Skerries(boolean enabled, float density) {
			this.enabled = enabled;
			this.density = density;
		}

		public Skerries copy() {
			return new Skerries(this.enabled, this.density);
		}

		public static Skerries makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Curved chains of volcanic islands out in the open sea.
	 */
	public static class IslandArcs {
		private static final IslandArcs DEFAULT = new IslandArcs(true, 0.3F);

		public static final Codec<IslandArcs> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, IslandArcs::new));

		public boolean enabled;
		// share of the suitable places out at sea that get one
		public float frequency;

		public IslandArcs(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public IslandArcs copy() {
			return new IslandArcs(this.enabled, this.frequency);
		}

		public static IslandArcs makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Islands in lakes, and long islands splitting wide rivers.
	 */
	public static class RiverIslands {
		private static final RiverIslands DEFAULT = new RiverIslands(true, 0.5F);

		public static final Codec<RiverIslands> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, RiverIslands::new));

		public boolean enabled;
		// how many islands the lakes and wide rivers have
		public float frequency;

		public RiverIslands(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public RiverIslands copy() {
			return new RiverIslands(this.enabled, this.frequency);
		}

		public static RiverIslands makeDefault() {
			return DEFAULT.copy();
		}
	}
}
