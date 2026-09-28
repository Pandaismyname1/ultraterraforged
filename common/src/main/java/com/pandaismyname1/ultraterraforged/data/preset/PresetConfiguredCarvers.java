package com.pandaismyname1.ultraterraforged.data.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.TrapezoidFloat;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.VeryBiasedToBottomInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.CanyonWorldCarver;
import net.minecraft.world.level.levelgen.carver.CaveWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import com.pandaismyname1.ultraterraforged.data.preset.settings.CaveSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.floatproviders.LegacyCanyonYScale;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightproviders.LegacyCarverHeight;

// since 26.3 configured carvers are carvers: the configuration is the carver record itself.
// The cave count, thickness and start vertical radius used to be hardcoded in CaveWorldCarver;
// they are fields now, set here to vanilla 26.3's overworld values (which match the old hardcoded ones).
// The lava level, debug settings and replaceable-blocks tag are gone (the material system handles them).
public class PresetConfiguredCarvers {

	public static void bootstrap(Preset preset, BootstrapContext<WorldCarver> ctx) {
		CaveSettings caveSettings = preset.caves();

        ctx.register(Carvers.CAVE, cave(caveSettings.caveCarverProbability, modifiedCaveY(caveSettings), modifiedCaveYScale(caveSettings), modifiedCaveHorizontalRadiusMultiplier(caveSettings), modifiedCaveVerticalRadiusMultiplier(caveSettings), modifiedCaveFloorLevel(caveSettings)));
        ctx.register(Carvers.CAVE_EXTRA_UNDERGROUND, cave(modifiedDeepCaveProbability(caveSettings), UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(47)), UniformFloat.of(0.1F, 0.9F), UniformFloat.of(0.7F, 1.4F), UniformFloat.of(0.8F, 1.3F), UniformFloat.of(-1.0F, -0.4F)));
        ctx.register(Carvers.CANYON, new CanyonWorldCarver(caveSettings.ravineCarverProbability, modifiedRavineY(caveSettings), modifiedRavineVerticalRotation(caveSettings), new CanyonWorldCarver.Shape(UniformFloat.of(0.75F, 1.0F), TrapezoidFloat.of(0.0F, 6.0F, 2.0F), 3, UniformFloat.of(0.75F, 1.0F), 1.0F, 0.0F, ConstantFloat.of(3.0F))));
	}

	private static CaveWorldCarver cave(float probability, HeightProvider y, FloatProvider roomVerticalRadiusMultiplier, FloatProvider horizontalRadiusMultiplier, FloatProvider verticalRadiusMultiplier, FloatProvider floorLevel) {
		return new CaveWorldCarver(probability, y, VeryBiasedToBottomInt.of(0, 14), TrapezoidFloat.of(0.0F, 3.0F, 1.0F), true, roomVerticalRadiusMultiplier, horizontalRadiusMultiplier, verticalRadiusMultiplier, ConstantFloat.of(1.0F), floorLevel);
	}

	private static HeightProvider modifiedCaveY(CaveSettings caveSettings) {
        return caveSettings.legacyCarverDistribution ? LegacyCarverHeight.of(0, 8, 120) : UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(180));
	}

	private static FloatProvider modifiedCaveYScale(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? ConstantFloat.of(0.5F) : UniformFloat.of(0.1F, 0.9F);
	}

	private static FloatProvider modifiedCaveFloorLevel(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? ConstantFloat.of(-0.7F) : UniformFloat.of(-1.0F, -0.4F);
	}

	private static FloatProvider modifiedCaveHorizontalRadiusMultiplier(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? ConstantFloat.of(1.0F) : UniformFloat.of(0.7F, 1.4F);
	}

	private static FloatProvider modifiedCaveVerticalRadiusMultiplier(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? ConstantFloat.of(1.0F) : UniformFloat.of(0.8F, 1.3F);
	}

	private static float modifiedDeepCaveProbability(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? 0.0F : caveSettings.deepCaveCarverProbability;
	}

	private static HeightProvider modifiedRavineY(CaveSettings caveSettings) {
        return caveSettings.legacyCarverDistribution ? LegacyCarverHeight.of(20, 8, 40) : UniformHeight.of(VerticalAnchor.absolute(10), VerticalAnchor.absolute(67));
	}

	private static FloatProvider modifiedRavineVerticalRotation(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? (FloatProvider) new LegacyCanyonYScale() : UniformFloat.of(-0.125F, 0.125F);
	}
}
