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
		PresetCodecs.defaulted(SeaCliffs.CODEC, "seaCliffs", SeaCliffs.makeDefault()).forGetter((o) -> o.seaCliffs)
	).apply(instance, LandformSettings::new));

	public Buttes buttes;
	public Canyons canyons;
	public SeaCliffs seaCliffs;

	public LandformSettings(Buttes buttes, Canyons canyons, SeaCliffs seaCliffs) {
		this.buttes = buttes;
		this.canyons = canyons;
		this.seaCliffs = seaCliffs;
	}

	public LandformSettings copy() {
		return new LandformSettings(this.buttes.copy(), this.canyons.copy(), this.seaCliffs.copy());
	}

	public static LandformSettings makeDefault() {
		return new LandformSettings(Buttes.makeDefault(), Canyons.makeDefault(), SeaCliffs.makeDefault());
	}

	// how the original TerraForged generated: none of the added landforms
	public static LandformSettings makeNone() {
		LandformSettings settings = makeDefault();
		settings.buttes.enabled = false;
		settings.canyons.enabled = false;
		settings.seaCliffs.enabled = false;
		return settings;
	}

	/**
	 * Stretches of coast that end in sheer cliffs over the sea, with a rocky shelf and sea stacks offshore.
	 */
	public static class SeaCliffs {
		private static final SeaCliffs DEFAULT = new SeaCliffs(true, 0.35F, 22, true);

		public static final Codec<SeaCliffs> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency),
			PresetCodecs.defaulted(Codec.INT, "height", DEFAULT.height).forGetter((o) -> o.height),
			PresetCodecs.defaulted(Codec.BOOL, "seaStacks", DEFAULT.seaStacks).forGetter((o) -> o.seaStacks)
		).apply(instance, SeaCliffs::new));

		public boolean enabled;
		// roughly the share of coastline with cliffs
		public float frequency;
		// the tallest cliffs rise this many blocks above the sea
		public int height;
		public boolean seaStacks;

		public SeaCliffs(boolean enabled, float frequency, int height, boolean seaStacks) {
			this.enabled = enabled;
			this.frequency = frequency;
			this.height = height;
			this.seaStacks = seaStacks;
		}

		public SeaCliffs copy() {
			return new SeaCliffs(this.enabled, this.frequency, this.height, this.seaStacks);
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
