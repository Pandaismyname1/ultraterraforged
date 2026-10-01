package com.pandaismyname1.ultraterraforged.data.preset;

import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.data.preset.settings.CaveFeatureSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.CaveSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellField;
import com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.UTFDensityFunctions;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

public class PresetNoiseRouterData {
	private static final float SCALER = 128.0F;
	private static final float UNIT = 1.0F / SCALER;
	private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0D);
	// ground with a gradient below this counts as flat, and above that as steep
	private static final double FLAT_GROUND = 0.25D;
	private static final double STEEP_GROUND = 0.45D;
	// how much the entrance noise is raised on flat ground, and lowered on steep ground, at full strength
	private static final double FLAT_BIAS = 0.35D;
	private static final double STEEP_BIAS = 0.12D;
	// how far under the surface cave entrances are kept to slopes
	private static final double MOUTH_DEPTH = 24.0D;
	
	// The overworld's functions, under UltraTerraForged's names. Registered under vanilla's they replaced its functions
	// in every dimension built on them, as many modded dimensions are, and gave those UltraTerraForged's overworld.
	public static final ResourceKey<DensityFunction> CONTINENTS = createKey("overworld/continents");
	public static final ResourceKey<DensityFunction> EROSION = createKey("overworld/erosion");
	public static final ResourceKey<DensityFunction> RIDGES = createKey("overworld/ridges");
	public static final ResourceKey<DensityFunction> RIDGES_FOLDED = createKey("overworld/ridges_folded");
	public static final ResourceKey<DensityFunction> OFFSET = createKey("overworld/offset");
	public static final ResourceKey<DensityFunction> FACTOR = createKey("overworld/factor");
	public static final ResourceKey<DensityFunction> JAGGEDNESS = createKey("overworld/jaggedness");
	public static final ResourceKey<DensityFunction> DEPTH = createKey("overworld/depth");
	public static final ResourceKey<DensityFunction> SLOPED_CHEESE = createKey("overworld/sloped_cheese");
	public static final ResourceKey<DensityFunction> BASE_3D_NOISE = createKey("overworld/base_3d_noise");
	public static final ResourceKey<DensityFunction> NOODLE = createKey("overworld/caves/noodle");
	public static final ResourceKey<DensityFunction> ENTRANCES = createKey("overworld/caves/entrances");
	public static final ResourceKey<DensityFunction> SPAGHETTI_2D = createKey("overworld/caves/spaghetti_2d");
	
    public static void bootstrap(Preset preset, BootstapContext<DensityFunction> ctx) {
        HolderGetter<DensityFunction> densityFunctions = ctx.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise.NoiseParameters> noiseParams = ctx.lookup(Registries.NOISE);
        
    	WorldSettings worldSettings = preset.world();
    	WorldSettings.Properties properties = worldSettings.properties;
    	
        int worldHeight = properties.worldHeight;
        int worldDepth = properties.worldDepth;
        
        Holder<DensityFunction> continents = ctx.register(CONTINENTS, UTFDensityFunctions.cell(CellField.CONTINENTALNESS));
        Holder<DensityFunction> erosion = ctx.register(EROSION, UTFDensityFunctions.cell(CellField.EROSION));
        Holder<DensityFunction> ridges = ctx.register(RIDGES, UTFDensityFunctions.cell(CellField.WEIRDNESS));
        Holder<DensityFunction> ridgesFolded = ctx.register(RIDGES_FOLDED, peaksAndValleys(new DensityFunctions.HolderHolder(ridges)));
        
        DensityFunction offset = NoiseRouterData.registerAndWrap(ctx, OFFSET, DensityFunctions.add(DensityFunctions.constant(NoiseRouterData.GLOBAL_OFFSET - 0.5F), DensityFunctions.mul(UTFDensityFunctions.clampToNearestUnit(UTFDensityFunctions.cell(CellField.HEIGHT), properties.terrainScaler()), DensityFunctions.constant(2.0D))));
        DensityFunction depth = NoiseRouterData.registerAndWrap(ctx, DEPTH, DensityFunctions.add(DensityFunctions.yClampedGradient(-worldDepth, worldHeight, yGradientRange(-worldDepth), yGradientRange(worldHeight)), offset));
        DensityFunction base3dNoise = NoiseRouterData.registerAndWrap(ctx, BASE_3D_NOISE, DensityFunctions.zero());
        DensityFunction jaggedness = NoiseRouterData.registerAndWrap(ctx, JAGGEDNESS, jaggednessPerformanceHack());
        // vanilla's factor and sloped cheese, read from the functions above
        DensityFunctions.Spline.Coordinate continentsCoordinate = new DensityFunctions.Spline.Coordinate(continents);
        DensityFunctions.Spline.Coordinate erosionCoordinate = new DensityFunctions.Spline.Coordinate(erosion);
        DensityFunctions.Spline.Coordinate ridgesCoordinate = new DensityFunctions.Spline.Coordinate(ridges);
        DensityFunctions.Spline.Coordinate ridgesFoldedCoordinate = new DensityFunctions.Spline.Coordinate(ridgesFolded);
        DensityFunction factor = NoiseRouterData.registerAndWrap(ctx, FACTOR, splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldFactor(continentsCoordinate, erosionCoordinate, ridgesCoordinate, ridgesFoldedCoordinate, false)), BLENDING_FACTOR));
        DensityFunction jaggedNoise = DensityFunctions.noise(noiseParams.getOrThrow(Noises.JAGGED), 1500.0D, 0.0D);
        DensityFunction slopedCheese = NoiseRouterData.noiseGradientDensity(factor, DensityFunctions.add(depth, DensityFunctions.mul(jaggedness, jaggedNoise.halfNegative())));
        ctx.register(SLOPED_CHEESE, DensityFunctions.add(slopedCheese, base3dNoise));
        CaveSettings caves = preset.caves();
        ctx.register(NOODLE, noodle(-worldDepth, worldHeight, 1.0F - caves.noodleCaveProbability, densityFunctions, noiseParams));
        DensityFunction entrances = probabilityDensity(caves.entranceCaveProbability, NoiseRouterData.entrances(densityFunctions, noiseParams));
        CaveFeatureSettings.CaveMouths mouths = preset.caveFeatures().caveMouths;
        if (mouths.enabled && mouths.strength > 0.0F) {
        	// cave entrances kept off flat ground and gathered on slopes, cliffs and gorge walls: the steeper the ground,
        	// the lower the entrance noise, which opens where it's below zero
        	DensityFunction gradient = UTFDensityFunctions.cell(CellField.GRADIENT);
        	DensityFunction flat = DensityFunctions.add(DensityFunctions.constant(FLAT_GROUND), DensityFunctions.mul(DensityFunctions.constant(-1.0D), gradient)).clamp(0.0D, FLAT_GROUND);
        	DensityFunction steep = DensityFunctions.add(gradient, DensityFunctions.constant(-STEEP_GROUND)).clamp(0.0D, 1.0D - STEEP_GROUND);
        	DensityFunction bias = DensityFunctions.add(DensityFunctions.mul(DensityFunctions.constant(mouths.strength * FLAT_BIAS / FLAT_GROUND), flat), DensityFunctions.mul(DensityFunctions.constant(-mouths.strength * STEEP_BIAS / (1.0D - STEEP_GROUND)), steep));
        	// only near the surface, where caves open onto it: 1 at the surface, 0 from MOUTH_DEPTH blocks down
        	DensityFunction surface = DensityFunctions.mul(UTFDensityFunctions.cell(CellField.HEIGHT), DensityFunctions.constant(properties.terrainScaler()));
        	DensityFunction belowSurface = DensityFunctions.add(DensityFunctions.yClampedGradient(-worldDepth, worldHeight, -worldDepth, worldHeight), DensityFunctions.mul(DensityFunctions.constant(-1.0D), surface));
        	DensityFunction near = DensityFunctions.add(DensityFunctions.mul(belowSurface, DensityFunctions.constant(1.0D / MOUTH_DEPTH)), DensityFunctions.constant(1.0D)).clamp(0.0D, 1.0D);
        	entrances = DensityFunctions.add(entrances, DensityFunctions.mul(bias, near));
        }
        ctx.register(ENTRANCES, entrances);
        ctx.register(SPAGHETTI_2D, probabilityDensity(caves.spaghettiCaveProbability, spaghetti2D(-worldDepth, worldHeight, densityFunctions, noiseParams)));
    }

    protected static NoiseRouter overworld(Preset preset, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise.NoiseParameters> noiseParams, HolderGetter<Noise> noises) {
    	WorldSettings worldSettings = preset.world();
    	WorldSettings.Properties properties = worldSettings.properties;
    	int worldDepth = properties.worldDepth;
    	
    	DensityFunction aquiferBarrier = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_BARRIER), 0.5);
        DensityFunction aquiferFluidLevelFloodedness = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67);
        DensityFunction aquiferFluidLevelSpread = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143);
        DensityFunction aquiferLava = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_LAVA));
        DensityFunction temperature = UTFDensityFunctions.cell(CellField.TEMPERATURE);
        DensityFunction vegetation = UTFDensityFunctions.cell(CellField.MOISTURE);
        DensityFunction factor = NoiseRouterData.getFunction(densityFunctions, FACTOR);
        DensityFunction depth = NoiseRouterData.getFunction(densityFunctions, DEPTH);
        DensityFunction initialDensity = NoiseRouterData.noiseGradientDensity(DensityFunctions.cache2d(factor), depth);
        DensityFunction slopedCheese = NoiseRouterData.getFunction(densityFunctions, SLOPED_CHEESE);
        CaveSettings caves = preset.caves();
        DensityFunction entrances = caves.entranceCaveProbability > 0.0F ? DensityFunctions.min(slopedCheese, DensityFunctions.mul(DensityFunctions.constant(5.0D), DensityFunctions.interpolated(NoiseRouterData.getFunction(densityFunctions, ENTRANCES)))) : slopedCheese;
        DensityFunction slopedCheeseRange = DensityFunctions.mul(DensityFunctions.rangeChoice(slopedCheese, -1000000.0D, caves.cheeseCaveDepthOffset, entrances, DensityFunctions.interpolated(slideOverworld(underground(caves.cheeseCaveProbability, densityFunctions, noiseParams, slopedCheese), -worldDepth))), DensityFunctions.constant(0.64)).squeeze();
        DensityFunction finalDensity = DensityFunctions.min(slopedCheeseRange, NoiseRouterData.getFunction(densityFunctions, NOODLE));
        DensityFunction y = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.Y);
        int minY = Stream.of(OreVeinifier.VeinType.values()).mapToInt(veinType -> veinType.minY).min().orElse(-DimensionType.MIN_Y * 2);
        int maxY = Stream.of(OreVeinifier.VeinType.values()).mapToInt(veinType -> veinType.maxY).max().orElse(-DimensionType.MIN_Y * 2);
        DensityFunction oreVeininess = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.ORE_VEININESS), 1.5, 1.5), minY, maxY, 0);
        DensityFunction oreVeinA = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.ORE_VEIN_A), 4.0, 4.0), minY, maxY, 0).abs();
        DensityFunction oreVeinB = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.ORE_VEIN_B), 4.0, 4.0), minY, maxY, 0).abs();
        DensityFunction oreVein = DensityFunctions.add(DensityFunctions.constant(-0.08F), DensityFunctions.max(oreVeinA, oreVeinB));
        DensityFunction oreGap = DensityFunctions.noise(noiseParams.getOrThrow(Noises.ORE_GAP));
        return new NoiseRouter(aquiferBarrier, aquiferFluidLevelFloodedness, aquiferFluidLevelSpread, aquiferLava, temperature, vegetation, NoiseRouterData.getFunction(densityFunctions, CONTINENTS), NoiseRouterData.getFunction(densityFunctions, EROSION), depth, NoiseRouterData.getFunction(densityFunctions, RIDGES), slideOverworld(DensityFunctions.add(initialDensity, DensityFunctions.constant(UNIT * -90)).clamp(-64.0, 64.0), -worldDepth), finalDensity, oreVeininess, oreVein, oreGap);
	}

    private static DensityFunction underground(float cheeseCaveProbability, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise.NoiseParameters> noiseParams, DensityFunction slopedCheese) {
        DensityFunction spaghetti2d = NoiseRouterData.getFunction(densityFunctions, SPAGHETTI_2D);
        DensityFunction spaghettiRoughnessFunction = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction caveLayerNoise = DensityFunctions.noise(noiseParams.getOrThrow(Noises.CAVE_LAYER), 8.0);
        DensityFunction caveLayer = DensityFunctions.mul(DensityFunctions.constant(4.0), caveLayerNoise.square());
        DensityFunction caveCheese = probabilityDensity(cheeseCaveProbability, DensityFunctions.noise(noiseParams.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666));
        DensityFunction slopedCaves = DensityFunctions.add(DensityFunctions.add(DensityFunctions.constant(0.27), caveCheese).clamp(-1.0, 1.0), DensityFunctions.add(DensityFunctions.constant(1.5), DensityFunctions.mul(DensityFunctions.constant(-0.64), slopedCheese)).clamp(0.0, 0.5));
        DensityFunction slopedCaveLayered = DensityFunctions.add(caveLayer, slopedCaves);
        DensityFunction underground = DensityFunctions.min(DensityFunctions.min(slopedCaveLayered, NoiseRouterData.getFunction(densityFunctions, ENTRANCES)), DensityFunctions.add(spaghetti2d, spaghettiRoughnessFunction));
        DensityFunction pillars = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.PILLARS);
        DensityFunction pillarRange = DensityFunctions.rangeChoice(pillars, -1000000.0, 0.03, DensityFunctions.constant(-1000000.0), pillars);
        return DensityFunctions.max(underground, pillarRange);
    }

    private static DensityFunction spaghetti2D(int minY, int maxY, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise.NoiseParameters> noiseParams) {
        DensityFunction modulator = DensityFunctions.noise(noiseParams.getOrThrow(Noises.SPAGHETTI_2D_MODULATOR), 2.0, 1.0);
        DensityFunction sampler = DensityFunctions.weirdScaledSampler(modulator, noiseParams.getOrThrow(Noises.SPAGHETTI_2D), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE2);
        DensityFunction elevation = DensityFunctions.mappedNoise(noiseParams.getOrThrow(Noises.SPAGHETTI_2D_ELEVATION), 0.0, Math.floorDiv(minY, 8), 8.0);
        DensityFunction thicknessModulator = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.SPAGHETTI_2D_THICKNESS_MODULATOR);
        DensityFunction elevationGradient = DensityFunctions.add(elevation, DensityFunctions.yClampedGradient(minY, maxY, minY / -8.0D, maxY / -8.0D)).abs();
        DensityFunction normal = DensityFunctions.add(elevationGradient, thicknessModulator).cube();
        DensityFunction weird = DensityFunctions.add(sampler, DensityFunctions.mul(DensityFunctions.constant(0.083D), thicknessModulator));
        return DensityFunctions.max(weird, normal).clamp(-1.0D, 1.0D);
    }

    private static DensityFunction noodle(int minY, int maxY, float threshold, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise.NoiseParameters> noiseParams) {
    	int baseY = minY + 4;
        
    	DensityFunction y = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.Y);
        DensityFunction selector = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE), 1.0, 1.0), baseY, maxY, -1);
        DensityFunction thickness = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.mappedNoise(noiseParams.getOrThrow(Noises.NOODLE_THICKNESS), 1.0, 1.0, -0.05, -0.1), baseY, maxY, 0);
        DensityFunction ridgeA = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE_RIDGE_A), 2.6666666666666665, 2.6666666666666665), baseY, maxY, 0);
        DensityFunction ridgeB = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE_RIDGE_B), 2.6666666666666665, 2.6666666666666665), baseY, maxY, 0);
        DensityFunction ridge = DensityFunctions.mul(DensityFunctions.constant(1.5), DensityFunctions.max(ridgeA.abs(), ridgeB.abs()));
        return DensityFunctions.rangeChoice(selector, -1000000.0, threshold, DensityFunctions.constant(64.0), DensityFunctions.add(thickness, ridge));
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
    
    private static DensityFunction slide(DensityFunction function, int minY, int bottomGradientStart, int bottomGradientEnd, double bottomGradientTarget) {
        DensityFunction bottomGradient = DensityFunctions.yClampedGradient(minY + bottomGradientStart, minY + bottomGradientEnd, 0.0, 1.0);
        return DensityFunctions.lerp(bottomGradient, bottomGradientTarget, function);
    }
    
    /* 
     * the multiply function doesnt sample the second input
     * if the first input is zero, however this optimization doesn't get
     * applied if either input is a Constant, so we cant just use DensityFunctions.zero()
     */
    private static DensityFunction jaggednessPerformanceHack() {
    	return DensityFunctions.add(DensityFunctions.zero(), DensityFunctions.zero());
    }
    
    private static DensityFunction peaksAndValleys(DensityFunction ridges) {
        return DensityFunctions.mul(DensityFunctions.add(DensityFunctions.add(ridges.abs(), DensityFunctions.constant(-0.6666666666666666D)).abs(), DensityFunctions.constant(-0.3333333333333333D)), DensityFunctions.constant(-3.0D));
    }
    
    private static DensityFunction splineWithBlending(DensityFunction spline, DensityFunction blendTarget) {
        return DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.lerp(DensityFunctions.blendAlpha(), blendTarget, spline)));
    }
    
    private static ResourceKey<DensityFunction> createKey(String name) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, UTFCommon.location(name));
    }
    
    private static float yGradientRange(float range) {
    	return 1.0F + (-range / SCALER);
    }
}