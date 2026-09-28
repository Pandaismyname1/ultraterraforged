package com.pandaismyname1.ultraterraforged.data.preset;

import java.util.List;

import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.OverworldFunctionSet;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import com.pandaismyname1.ultraterraforged.data.preset.settings.CaveFeatureSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.CaveSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellField;
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.UTFDensityFunctions;

/*
 * The preset's density functions, registered over vanilla's overworld ones, and the router built from them.
 *
 * Since 26.3 the router only holds what's sampled by name (climate, the surface level, the final density); aquifers and
 * ore veins moved to the noise settings and the material rule, and the final density includes the beardifier and the
 * interpolation cell sizes itself. Vanilla's own overworld functions that aren't replaced here (factor, sloped cheese,
 * the cave pieces) read UltraTerraForged's continents, erosion, ridges, depth and jaggedness through their names.
 */
public class PresetNoiseRouterData {
	private static final float SCALER = 128.0F;
	private static final float UNIT = 1.0F / SCALER;
	// the noise cells the final density is interpolated over, as vanilla's overworld
	private static final int CELL_WIDTH = 4;
	private static final int CELL_HEIGHT = 8;
	// ground with a gradient below this counts as flat, and above that as steep
	private static final float FLAT_GROUND = 0.25F;
	private static final float STEEP_GROUND = 0.45F;
	// how much the entrance noise is raised on flat ground, and lowered on steep ground, at full strength
	private static final float FLAT_BIAS = 0.35F;
	private static final float STEEP_BIAS = 0.12F;
	// how far under the surface cave entrances are kept to slopes
	private static final float MOUTH_DEPTH = 24.0F;

	private static final OverworldFunctionSet<ResourceKey<DensityFunction>> OVERWORLD = NoiseRouterData.OVERWORLD_FUNCTIONS;
	// vanilla's overworld cave functions, private to NoiseRouterData
	private static final ResourceKey<DensityFunction> Y = vanillaKey("y");
	private static final ResourceKey<DensityFunction> BASE_3D_NOISE_OVERWORLD = vanillaKey("overworld/base_3d_noise");
	private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = vanillaKey("overworld/caves/spaghetti_roughness_function");
	private static final ResourceKey<DensityFunction> ENTRANCES = vanillaKey("overworld/caves/entrances");
	private static final ResourceKey<DensityFunction> NOODLE = vanillaKey("overworld/caves/noodle");
	private static final ResourceKey<DensityFunction> PILLARS = vanillaKey("overworld/caves/pillars");
	private static final ResourceKey<DensityFunction> SPAGHETTI_2D_THICKNESS_MODULATOR = vanillaKey("overworld/caves/spaghetti_2d_thickness_modulator");
	private static final ResourceKey<DensityFunction> SPAGHETTI_2D = vanillaKey("overworld/caves/spaghetti_2d");

    public static void bootstrap(Preset preset, BootstrapContext<DensityFunction> ctx) {
        HolderGetter<DensityFunction> densityFunctions = ctx.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise> noiseParams = ctx.lookup(Registries.NOISE);

    	WorldSettings worldSettings = preset.world();
    	WorldSettings.Properties properties = worldSettings.properties;

        int worldHeight = properties.worldHeight;
        int worldDepth = properties.worldDepth;

        ctx.register(OVERWORLD.temperature(), UTFDensityFunctions.cell(CellField.TEMPERATURE));
        ctx.register(OVERWORLD.vegetation(), UTFDensityFunctions.cell(CellField.MOISTURE));
        ctx.register(OVERWORLD.continents(), UTFDensityFunctions.cell(CellField.CONTINENTALNESS));
        ctx.register(OVERWORLD.erosion(), UTFDensityFunctions.cell(CellField.EROSION));
        ctx.register(NoiseRouterData.RIDGES, UTFDensityFunctions.cell(CellField.WEIRDNESS));

        DensityFunction offset = registerAndWrap(ctx, OVERWORLD.offset(), DensityFunctions.add(DensityFunctions.constant(NoiseRouterData.GLOBAL_OFFSET - 0.5F), DensityFunctions.mul(UTFDensityFunctions.clampToNearestUnit(UTFDensityFunctions.cell(CellField.HEIGHT), properties.terrainScaler()), DensityFunctions.constant(2.0F))));
        DensityFunction depth = registerAndWrap(ctx, OVERWORLD.depth(), DensityFunctions.add(DensityFunctions.yClampedGradient(-worldDepth, worldHeight, yGradientRange(-worldDepth), yGradientRange(worldHeight)), offset));
        ctx.register(BASE_3D_NOISE_OVERWORLD, DensityFunctions.zero());
        ctx.register(OVERWORLD.jaggedness(), DensityFunctions.zero());
        CaveSettings caves = preset.caves();
        ctx.register(NOODLE, noodle(-worldDepth, worldHeight, 1.0F - caves.noodleCaveProbability, densityFunctions, noiseParams));
        DensityFunction entrances = probabilityDensity(caves.entranceCaveProbability, entrances(densityFunctions, noiseParams));
        CaveFeatureSettings.CaveMouths mouths = preset.caveFeatures().caveMouths;
        if (mouths.enabled && mouths.strength > 0.0F) {
        	// cave entrances kept off flat ground and gathered on slopes, cliffs and gorge walls: the steeper the ground,
        	// the lower the entrance noise, which opens where it's below zero
        	DensityFunction gradient = UTFDensityFunctions.cell(CellField.GRADIENT);
        	DensityFunction flat = DensityFunctions.add(DensityFunctions.constant(FLAT_GROUND), DensityFunctions.mul(DensityFunctions.constant(-1.0F), gradient)).clamp(0.0F, FLAT_GROUND);
        	DensityFunction steep = DensityFunctions.add(gradient, DensityFunctions.constant(-STEEP_GROUND)).clamp(0.0F, 1.0F - STEEP_GROUND);
        	DensityFunction bias = DensityFunctions.add(DensityFunctions.mul(DensityFunctions.constant(mouths.strength * FLAT_BIAS / FLAT_GROUND), flat), DensityFunctions.mul(DensityFunctions.constant(-mouths.strength * STEEP_BIAS / (1.0F - STEEP_GROUND)), steep));
        	// only near the surface, where caves open onto it: 1 at the surface, 0 from MOUTH_DEPTH blocks down
        	DensityFunction surface = DensityFunctions.mul(UTFDensityFunctions.cell(CellField.HEIGHT), DensityFunctions.constant(properties.terrainScaler()));
        	DensityFunction below = DensityFunctions.add(DensityFunctions.yClampedGradient(-worldDepth, worldHeight, -worldDepth, worldHeight), DensityFunctions.mul(DensityFunctions.constant(-1.0F), surface));
        	DensityFunction near = DensityFunctions.add(DensityFunctions.mul(below, DensityFunctions.constant(1.0F / MOUTH_DEPTH)), DensityFunctions.constant(1.0F)).clamp(0.0F, 1.0F);
        	entrances = DensityFunctions.add(entrances, DensityFunctions.mul(bias, near));
        }
        ctx.register(ENTRANCES, entrances);
        ctx.register(SPAGHETTI_2D, probabilityDensity(caves.spaghettiCaveProbability, spaghetti2D(-worldDepth, worldHeight, densityFunctions, noiseParams)));

        // the overworld's surface level and final density, over vanilla's: mods that read them by name get the preset's
        DensityFunction factor = getFunction(densityFunctions, OVERWORLD.factor());
        DensityFunction initialDensity = noiseGradientDensity(DensityFunctions.cache(factor), depth);
        // Since 1.21.9 the router gives the surface's height here, not a density. It's found as NoiseChunk found it before:
        // from the top down, one noise cell (8 blocks) at a time, the first height where the density passes NOISE_ZERO.
        DensityFunction initialDensityWithoutJaggedness = slideOverworld(DensityFunctions.add(initialDensity, DensityFunctions.constant(UNIT * -90)).clamp(-64.0F, 64.0F), -worldDepth);
        DensityFunction preliminarySurfaceLevel = registerAndWrap(ctx, OVERWORLD.preliminarySurfaceLevel(), DensityFunctions.findTopSurface(DensityFunctions.add(initialDensityWithoutJaggedness, DensityFunctions.constant(-NoiseRouterData.NOISE_ZERO)), DensityFunctions.constant(properties.worldHeight), -worldDepth, CELL_HEIGHT));
        // as vanilla: interpolated between the chunk's corners
        ctx.register(OVERWORLD.chunkSurfaceLevel(), DensityFunctions.interpolated(preliminarySurfaceLevel, 16, 1));
        ctx.register(OVERWORLD.finalDensity(), finalDensity(caves, worldDepth, densityFunctions, noiseParams));
    }

    protected static NoiseRouter overworld(HolderGetter<DensityFunction> densityFunctions) {
    	OverworldFunctionSet<DensityFunction> functions = OVERWORLD.map((key) -> getFunction(densityFunctions, key));
    	return new NoiseRouter(functions.temperature(), functions.vegetation(), functions.continents(), functions.erosion(), functions.depth(), getFunction(densityFunctions, NoiseRouterData.RIDGES), functions.chunkSurfaceLevel(), functions.finalDensity());
	}

    // as vanilla's overworld aquifers, below the preset's preliminary surface
    protected static Aquifer.Config overworldAquifers(HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
    	DensityFunction barrier = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_BARRIER), 0.5);
        DensityFunction fluidLevelFloodedness = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67);
        DensityFunction fluidLevelSpread = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143);
        DensityFunction lava = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_LAVA));
        DensityFunction exclusion = OverworldBiomeBuilder.deepDarkRegion(getFunction(densityFunctions, OVERWORLD.erosion()), getFunction(densityFunctions, OVERWORLD.depth()));
        return new Aquifer.Config(barrier, fluidLevelFloodedness, fluidLevelSpread, lava, exclusion, getFunction(densityFunctions, OVERWORLD.preliminarySurfaceLevel()));
    }

    private static DensityFunction finalDensity(CaveSettings caves, int worldDepth, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
        DensityFunction slopedCheese = getFunction(densityFunctions, OVERWORLD.slopedCheese());
        DensityFunction entrances = caves.entranceCaveProbability > 0.0F ? DensityFunctions.min(slopedCheese, DensityFunctions.mul(DensityFunctions.constant(5.0F), DensityFunctions.interpolated(getFunction(densityFunctions, ENTRANCES), CELL_WIDTH, CELL_HEIGHT))) : slopedCheese;
        DensityFunction slopedCheeseRange = DensityFunctions.mul(DensityFunctions.rangeChoice(slopedCheese, -1000000.0F, caves.cheeseCaveDepthOffset, entrances, DensityFunctions.interpolated(slideOverworld(underground(caves.cheeseCaveProbability, densityFunctions, noiseParams, slopedCheese), -worldDepth), CELL_WIDTH, CELL_HEIGHT)), DensityFunctions.constant(0.64F)).squeeze();
        // structures' terrain adaption, which NoiseChunk added itself before 26.3
        return DensityFunctions.add(DensityFunctions.min(slopedCheeseRange, getFunction(densityFunctions, NOODLE)), DensityFunctions.beardifier());
    }

    private static DensityFunction underground(float cheeseCaveProbability, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams, DensityFunction slopedCheese) {
        DensityFunction spaghetti2d = getFunction(densityFunctions, SPAGHETTI_2D);
        DensityFunction spaghettiRoughnessFunction = getFunction(densityFunctions, SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction caveLayerNoise = DensityFunctions.noise(noiseParams.getOrThrow(Noises.CAVE_LAYER), 8.0);
        DensityFunction caveLayer = DensityFunctions.mul(DensityFunctions.constant(4.0F), caveLayerNoise.square());
        DensityFunction caveCheese = probabilityDensity(cheeseCaveProbability, DensityFunctions.noise(noiseParams.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666));
        DensityFunction slopedCaves = DensityFunctions.add(DensityFunctions.add(DensityFunctions.constant(0.27F), caveCheese).clamp(-1.0F, 1.0F), DensityFunctions.add(DensityFunctions.constant(1.5F), DensityFunctions.mul(DensityFunctions.constant(-0.64F), slopedCheese)).clamp(0.0F, 0.5F));
        DensityFunction slopedCaveLayered = DensityFunctions.add(caveLayer, slopedCaves);
        DensityFunction underground = DensityFunctions.min(DensityFunctions.min(slopedCaveLayered, getFunction(densityFunctions, ENTRANCES)), DensityFunctions.add(spaghetti2d, spaghettiRoughnessFunction));
        DensityFunction pillars = getFunction(densityFunctions, PILLARS);
        DensityFunction pillarRange = DensityFunctions.rangeChoice(pillars, -1000000.0F, 0.03F, DensityFunctions.constant(-1000000.0F), pillars);
        return DensityFunctions.max(underground, pillarRange);
    }

    // vanilla's overworld cave entrances (NoiseRouterData.entrances)
    private static DensityFunction entrances(HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
        DensityFunction rarityModulator = DensityFunctions.cache(DensityFunctions.noise(noiseParams.getOrThrow(Noises.SPAGHETTI_3D_RARITY), 2.0, 1.0));
        DensityFunction thicknessModulator = DensityFunctions.mappedNoise(noiseParams.getOrThrow(Noises.SPAGHETTI_3D_THICKNESS), -0.065F, -0.088F);
        DensityFunction cave1 = wrapRarity3d(rarityModulator, noiseParams.getOrThrow(Noises.SPAGHETTI_3D_1));
        DensityFunction cave2 = wrapRarity3d(rarityModulator, noiseParams.getOrThrow(Noises.SPAGHETTI_3D_2));
        DensityFunction spaghetti3d = DensityFunctions.add(DensityFunctions.max(cave1, cave2), thicknessModulator).clamp(-1.0F, 1.0F);
        DensityFunction spaghettiRoughness = getFunction(densityFunctions, SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction bigEntranceNoise = DensityFunctions.noise(noiseParams.getOrThrow(Noises.CAVE_ENTRANCE), 0.75, 0.5);
        DensityFunction bigEntrances = DensityFunctions.add(bigEntranceNoise.add(0.37F), DensityFunctions.yClampedGradient(-10, 30, 0.3F, 0.0F));
        return DensityFunctions.cache(DensityFunctions.min(bigEntrances, DensityFunctions.add(spaghettiRoughness, spaghetti3d)));
    }

    private static DensityFunction spaghetti2D(int minY, int maxY, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
        DensityFunction modulator = DensityFunctions.noise(noiseParams.getOrThrow(Noises.SPAGHETTI_2D_MODULATOR), 2.0, 1.0);
        DensityFunction sampler = wrapRarity2d(modulator, noiseParams.getOrThrow(Noises.SPAGHETTI_2D));
        DensityFunction elevation = DensityFunctions.mappedNoise(noiseParams.getOrThrow(Noises.SPAGHETTI_2D_ELEVATION), 0.0, Math.floorDiv(minY, 8), 8.0F);
        DensityFunction thicknessModulator = getFunction(densityFunctions, SPAGHETTI_2D_THICKNESS_MODULATOR);
        DensityFunction elevationGradient = DensityFunctions.add(elevation, DensityFunctions.yClampedGradient(minY, maxY, minY / -8.0F, maxY / -8.0F)).abs();
        DensityFunction normal = DensityFunctions.add(elevationGradient, thicknessModulator).cube();
        DensityFunction weird = DensityFunctions.add(sampler, DensityFunctions.mul(DensityFunctions.constant(0.083F), thicknessModulator));
        return DensityFunctions.max(weird, normal).clamp(-1.0F, 1.0F);
    }

    private static DensityFunction noodle(int minY, int maxY, float threshold, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
    	int baseY = minY + 4;

    	DensityFunction y = getFunction(densityFunctions, Y);
        DensityFunction selector = yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE), 1.0, 1.0), baseY, maxY, -1);
        DensityFunction thickness = yLimitedInterpolatable(y, DensityFunctions.mappedNoise(noiseParams.getOrThrow(Noises.NOODLE_THICKNESS), 1.0, 1.0, -0.05F, -0.1F), baseY, maxY, 0);
        DensityFunction ridgeA = yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE_RIDGE_A), 2.6666666666666665, 2.6666666666666665), baseY, maxY, 0);
        DensityFunction ridgeB = yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE_RIDGE_B), 2.6666666666666665, 2.6666666666666665), baseY, maxY, 0);
        DensityFunction ridge = DensityFunctions.mul(DensityFunctions.constant(1.5F), DensityFunctions.max(ridgeA.abs(), ridgeB.abs()));
        return DensityFunctions.rangeChoice(selector, -1000000.0F, threshold, DensityFunctions.constant(64.0F), DensityFunctions.add(thickness, ridge));
    }

    // shifts a cave noise so that fewer of its values open up air; 1 leaves it unchanged and 0 removes the cave type
    private static DensityFunction probabilityDensity(float probability, DensityFunction function) {
    	if (probability >= 1.0F) {
    		return function;
    	}
    	if (probability <= 0.0F) {
    		return DensityFunctions.constant(1.0F);
    	}
    	return DensityFunctions.add(DensityFunctions.constant(1.0F - probability), function);
    }

    private static DensityFunction slideOverworld(DensityFunction function, int minY) {
        return slide(function, minY, 0, 24, UNIT * 15);
    }

    private static DensityFunction slide(DensityFunction function, int minY, int bottomGradientStart, int bottomGradientEnd, float bottomGradientTarget) {
        DensityFunction bottomGradient = DensityFunctions.yClampedGradient(minY + bottomGradientStart, minY + bottomGradientEnd, 0.0F, 1.0F);
        return DensityFunctions.lerp(bottomGradient, bottomGradientTarget, function);
    }

    // the vanilla helpers below are private to NoiseRouterData since 26.3

    private static DensityFunction noiseGradientDensity(DensityFunction factor, DensityFunction depthWithJaggedness) {
    	return DensityFunctions.mul(depthWithJaggedness, factor).quarterNegative().mul(4.0F);
    }

    private static DensityFunction yLimitedInterpolatable(DensityFunction y, DensityFunction whenInRange, int minYInclusive, int maxYInclusive, int whenOutOfRange) {
    	return DensityFunctions.interpolated(DensityFunctions.rangeChoice(y, minYInclusive, maxYInclusive + 1, whenInRange, DensityFunctions.constant(whenOutOfRange)), CELL_WIDTH, CELL_HEIGHT);
    }

    // NoiseRouterData.QuantizedSpaghettiRarity: the spaghetti noise, stretched more where the modulator is higher
    private static DensityFunction wrapRarity2d(DensityFunction input, Holder<NormalNoise> noise) {
    	return DensityFunctions.intervalSelect(input, FloatList.of(-0.75F, -0.5F, 0.5F, 0.75F), List.of(
    		noiseForRarity(noise, 0.5F), noiseForRarity(noise, 0.75F), noiseForRarity(noise, 1.0F), noiseForRarity(noise, 2.0F), noiseForRarity(noise, 3.0F)
    	)).abs();
    }

    private static DensityFunction wrapRarity3d(DensityFunction input, Holder<NormalNoise> noise) {
    	return DensityFunctions.intervalSelect(input, FloatList.of(-0.5F, 0.0F, 0.5F), List.of(
    		noiseForRarity(noise, 0.75F), noiseForRarity(noise, 1.0F), noiseForRarity(noise, 1.5F), noiseForRarity(noise, 2.0F)
    	)).abs();
    }

    private static DensityFunction noiseForRarity(Holder<NormalNoise> noise, float rarity) {
    	return DensityFunctions.noise(noise, 1.0 / rarity, 1.0 / rarity).mul(rarity);
    }

    private static DensityFunction registerAndWrap(BootstrapContext<DensityFunction> ctx, ResourceKey<DensityFunction> key, DensityFunction value) {
    	return new DensityFunctions.HolderHolder(ctx.register(key, value));
    }

    private static DensityFunction getFunction(HolderGetter<DensityFunction> densityFunctions, ResourceKey<DensityFunction> key) {
    	return NoiseRouterData.getFunction(densityFunctions, key);
    }

    private static ResourceKey<DensityFunction> vanillaKey(String name) {
    	return ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.withDefaultNamespace(name));
    }

    private static float yGradientRange(float range) {
    	return 1.0F + (-range / SCALER);
    }
}
