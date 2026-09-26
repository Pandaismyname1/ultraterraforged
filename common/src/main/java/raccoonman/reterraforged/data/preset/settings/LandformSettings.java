package raccoonman.reterraforged.data.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Distinct landforms added on top of the terrain types, like buttes in badlands. Every field is optional in the file,
 * so presets saved before a landform existed get it with its default settings.
 */
public class LandformSettings {
	public static final Codec<LandformSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		PresetCodecs.defaulted(Buttes.CODEC, "buttes", Buttes.makeDefault()).forGetter((o) -> o.buttes)
	).apply(instance, LandformSettings::new));

	public Buttes buttes;

	public LandformSettings(Buttes buttes) {
		this.buttes = buttes;
	}

	public LandformSettings copy() {
		return new LandformSettings(this.buttes.copy());
	}

	public static LandformSettings makeDefault() {
		return new LandformSettings(Buttes.makeDefault());
	}

	// how the original TerraForged generated: none of the added landforms
	public static LandformSettings makeNone() {
		LandformSettings settings = makeDefault();
		settings.buttes.enabled = false;
		return settings;
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
