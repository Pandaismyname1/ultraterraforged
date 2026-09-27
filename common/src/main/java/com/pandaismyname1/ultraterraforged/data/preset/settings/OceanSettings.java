package com.pandaismyname1.ultraterraforged.data.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The shape of the sea floor, and what's to be found there: continental shelves, submarine canyons, trenches, seamounts and guyots, mid-ocean ridges with hydrothermal vents, blue holes, coral reefs, sand waves and whale falls. Every field is optional in the file, so presets saved before a feature existed get it with its default
 * settings.
 */
public class OceanSettings {
	public static final Codec<OceanSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		PresetCodecs.defaulted(Shelves.CODEC, "shelves", Shelves.makeDefault()).forGetter((o) -> o.shelves),
		PresetCodecs.defaulted(SubmarineCanyons.CODEC, "submarineCanyons", SubmarineCanyons.makeDefault()).forGetter((o) -> o.submarineCanyons),
		PresetCodecs.defaulted(Trenches.CODEC, "trenches", Trenches.makeDefault()).forGetter((o) -> o.trenches),
		PresetCodecs.defaulted(Seamounts.CODEC, "seamounts", Seamounts.makeDefault()).forGetter((o) -> o.seamounts),
		PresetCodecs.defaulted(Ridges.CODEC, "ridges", Ridges.makeDefault()).forGetter((o) -> o.ridges),
		PresetCodecs.defaulted(BlueHoles.CODEC, "blueHoles", BlueHoles.makeDefault()).forGetter((o) -> o.blueHoles),
		PresetCodecs.defaulted(CoralReefs.CODEC, "coralReefs", CoralReefs.makeDefault()).forGetter((o) -> o.coralReefs),
		PresetCodecs.defaulted(SandWaves.CODEC, "sandWaves", SandWaves.makeDefault()).forGetter((o) -> o.sandWaves),
		PresetCodecs.defaulted(WhaleFalls.CODEC, "whaleFalls", WhaleFalls.makeDefault()).forGetter((o) -> o.whaleFalls),
		PresetCodecs.defaulted(Codec.BOOL, "sediment", true).forGetter((o) -> o.sediment)
	).apply(instance, OceanSettings::new));

	public Shelves shelves;
	public SubmarineCanyons submarineCanyons;
	public Trenches trenches;
	public Seamounts seamounts;
	public Ridges ridges;
	public BlueHoles blueHoles;
	public CoralReefs coralReefs;
	public SandWaves sandWaves;
	public WhaleFalls whaleFalls;
	// sand, gravel and clay on the sea floor rather than bare rock, the rock showing only on steep slopes
	public boolean sediment;

	public OceanSettings(Shelves shelves, SubmarineCanyons submarineCanyons, Trenches trenches, Seamounts seamounts, Ridges ridges, BlueHoles blueHoles, CoralReefs coralReefs, SandWaves sandWaves, WhaleFalls whaleFalls, boolean sediment) {
		this.shelves = shelves;
		this.submarineCanyons = submarineCanyons;
		this.trenches = trenches;
		this.seamounts = seamounts;
		this.ridges = ridges;
		this.blueHoles = blueHoles;
		this.coralReefs = coralReefs;
		this.sandWaves = sandWaves;
		this.whaleFalls = whaleFalls;
		this.sediment = sediment;
	}

	public OceanSettings copy() {
		return new OceanSettings(this.shelves.copy(), this.submarineCanyons.copy(), this.trenches.copy(), this.seamounts.copy(), this.ridges.copy(), this.blueHoles.copy(), this.coralReefs.copy(), this.sandWaves.copy(), this.whaleFalls.copy(), this.sediment);
	}

	public static OceanSettings makeDefault() {
		return new OceanSettings(Shelves.makeDefault(), SubmarineCanyons.makeDefault(), Trenches.makeDefault(), Seamounts.makeDefault(), Ridges.makeDefault(), BlueHoles.makeDefault(), CoralReefs.makeDefault(), SandWaves.makeDefault(), WhaleFalls.makeDefault(), true);
	}

	// how the original TerraForged generated: none of them
	public static OceanSettings makeNone() {
		OceanSettings settings = makeDefault();
		settings.shelves.enabled = false;
		settings.submarineCanyons.enabled = false;
		settings.trenches.enabled = false;
		settings.seamounts.enabled = false;
		settings.ridges.enabled = false;
		settings.blueHoles.enabled = false;
		settings.coralReefs.enabled = false;
		settings.sandWaves.enabled = false;
		settings.whaleFalls.enabled = false;
		settings.sediment = false;
		return settings;
	}

	/**
	 * A broad shallow shelf off the coast, ending in a drop-off into the deep.
	 */
	public static class Shelves {
		private static final Shelves DEFAULT = new Shelves(true, 1.0F);

		public static final Codec<Shelves> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "width", DEFAULT.width).forGetter((o) -> o.width)
		).apply(instance, Shelves::new));

		public boolean enabled;
		// scales how far the shelf reaches out
		public float width;

		public Shelves(boolean enabled, float width) {
			this.enabled = enabled;
			this.width = width;
		}

		public Shelves copy() {
			return new Shelves(this.enabled, this.width);
		}

		public static Shelves makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Deep winding canyons cut across the shelf and down the drop-off.
	 */
	public static class SubmarineCanyons {
		private static final SubmarineCanyons DEFAULT = new SubmarineCanyons(true, 0.5F);

		public static final Codec<SubmarineCanyons> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, SubmarineCanyons::new));

		public boolean enabled;
		// share of the suitable places that get one
		public float frequency;

		public SubmarineCanyons(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public SubmarineCanyons copy() {
			return new SubmarineCanyons(this.enabled, this.frequency);
		}

		public static SubmarineCanyons makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Long narrow chasms plunging below the deep ocean floor.
	 */
	public static class Trenches {
		private static final Trenches DEFAULT = new Trenches(true, 0.4F, 45);

		public static final Codec<Trenches> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency),
			PresetCodecs.defaulted(Codec.INT, "depth", DEFAULT.depth).forGetter((o) -> o.depth)
		).apply(instance, Trenches::new));

		public boolean enabled;
		// share of the suitable places that get one
		public float frequency;
		// how many blocks they plunge below the floor
		public int depth;

		public Trenches(boolean enabled, float frequency, int depth) {
			this.enabled = enabled;
			this.frequency = frequency;
			this.depth = depth;
		}

		public Trenches copy() {
			return new Trenches(this.enabled, this.frequency, this.depth);
		}

		public static Trenches makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Underwater mountains, some with flat tops.
	 */
	public static class Seamounts {
		private static final Seamounts DEFAULT = new Seamounts(true, 0.5F);

		public static final Codec<Seamounts> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, Seamounts::new));

		public boolean enabled;
		// share of the suitable places that get one
		public float frequency;

		public Seamounts(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public Seamounts copy() {
			return new Seamounts(this.enabled, this.frequency);
		}

		public static Seamounts makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Long ridges across the deep ocean, split by a rift with hydrothermal vents.
	 */
	public static class Ridges {
		private static final Ridges DEFAULT = new Ridges(true, 0.5F);

		public static final Codec<Ridges> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, Ridges::new));

		public boolean enabled;
		// how much of the deep ocean they cross
		public float frequency;

		public Ridges(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public Ridges copy() {
			return new Ridges(this.enabled, this.frequency);
		}

		public static Ridges makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Deep round sheer-sided pits in warm shallow seas.
	 */
	public static class BlueHoles {
		private static final BlueHoles DEFAULT = new BlueHoles(true, 0.4F);

		public static final Codec<BlueHoles> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, BlueHoles::new));

		public boolean enabled;
		// share of the suitable places that get one
		public float frequency;

		public BlueHoles(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public BlueHoles copy() {
			return new BlueHoles(this.enabled, this.frequency);
		}

		public static BlueHoles makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Reefs of coral growing almost to the surface off warm coasts.
	 */
	public static class CoralReefs {
		private static final CoralReefs DEFAULT = new CoralReefs(true, 0.5F);

		public static final Codec<CoralReefs> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "coverage", DEFAULT.coverage).forGetter((o) -> o.coverage)
		).apply(instance, CoralReefs::new));

		public boolean enabled;
		// share of the warm coasts with reefs
		public float coverage;

		public CoralReefs(boolean enabled, float coverage) {
			this.enabled = enabled;
			this.coverage = coverage;
		}

		public CoralReefs copy() {
			return new CoralReefs(this.enabled, this.coverage);
		}

		public static CoralReefs makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * Rippled ridges of sand on the shallow sea floor.
	 */
	public static class SandWaves {
		private static final SandWaves DEFAULT = new SandWaves(true, 2);

		public static final Codec<SandWaves> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.INT, "height", DEFAULT.height).forGetter((o) -> o.height)
		).apply(instance, SandWaves::new));

		public boolean enabled;
		// how many blocks they rise
		public int height;

		public SandWaves(boolean enabled, int height) {
			this.enabled = enabled;
			this.height = height;
		}

		public SandWaves copy() {
			return new SandWaves(this.enabled, this.height);
		}

		public static SandWaves makeDefault() {
			return DEFAULT.copy();
		}
	}

	/**
	 * The bones of whales lying on the deep sea floor.
	 */
	public static class WhaleFalls {
		private static final WhaleFalls DEFAULT = new WhaleFalls(true, 0.3F);

		public static final Codec<WhaleFalls> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PresetCodecs.defaulted(Codec.BOOL, "enabled", DEFAULT.enabled).forGetter((o) -> o.enabled),
			PresetCodecs.defaulted(Codec.FLOAT, "frequency", DEFAULT.frequency).forGetter((o) -> o.frequency)
		).apply(instance, WhaleFalls::new));

		public boolean enabled;
		// how often they lie on the floor
		public float frequency;

		public WhaleFalls(boolean enabled, float frequency) {
			this.enabled = enabled;
			this.frequency = frequency;
		}

		public WhaleFalls copy() {
			return new WhaleFalls(this.enabled, this.frequency);
		}

		public static WhaleFalls makeDefault() {
			return DEFAULT.copy();
		}
	}
}
