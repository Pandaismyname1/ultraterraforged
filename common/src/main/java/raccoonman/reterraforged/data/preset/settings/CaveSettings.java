package raccoonman.reterraforged.data.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// Keys match ReTerraForged 0.0.6, so presets saved by it keep their cave settings
public class CaveSettings {
	private static final CaveSettings VANILLA = makeVanilla();

	public static final Codec<CaveSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		PresetCodecs.defaulted(Codec.FLOAT, "entranceCaveProbability", VANILLA.entranceCaveProbability).forGetter((o) -> o.entranceCaveProbability),
		PresetCodecs.defaulted(Codec.FLOAT, "cheeseCaveDepthOffset", VANILLA.cheeseCaveDepthOffset).forGetter((o) -> o.cheeseCaveDepthOffset),
		PresetCodecs.defaulted(Codec.FLOAT, "cheeseCaveProbability", VANILLA.cheeseCaveProbability).forGetter((o) -> o.cheeseCaveProbability),
		PresetCodecs.defaulted(Codec.FLOAT, "spaghettiCaveProbability", VANILLA.spaghettiCaveProbability).forGetter((o) -> o.spaghettiCaveProbability),
		PresetCodecs.defaulted(Codec.FLOAT, "noodleCaveProbability", VANILLA.noodleCaveProbability).forGetter((o) -> o.noodleCaveProbability),
		PresetCodecs.defaulted(Codec.FLOAT, "caveCarverProbability", VANILLA.caveCarverProbability).forGetter((o) -> o.caveCarverProbability),
		PresetCodecs.defaulted(Codec.FLOAT, "deepCaveCarverProbability", VANILLA.deepCaveCarverProbability).forGetter((o) -> o.deepCaveCarverProbability),
		PresetCodecs.defaulted(Codec.FLOAT, "ravineCarverProbability", VANILLA.ravineCarverProbability).forGetter((o) -> o.ravineCarverProbability),
		PresetCodecs.defaulted(Codec.BOOL, "largeOreVeins", VANILLA.largeOreVeins).forGetter((o) -> o.largeOreVeins),
		PresetCodecs.defaulted(Codec.BOOL, "legacyCarverDistribution", VANILLA.legacyCarverDistribution).forGetter((o) -> o.legacyCarverDistribution)
	).apply(instance, CaveSettings::new));

	public float entranceCaveProbability;
	public float cheeseCaveDepthOffset;
	public float cheeseCaveProbability;
	public float spaghettiCaveProbability;
	public float noodleCaveProbability;
	public float caveCarverProbability;
	public float deepCaveCarverProbability;
	public float ravineCarverProbability;
	public boolean largeOreVeins;
	public boolean legacyCarverDistribution;

	public CaveSettings(float entranceCaveProbability, float cheeseCaveDepthOffset, float cheeseCaveProbability, float spaghettiCaveProbability, float noodleCaveProbability, float caveCarverProbability, float deepCaveCarverProbability, float ravineCarverProbability, boolean largeOreVeins, boolean legacyCarverDistribution) {
		this.entranceCaveProbability = entranceCaveProbability;
		this.cheeseCaveDepthOffset = cheeseCaveDepthOffset;
		this.cheeseCaveProbability = cheeseCaveProbability;
		this.spaghettiCaveProbability = spaghettiCaveProbability;
		this.noodleCaveProbability = noodleCaveProbability;
		this.caveCarverProbability = caveCarverProbability;
		this.deepCaveCarverProbability = deepCaveCarverProbability;
		this.ravineCarverProbability = ravineCarverProbability;
		this.largeOreVeins = largeOreVeins;
		this.legacyCarverDistribution = legacyCarverDistribution;
	}

	public CaveSettings copy() {
		return new CaveSettings(this.entranceCaveProbability, this.cheeseCaveDepthOffset, this.cheeseCaveProbability, this.spaghettiCaveProbability, this.noodleCaveProbability, this.caveCarverProbability, this.deepCaveCarverProbability, this.ravineCarverProbability, this.largeOreVeins, this.legacyCarverDistribution);
	}

	/**
	 * Matches vanilla 1.20.1 caves; also what presets saved by 0.0.7, which ignored cave settings, get.
	 */
	public static CaveSettings makeVanilla() {
		return new CaveSettings(1.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.15F, 0.07F, 0.01F, true, false);
	}
}
