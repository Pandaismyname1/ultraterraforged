package com.pandaismyname1.ultraterraforged.preset.option;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.data.preset.settings.ClimateSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.RiverSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.TerrainSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.spawn.SpawnType;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.ContinentType;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.DistanceFunction;

/**
 * Every user-facing preset setting, grouped into pages and categories in display order.
 *
 * Option paths match the keys in the preset file. Ranges and constraints mirror what the pre-0.1 editor
 * pages allowed, with value ordering now enforced on real values rather than slider positions.
 */
public final class PresetOptions {

	// World

	private static final Predicate<Preset> CLASSIC_CONTINENTS = (preset) -> {
		ContinentType type = preset.world().continent.continentType;
		return type == ContinentType.MULTI || type == ContinentType.SINGLE;
	};
	private static final Predicate<Preset> IMPROVED_CONTINENTS = (preset) -> preset.world().continent.continentType == ContinentType.MULTI_IMPROVED;

	public static final EnumOption<ContinentType> CONTINENT_TYPE = EnumOption.<ContinentType>builder("world.continent.continentType")
		.translation(UTFTranslationKeys.GUI_BUTTON_CONTINENT_TYPE)
		.values(List.of(ContinentType.MULTI, ContinentType.SINGLE, ContinentType.MULTI_IMPROVED, ContinentType.EXPERIMENTAL))
		.bind((p) -> p.world().continent.continentType, (p, v) -> p.world().continent.continentType = v)
		.build();

	public static final EnumOption<DistanceFunction> CONTINENT_SHAPE = EnumOption.<DistanceFunction>builder("world.continent.continentShape")
		.translation(UTFTranslationKeys.GUI_BUTTON_CONTINENT_SHAPE)
		.values(List.of(DistanceFunction.values()))
		.bind((p) -> p.world().continent.continentShape, (p, v) -> p.world().continent.continentShape = v)
		.activeWhen(CLASSIC_CONTINENTS)
		.build();

	public static final IntOption CONTINENT_SCALE = IntOption.builder("world.continent.continentScale")
		.translation(UTFTranslationKeys.GUI_SLIDER_CONTINENT_SCALE)
		.range(100, 10000)
		.bind((p) -> p.world().continent.continentScale, (p, v) -> p.world().continent.continentScale = v)
		.build();

	public static final FloatOption CONTINENT_JITTER = FloatOption.builder("world.continent.continentJitter")
		.translation(UTFTranslationKeys.GUI_SLIDER_CONTINENT_JITTER)
		.range(0.5F, 1.0F)
		.bind((p) -> p.world().continent.continentJitter, (p, v) -> p.world().continent.continentJitter = v)
		.build();

	public static final FloatOption CONTINENT_SKIPPING = FloatOption.builder("world.continent.continentSkipping")
		.translation(UTFTranslationKeys.GUI_SLIDER_CONTINENT_SKIPPING)
		.range(0.0F, 1.0F)
		.bind((p) -> p.world().continent.continentSkipping, (p, v) -> p.world().continent.continentSkipping = v)
		.activeWhen(IMPROVED_CONTINENTS)
		.build();

	public static final FloatOption CONTINENT_SIZE_VARIANCE = FloatOption.builder("world.continent.continentSizeVariance")
		.translation(UTFTranslationKeys.GUI_SLIDER_CONTINENT_SIZE_VARIANCE)
		.range(0.0F, 0.75F)
		.bind((p) -> p.world().continent.continentSizeVariance, (p, v) -> p.world().continent.continentSizeVariance = v)
		.activeWhen(IMPROVED_CONTINENTS)
		.build();

	public static final IntOption CONTINENT_NOISE_OCTAVES = IntOption.builder("world.continent.continentNoiseOctaves")
		.translation(UTFTranslationKeys.GUI_SLIDER_CONTINENT_NOISE_OCTAVES)
		.range(1, 5)
		.bind((p) -> p.world().continent.continentNoiseOctaves, (p, v) -> p.world().continent.continentNoiseOctaves = v)
		.activeWhen(IMPROVED_CONTINENTS)
		.build();

	public static final FloatOption CONTINENT_NOISE_GAIN = FloatOption.builder("world.continent.continentNoiseGain")
		.translation(UTFTranslationKeys.GUI_SLIDER_CONTINENT_NOISE_GAIN)
		.range(0.0F, 0.5F)
		.bind((p) -> p.world().continent.continentNoiseGain, (p, v) -> p.world().continent.continentNoiseGain = v)
		.activeWhen(IMPROVED_CONTINENTS)
		.build();

	public static final FloatOption CONTINENT_NOISE_LACUNARITY = FloatOption.builder("world.continent.continentNoiseLacunarity")
		.translation(UTFTranslationKeys.GUI_SLIDER_CONTINENT_NOISE_LACUNARITY)
		.range(1.0F, 10.0F)
		.bind((p) -> p.world().continent.continentNoiseLacunarity, (p, v) -> p.world().continent.continentNoiseLacunarity = v)
		.activeWhen(IMPROVED_CONTINENTS)
		.build();

	// island control points are kept in the file but the current generator doesn't use them; legacy presets use -1 for no islands
	public static final FloatOption ISLAND_INLAND = FloatOption.builder("world.controlPoints.islandInland")
		.translation(UTFTranslationKeys.GUI_SLIDER_ISLAND_INLAND)
		.range(-1.0F, 1.0F)
		.atMost((p) -> p.world().controlPoints.islandCoast)
		.bind((p) -> p.world().controlPoints.islandInland, (p, v) -> p.world().controlPoints.islandInland = v)
		.hidden()
		.build();

	public static final FloatOption ISLAND_COAST = FloatOption.builder("world.controlPoints.islandCoast")
		.translation(UTFTranslationKeys.GUI_SLIDER_ISLAND_COAST)
		.range(-1.0F, 1.0F)
		.atLeast((p) -> p.world().controlPoints.islandInland)
		.bind((p) -> p.world().controlPoints.islandCoast, (p, v) -> p.world().controlPoints.islandCoast = v)
		.hidden()
		.build();

	// ocean to inland transition points must stay in order
	public static final FloatOption DEEP_OCEAN = FloatOption.builder("world.controlPoints.deepOcean")
		.translation(UTFTranslationKeys.GUI_SLIDER_DEEP_OCEAN)
		.range(0.0F, 1.0F)
		.atMost((p) -> p.world().controlPoints.shallowOcean)
		.bind((p) -> p.world().controlPoints.deepOcean, (p, v) -> p.world().controlPoints.deepOcean = v)
		.build();

	public static final FloatOption SHALLOW_OCEAN = FloatOption.builder("world.controlPoints.shallowOcean")
		.translation(UTFTranslationKeys.GUI_SLIDER_SHALLOW_OCEAN)
		.range(0.0F, 1.0F)
		.atLeast((p) -> p.world().controlPoints.deepOcean)
		.atMost((p) -> p.world().controlPoints.beach)
		.bind((p) -> p.world().controlPoints.shallowOcean, (p, v) -> p.world().controlPoints.shallowOcean = v)
		.build();

	public static final FloatOption BEACH = FloatOption.builder("world.controlPoints.beach")
		.translation(UTFTranslationKeys.GUI_SLIDER_BEACH)
		.range(0.0F, 1.0F)
		.atLeast((p) -> p.world().controlPoints.shallowOcean)
		.atMost((p) -> p.world().controlPoints.coast)
		.bind((p) -> p.world().controlPoints.beach, (p, v) -> p.world().controlPoints.beach = v)
		.build();

	public static final FloatOption COAST = FloatOption.builder("world.controlPoints.coast")
		.translation(UTFTranslationKeys.GUI_SLIDER_COAST)
		.range(0.0F, 1.0F)
		.atLeast((p) -> p.world().controlPoints.beach)
		.atMost((p) -> p.world().controlPoints.nearInland)
		.bind((p) -> p.world().controlPoints.coast, (p, v) -> p.world().controlPoints.coast = v)
		.build();

	public static final FloatOption NEAR_INLAND = FloatOption.builder("world.controlPoints.inland")
		.translation(UTFTranslationKeys.GUI_SLIDER_NEAR_INLAND)
		.range(0.0F, 5.0F)
		.atLeast((p) -> p.world().controlPoints.coast)
		.bind((p) -> p.world().controlPoints.nearInland, (p, v) -> p.world().controlPoints.nearInland = v)
		.build();

	public static final FloatOption MID_INLAND = FloatOption.builder("world.controlPoints.midInland")
		.translation(UTFTranslationKeys.GUI_SLIDER_MID_INLAND)
		.range(0.0F, 5.0F)
		.bind((p) -> p.world().controlPoints.midInland, (p, v) -> p.world().controlPoints.midInland = v)
		.build();

	public static final FloatOption FAR_INLAND = FloatOption.builder("world.controlPoints.farInland")
		.translation(UTFTranslationKeys.GUI_SLIDER_FAR_INLAND)
		.range(0.0F, 5.0F)
		.bind((p) -> p.world().controlPoints.farInland, (p, v) -> p.world().controlPoints.farInland = v)
		.build();

	public static final EnumOption<SpawnType> SPAWN_TYPE = EnumOption.<SpawnType>builder("world.properties.spawnType")
		.translation(UTFTranslationKeys.GUI_BUTTON_SPAWN_TYPE)
		.values(List.of(SpawnType.values()))
		.bind((p) -> p.world().properties.spawnType, (p, v) -> p.world().properties.spawnType = v)
		.build();

	public static final IntOption WORLD_HEIGHT = IntOption.builder("world.properties.worldHeight")
		.translation(UTFTranslationKeys.GUI_SLIDER_WORLD_HEIGHT)
		.range(16, 1024)
		.step(16)
		.tag(OptionTag.MEDIUM_PERFORMANCE_IMPACT)
		.bind((p) -> p.world().properties.worldHeight, (p, v) -> p.world().properties.worldHeight = v)
		.build();

	public static final IntOption WORLD_DEPTH = IntOption.builder("world.properties.worldDepth")
		.translation(UTFTranslationKeys.GUI_SLIDER_WORLD_DEPTH)
		.range(0, 1024)
		.step(16)
		.tag(OptionTag.MEDIUM_PERFORMANCE_IMPACT)
		.bind((p) -> p.world().properties.worldDepth, (p, v) -> p.world().properties.worldDepth = v)
		.build();

	public static final IntOption SEA_LEVEL = IntOption.builder("world.properties.seaLevel")
		.translation(UTFTranslationKeys.GUI_SLIDER_SEA_LEVEL)
		.range(0, 255)
		.bind((p) -> p.world().properties.seaLevel, (p, v) -> p.world().properties.seaLevel = v)
		.build();

	public static final IntOption LAVA_LEVEL = IntOption.builder("world.properties.lavaLevel")
		.translation(UTFTranslationKeys.GUI_SLIDER_LAVA_LEVEL)
		.range(-1024, 1024)
		.bind((p) -> p.world().properties.lavaLevel, (p, v) -> p.world().properties.lavaLevel = v)
		.build();

	public static final Page WORLD = Page.of("world", UTFTranslationKeys.GUI_WORLD_SETTINGS_TITLE,
		Category.of("continent", UTFTranslationKeys.GUI_LABEL_CONTINENT,
			CONTINENT_TYPE, CONTINENT_SHAPE, CONTINENT_SCALE, CONTINENT_JITTER, CONTINENT_SKIPPING, CONTINENT_SIZE_VARIANCE,
			CONTINENT_NOISE_OCTAVES, CONTINENT_NOISE_GAIN, CONTINENT_NOISE_LACUNARITY
		),
		Category.of("controlPoints", UTFTranslationKeys.GUI_LABEL_CONTROL_POINTS,
			ISLAND_INLAND, ISLAND_COAST, DEEP_OCEAN, SHALLOW_OCEAN, BEACH, COAST, NEAR_INLAND, MID_INLAND, FAR_INLAND
		),
		Category.of("properties", UTFTranslationKeys.GUI_LABEL_PROPERTIES,
			SPAWN_TYPE, WORLD_HEIGHT, WORLD_DEPTH, SEA_LEVEL, LAVA_LEVEL
		)
	);

	// Surface

	private static final Predicate<Preset> EROSION = (preset) -> preset.miscellaneous().erosionDecorator;
	private static final Predicate<Preset> STRATA = (preset) -> preset.miscellaneous().strataDecorator;

	public static final Page SURFACE = Page.of("surface", UTFTranslationKeys.GUI_SURFACE_SETTINGS_TITLE,
		Category.of("strata", UTFTranslationKeys.GUI_LABEL_STRATA,
			BoolOption.builder("miscellaneous.strataDecorator")
				.translation(UTFTranslationKeys.GUI_BUTTON_STRATA_DECORATOR)
				.bind((p) -> p.miscellaneous().strataDecorator, (p, v) -> p.miscellaneous().strataDecorator = v)
				.build(),
			IntOption.builder("miscellaneous.strataRegionSize")
				.translation(UTFTranslationKeys.GUI_SLIDER_STRATA_REGION_SIZE)
				.range(50, 1000)
				.activeWhen(STRATA)
				.bind((p) -> p.miscellaneous().strataRegionSize, (p, v) -> p.miscellaneous().strataRegionSize = v)
				.build(),
			BoolOption.builder("miscellaneous.oreCompatibleStoneOnly")
				.translation(UTFTranslationKeys.GUI_BUTTON_ORE_COMPATIBLE_STONE_ONLY)
				.activeWhen(STRATA)
				.bind((p) -> p.miscellaneous().oreCompatibleStoneOnly, (p, v) -> p.miscellaneous().oreCompatibleStoneOnly = v)
				.build(),
			BoolOption.builder("miscellaneous.plainStoneErosion")
				.translation(UTFTranslationKeys.GUI_BUTTON_PLAIN_STONE_EROSION)
				.activeWhen(STRATA)
				.activeWhen(EROSION)
				.bind((p) -> p.miscellaneous().plainStoneErosion, (p, v) -> p.miscellaneous().plainStoneErosion = v)
				.build()
		),
		Category.of("erosion", UTFTranslationKeys.GUI_LABEL_SURFACE_EROSION,
			BoolOption.builder("miscellaneous.erosionDecorator")
				.translation(UTFTranslationKeys.GUI_BUTTON_EROSION_DECORATOR)
				.bind((p) -> p.miscellaneous().erosionDecorator, (p, v) -> p.miscellaneous().erosionDecorator = v)
				.build(),
			IntOption.builder("surface.erosion.rockVariance")
				.translation(UTFTranslationKeys.GUI_SLIDER_ROCK_VARIANCE)
				.range(0, 255)
				.bind((p) -> p.surface().erosion().rockVariance, (p, v) -> p.surface().erosion().rockVariance = v)
				.activeWhen(EROSION)
				.build(),
			IntOption.builder("surface.erosion.rockMin")
				.translation(UTFTranslationKeys.GUI_SLIDER_ROCK_MIN)
				.range(-1024, 1024)
				.bind((p) -> p.surface().erosion().rockMin, (p, v) -> p.surface().erosion().rockMin = v)
				.activeWhen(EROSION)
				.build(),
			IntOption.builder("surface.erosion.dirtVariance")
				.translation(UTFTranslationKeys.GUI_SLIDER_DIRT_VARIANCE)
				.range(0, 255)
				.bind((p) -> p.surface().erosion().dirtVariance, (p, v) -> p.surface().erosion().dirtVariance = v)
				.activeWhen(EROSION)
				.build(),
			IntOption.builder("surface.erosion.dirtMin")
				.translation(UTFTranslationKeys.GUI_SLIDER_DIRT_MIN)
				.range(-1024, 1024)
				.bind((p) -> p.surface().erosion().dirtMin, (p, v) -> p.surface().erosion().dirtMin = v)
				.activeWhen(EROSION)
				.build(),
			// steepness thresholds must stay ordered scree <= dirt <= rock
			FloatOption.builder("surface.erosion.rockSteepness")
				.translation(UTFTranslationKeys.GUI_SLIDER_ROCK_STEEPNESS)
				.range(0.0F, 1.0F)
				.atLeast((p) -> p.surface().erosion().dirtSteepness)
				.bind((p) -> p.surface().erosion().rockSteepness, (p, v) -> p.surface().erosion().rockSteepness = v)
				.activeWhen(EROSION)
				.build(),
			FloatOption.builder("surface.erosion.dirtSteepness")
				.translation(UTFTranslationKeys.GUI_SLIDER_DIRT_STEEPNESS)
				.range(0.0F, 1.0F)
				.atLeast((p) -> p.surface().erosion().screeSteepness)
				.atMost((p) -> p.surface().erosion().rockSteepness)
				.bind((p) -> p.surface().erosion().dirtSteepness, (p, v) -> p.surface().erosion().dirtSteepness = v)
				.activeWhen(EROSION)
				.build(),
			FloatOption.builder("surface.erosion.screeSteepness")
				.translation(UTFTranslationKeys.GUI_SLIDER_SCREE_STEEPNESS)
				.range(0.0F, 1.0F)
				.atMost((p) -> p.surface().erosion().dirtSteepness)
				.bind((p) -> p.surface().erosion().screeSteepness, (p, v) -> p.surface().erosion().screeSteepness = v)
				.activeWhen(EROSION)
				.build(),
			FloatOption.builder("surface.erosion.snowSteepness")
				.translation(UTFTranslationKeys.GUI_SLIDER_SNOW_STEEPNESS)
				.range(0.0F, 1.0F)
				.bind((p) -> p.surface().erosion().snowSteepness, (p, v) -> p.surface().erosion().snowSteepness = v)
				.build(),
			IntOption.builder("surface.erosion.snowAspect")
				.translation(UTFTranslationKeys.GUI_SLIDER_SNOW_ASPECT)
				.range(0, 60)
				.bind((p) -> p.surface().erosion().snowAspect, (p, v) -> p.surface().erosion().snowAspect = v)
				.build(),
			FloatOption.builder("surface.erosion.heightModifier")
				.translation(UTFTranslationKeys.GUI_SLIDER_HEIGHT_MODIFIER)
				.range(0.0F, 255.0F)
				.bind((p) -> p.surface().erosion().heightModifier, (p, v) -> p.surface().erosion().heightModifier = v)
				.build(),
			FloatOption.builder("surface.erosion.slopeModifier")
				.translation(UTFTranslationKeys.GUI_SLIDER_SLOPE_MODIFIER)
				.range(0.0F, 255.0F)
				.bind((p) -> p.surface().erosion().slopeModifier, (p, v) -> p.surface().erosion().slopeModifier = v)
				.build()
		)
	);

	// Caves

	public static final Page CAVES = Page.of("caves", UTFTranslationKeys.GUI_CAVE_SETTINGS_TITLE,
		Category.of("noiseCaves", UTFTranslationKeys.GUI_LABEL_NOISE_CAVES,
			caveChance("entranceCaveProbability", UTFTranslationKeys.GUI_SLIDER_ENTRANCE_CAVE_CHANCE, (p) -> p.caves().entranceCaveProbability, (p, v) -> p.caves().entranceCaveProbability = v),
			FloatOption.builder("caves.cheeseCaveDepthOffset")
				.translation(UTFTranslationKeys.GUI_SLIDER_SURFACE_DENSITY_THRESHOLD)
				.range(1.5625F, 10.0F)
				.bind((p) -> p.caves().cheeseCaveDepthOffset, (p, v) -> p.caves().cheeseCaveDepthOffset = v)
				.build(),
			caveChance("cheeseCaveProbability", UTFTranslationKeys.GUI_SLIDER_CHEESE_CAVE_CHANCE, (p) -> p.caves().cheeseCaveProbability, (p, v) -> p.caves().cheeseCaveProbability = v),
			caveChance("spaghettiCaveProbability", UTFTranslationKeys.GUI_SLIDER_SPAGHETTI_CAVE_CHANCE, (p) -> p.caves().spaghettiCaveProbability, (p, v) -> p.caves().spaghettiCaveProbability = v),
			caveChance("noodleCaveProbability", UTFTranslationKeys.GUI_SLIDER_NOODLE_CAVE_CHANCE, (p) -> p.caves().noodleCaveProbability, (p, v) -> p.caves().noodleCaveProbability = v),
			BoolOption.builder("caves.largeOreVeins")
				.translation(UTFTranslationKeys.GUI_BUTTON_LARGE_ORE_VEINS)
				.bind((p) -> p.caves().largeOreVeins, (p, v) -> p.caves().largeOreVeins = v)
				.build()
		),
		Category.of("carvers", UTFTranslationKeys.GUI_LABEL_CARVERS,
			caveChance("caveCarverProbability", UTFTranslationKeys.GUI_SLIDER_CAVE_CARVER_CHANCE, (p) -> p.caves().caveCarverProbability, (p, v) -> p.caves().caveCarverProbability = v),
			FloatOption.builder("caves.deepCaveCarverProbability")
				.translation(UTFTranslationKeys.GUI_SLIDER_DEEP_CAVE_CARVER_CHANCE)
				.range(0.0F, 1.0F)
				// legacy distribution has no deep cave carvers
				.activeWhen((p) -> !p.caves().legacyCarverDistribution)
				.bind((p) -> p.caves().deepCaveCarverProbability, (p, v) -> p.caves().deepCaveCarverProbability = v)
				.build(),
			caveChance("ravineCarverProbability", UTFTranslationKeys.GUI_SLIDER_RAVINE_CARVER_CHANCE, (p) -> p.caves().ravineCarverProbability, (p, v) -> p.caves().ravineCarverProbability = v),
			BoolOption.builder("caves.legacyCarverDistribution")
				.translation(UTFTranslationKeys.GUI_BUTTON_LEGACY_CARVER_DISTRIBUTION)
				.bind((p) -> p.caves().legacyCarverDistribution, (p, v) -> p.caves().legacyCarverDistribution = v)
				.build()
		)
	);

	// Climate

	public static final Page CLIMATE = Page.of("climate", UTFTranslationKeys.GUI_CLIMATE_SETTINGS_TITLE,
		climateRange("temperature", UTFTranslationKeys.GUI_LABEL_TEMPERATURE, (p) -> p.climate().temperature,
			UTFTranslationKeys.GUI_SLIDER_TEMPERATURE_SCALE, UTFTranslationKeys.GUI_SLIDER_TEMPERATURE_FALLOFF,
			UTFTranslationKeys.GUI_SLIDER_TEMPERATURE_MIN, UTFTranslationKeys.GUI_SLIDER_TEMPERATURE_MAX, UTFTranslationKeys.GUI_SLIDER_TEMPERATURE_BIAS
		),
		climateRange("moisture", UTFTranslationKeys.GUI_LABEL_MOISTURE, (p) -> p.climate().moisture,
			UTFTranslationKeys.GUI_SLIDER_MOISTURE_SCALE, UTFTranslationKeys.GUI_SLIDER_MOISTURE_FALLOFF,
			UTFTranslationKeys.GUI_SLIDER_MOISTURE_MIN, UTFTranslationKeys.GUI_SLIDER_MOISTURE_MAX, UTFTranslationKeys.GUI_SLIDER_MOISTURE_BIAS
		),
		Category.of("biomeShape", UTFTranslationKeys.GUI_LABEL_BIOME_SHAPE,
			IntOption.builder("climate.biomeShape.biomeSize")
				.translation(UTFTranslationKeys.GUI_SLIDER_BIOME_SIZE)
				.range(50, 2000)
				.bind((p) -> p.climate().biomeShape.biomeSize, (p, v) -> p.climate().biomeShape.biomeSize = v)
				.build(),
			IntOption.builder("climate.biomeShape.macroNoiseSize")
				.translation(UTFTranslationKeys.GUI_SLIDER_MACRO_NOISE_SIZE)
				.range(1, 20)
				.bind((p) -> p.climate().biomeShape.macroNoiseSize, (p, v) -> p.climate().biomeShape.macroNoiseSize = v)
				.build(),
			IntOption.builder("climate.biomeShape.biomeWarpScale")
				.translation(UTFTranslationKeys.GUI_SLIDER_BIOME_WARP_SCALE)
				.range(1, 500)
				.bind((p) -> p.climate().biomeShape.biomeWarpScale, (p, v) -> p.climate().biomeShape.biomeWarpScale = v)
				.build(),
			IntOption.builder("climate.biomeShape.biomeWarpStrength")
				.translation(UTFTranslationKeys.GUI_SLIDER_BIOME_WARP_STRENGTH)
				.range(1, 500)
				.bind((p) -> p.climate().biomeShape.biomeWarpStrength, (p, v) -> p.climate().biomeShape.biomeWarpStrength = v)
				.build()
		),
		Category.of("biomeEdgeShape", UTFTranslationKeys.GUI_LABEL_BIOME_EDGE_SHAPE,
			EnumOption.<ClimateSettings.BiomeNoise.EdgeType>builder("climate.biomeEdgeShape.type")
				.translation(UTFTranslationKeys.GUI_BUTTON_BIOME_EDGE_TYPE)
				.values(List.of(ClimateSettings.BiomeNoise.EdgeType.values()))
				.bind((p) -> p.climate().biomeEdgeShape.type, (p, v) -> p.climate().biomeEdgeShape.type = v)
				.build(),
			IntOption.builder("climate.biomeEdgeShape.scale")
				.translation(UTFTranslationKeys.GUI_SLIDER_BIOME_EDGE_SCALE)
				.range(1, 500)
				.bind((p) -> p.climate().biomeEdgeShape.scale, (p, v) -> p.climate().biomeEdgeShape.scale = v)
				.build(),
			IntOption.builder("climate.biomeEdgeShape.octaves")
				.translation(UTFTranslationKeys.GUI_SLIDER_BIOME_EDGE_OCTAVES)
				.range(1, 5)
				.bind((p) -> p.climate().biomeEdgeShape.octaves, (p, v) -> p.climate().biomeEdgeShape.octaves = v)
				.build(),
			FloatOption.builder("climate.biomeEdgeShape.gain")
				.translation(UTFTranslationKeys.GUI_SLIDER_BIOME_EDGE_GAIN)
				.range(0.0F, 5.5F)
				.bind((p) -> p.climate().biomeEdgeShape.gain, (p, v) -> p.climate().biomeEdgeShape.gain = v)
				.build(),
			FloatOption.builder("climate.biomeEdgeShape.lacunarity")
				.translation(UTFTranslationKeys.GUI_SLIDER_BIOME_EDGE_LACUNARITY)
				.range(0.0F, 10.5F)
				.bind((p) -> p.climate().biomeEdgeShape.lacunarity, (p, v) -> p.climate().biomeEdgeShape.lacunarity = v)
				.build(),
			IntOption.builder("climate.biomeEdgeShape.strength")
				.translation(UTFTranslationKeys.GUI_SLIDER_BIOME_EDGE_STRENGTH)
				.range(1, 500)
				.bind((p) -> p.climate().biomeEdgeShape.strength, (p, v) -> p.climate().biomeEdgeShape.strength = v)
				.build()
		)
	);

	// Terrain

	public static final Page TERRAIN = Page.of("terrain", UTFTranslationKeys.GUI_TERRAIN_SETTINGS_TITLE,
		Category.of("general", UTFTranslationKeys.GUI_LABEL_GENERAL,
			IntOption.builder("terrain.general.terrainSeedOffset")
				.translation(UTFTranslationKeys.GUI_BUTTON_TERRAIN_SEED_OFFSET)
				.seed()
				.bind((p) -> p.terrain().general.terrainSeedOffset, (p, v) -> p.terrain().general.terrainSeedOffset = v)
				.build(),
			IntOption.builder("terrain.general.terrainRegionSize")
				.translation(UTFTranslationKeys.GUI_SLIDER_TERRAIN_REGION_SIZE)
				.range(125, 5000)
				.bind((p) -> p.terrain().general.terrainRegionSize, (p, v) -> p.terrain().general.terrainRegionSize = v)
				.build(),
			// the global scales were never shown in the editor
			FloatOption.builder("terrain.general.globalVerticalScale")
				.translation(UTFTranslationKeys.GUI_SLIDER_GLOBAL_VERTICAL_SCALE)
				.range(0.01F, 1.0F)
				.bind((p) -> p.terrain().general.globalVerticalScale, (p, v) -> p.terrain().general.globalVerticalScale = v)
				.hidden()
				.build(),
			FloatOption.builder("terrain.general.globalHorizontalScale")
				.translation(UTFTranslationKeys.GUI_SLIDER_GLOBAL_HORIZONTAL_SCALE)
				.range(0.01F, 5.0F)
				.bind((p) -> p.terrain().general.globalHorizontalScale, (p, v) -> p.terrain().general.globalHorizontalScale = v)
				.hidden()
				.build(),
			BoolOption.builder("terrain.general.fancyMountains")
				.translation(UTFTranslationKeys.GUI_BUTTON_FANCY_MOUNTAINS)
				.bind((p) -> p.terrain().general.fancyMountains, (p, v) -> p.terrain().general.fancyMountains = v)
				.build()
		),
		terrainType("steppe", UTFTranslationKeys.GUI_LABEL_STEPPE, (p) -> p.terrain().steppe, true, true),
		terrainType("plains", UTFTranslationKeys.GUI_LABEL_PLAINS, (p) -> p.terrain().plains, true, false),
		terrainType("hills", UTFTranslationKeys.GUI_LABEL_HILLS, (p) -> p.terrain().hills, false, true),
		terrainType("dales", UTFTranslationKeys.GUI_LABEL_DALES, (p) -> p.terrain().dales, false, true),
		terrainType("plateau", UTFTranslationKeys.GUI_LABEL_PLATEAU, (p) -> p.terrain().plateau, false, true),
		terrainType("badlands", UTFTranslationKeys.GUI_LABEL_BADLANDS, (p) -> p.terrain().badlands, false, true),
		terrainType("torridonian", UTFTranslationKeys.GUI_LABEL_TORRIDONIAN, (p) -> p.terrain().torridonian, false, true),
		terrainType("mountains", UTFTranslationKeys.GUI_LABEL_MOUNTAINS, (p) -> p.terrain().mountains, true, true),
		terrainType("volcano", UTFTranslationKeys.GUI_LABEL_VOLCANO, (p) -> p.terrain().volcano, false, false)
	);

	// Rivers

	public static final Page RIVERS = Page.of("rivers", UTFTranslationKeys.GUI_RIVER_SETTINGS_TITLE,
		Category.of("general", null,
			IntOption.builder("rivers.seedOffset")
				.translation(UTFTranslationKeys.GUI_BUTTON_RIVER_SEED_OFFSET)
				.seed()
				.bind((p) -> p.rivers().seedOffset, (p, v) -> p.rivers().seedOffset = v)
				.build(),
			IntOption.builder("rivers.riverCount")
				.translation(UTFTranslationKeys.GUI_SLIDER_RIVER_COUNT)
				.range(0, 30)
				.bind((p) -> p.rivers().riverCount, (p, v) -> p.rivers().riverCount = v)
				.build(),
			BoolOption.builder("rivers.raisedWater")
				.translation(UTFTranslationKeys.GUI_BUTTON_RAISED_WATER)
				.bind((p) -> p.rivers().raisedWater, (p, v) -> p.rivers().raisedWater = v)
				.build(),
			IntOption.builder("rivers.gorgeDepth")
				.translation(UTFTranslationKeys.GUI_SLIDER_GORGE_DEPTH)
				.range(4, 80)
				.activeWhen((p) -> p.rivers().raisedWater)
				.bind((p) -> p.rivers().gorgeDepth, (p, v) -> p.rivers().gorgeDepth = v)
				.build(),
			BoolOption.builder("rivers.winding")
				.translation(UTFTranslationKeys.GUI_BUTTON_WINDING_RIVERS)
				.bind((p) -> p.rivers().winding, (p, v) -> p.rivers().winding = v)
				.build()
		),
		river("mainRivers", UTFTranslationKeys.GUI_LABEL_MAIN_RIVERS, (p) -> p.rivers().mainRivers, 50, 200, 150),
		river("branchRivers", UTFTranslationKeys.GUI_LABEL_BRANCH_RIVERS, (p) -> p.rivers().branchRivers, 20, 50, 100),
		Category.of("lakes", UTFTranslationKeys.GUI_LABEL_LAKES,
			FloatOption.builder("rivers.lakes.chance")
				.translation(UTFTranslationKeys.GUI_SLIDER_LAKE_CHANCE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.rivers().lakes.chance, (p, v) -> p.rivers().lakes.chance = v)
				.build(),
			IntOption.builder("rivers.lakes.depth")
				.translation(UTFTranslationKeys.GUI_SLIDER_LAKE_DEPTH)
				.range(1, 20)
				.bind((p) -> p.rivers().lakes.depth, (p, v) -> p.rivers().lakes.depth = v)
				.build(),
			IntOption.builder("rivers.lakes.sizeMin")
				.translation(UTFTranslationKeys.GUI_SLIDER_LAKE_SIZE_MIN)
				.range(1, 100)
				.atMost((p) -> p.rivers().lakes.sizeMax)
				.bind((p) -> p.rivers().lakes.sizeMin, (p, v) -> p.rivers().lakes.sizeMin = v)
				.build(),
			IntOption.builder("rivers.lakes.sizeMax")
				.translation(UTFTranslationKeys.GUI_SLIDER_LAKE_SIZE_MAX)
				.range(1, 500)
				.atLeast((p) -> p.rivers().lakes.sizeMin)
				.bind((p) -> p.rivers().lakes.sizeMax, (p, v) -> p.rivers().lakes.sizeMax = v)
				.build(),
			IntOption.builder("rivers.lakes.minBankHeight")
				.translation(UTFTranslationKeys.GUI_SLIDER_LAKE_MIN_BANK_HEIGHT)
				.range(1, 10)
				.atMost((p) -> p.rivers().lakes.maxBankHeight)
				.bind((p) -> p.rivers().lakes.minBankHeight, (p, v) -> p.rivers().lakes.minBankHeight = v)
				.build(),
			IntOption.builder("rivers.lakes.maxBankHeight")
				.translation(UTFTranslationKeys.GUI_SLIDER_LAKE_MAX_BANK_HEIGHT)
				.range(1, 10)
				.atLeast((p) -> p.rivers().lakes.minBankHeight)
				.bind((p) -> p.rivers().lakes.maxBankHeight, (p, v) -> p.rivers().lakes.maxBankHeight = v)
				.build()
		),
		Category.of("wetlands", UTFTranslationKeys.GUI_LABEL_WETLANDS,
			FloatOption.builder("rivers.wetlands.chance")
				.translation(UTFTranslationKeys.GUI_SLIDER_WETLAND_CHANCE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.rivers().wetlands.chance, (p, v) -> p.rivers().wetlands.chance = v)
				.build(),
			IntOption.builder("rivers.wetlands.sizeMin")
				.translation(UTFTranslationKeys.GUI_SLIDER_WETLAND_SIZE_MIN)
				.range(50, 500)
				.atMost((p) -> p.rivers().wetlands.sizeMax)
				.bind((p) -> p.rivers().wetlands.sizeMin, (p, v) -> p.rivers().wetlands.sizeMin = v)
				.build(),
			IntOption.builder("rivers.wetlands.sizeMax")
				.translation(UTFTranslationKeys.GUI_SLIDER_WETLAND_SIZE_MAX)
				.range(50, 500)
				.atLeast((p) -> p.rivers().wetlands.sizeMin)
				.bind((p) -> p.rivers().wetlands.sizeMax, (p, v) -> p.rivers().wetlands.sizeMax = v)
				.build()
		)
	);

	// Filters

	public static final Page FILTERS = Page.of("filters", UTFTranslationKeys.GUI_FILTER_SETTINGS_TITLE,
		Category.of("erosion", UTFTranslationKeys.GUI_LABEL_EROSION,
			IntOption.builder("filters.erosion.dropletsPerChunk")
				.translation(UTFTranslationKeys.GUI_SLIDER_EROSION_DROPLETS_PER_CHUNK)
				.range(10, 250)
				.tag(OptionTag.HEAVY_PERFORMANCE_IMPACT)
				.bind((p) -> p.filters().erosion.dropletsPerChunk, (p, v) -> p.filters().erosion.dropletsPerChunk = v)
				.build(),
			IntOption.builder("filters.erosion.dropletLifetime")
				.translation(UTFTranslationKeys.GUI_SLIDER_EROSION_DROPLET_LIFETIME)
				.range(1, 32)
				.tag(OptionTag.HEAVY_PERFORMANCE_IMPACT)
				.bind((p) -> p.filters().erosion.dropletLifetime, (p, v) -> p.filters().erosion.dropletLifetime = v)
				.build(),
			FloatOption.builder("filters.erosion.dropletVolume")
				.translation(UTFTranslationKeys.GUI_SLIDER_EROSION_DROPLET_VOLUME)
				.range(0.0F, 1.0F)
				.bind((p) -> p.filters().erosion.dropletVolume, (p, v) -> p.filters().erosion.dropletVolume = v)
				.build(),
			FloatOption.builder("filters.erosion.dropletVelocity")
				.translation(UTFTranslationKeys.GUI_SLIDER_EROSION_DROPLET_VELOCITY)
				.range(0.1F, 1.0F)
				.bind((p) -> p.filters().erosion.dropletVelocity, (p, v) -> p.filters().erosion.dropletVelocity = v)
				.build(),
			FloatOption.builder("filters.erosion.erosionRate")
				.translation(UTFTranslationKeys.GUI_SLIDER_EROSION_RATE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.filters().erosion.erosionRate, (p, v) -> p.filters().erosion.erosionRate = v)
				.build(),
			FloatOption.builder("filters.erosion.depositeRate")
				.translation(UTFTranslationKeys.GUI_SLIDER_DEPOSITE_RATE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.filters().erosion.depositeRate, (p, v) -> p.filters().erosion.depositeRate = v)
				.build()
		),
		Category.of("smoothing", UTFTranslationKeys.GUI_LABEL_SMOOTHING,
			IntOption.builder("filters.smoothing.iterations")
				.translation(UTFTranslationKeys.GUI_SLIDER_SMOOTHING_ITERATIONS)
				.range(0, 5)
				.tag(OptionTag.MEDIUM_PERFORMANCE_IMPACT)
				.bind((p) -> p.filters().smoothing.iterations, (p, v) -> p.filters().smoothing.iterations = v)
				.build(),
			FloatOption.builder("filters.smoothing.smoothingRadius")
				.translation(UTFTranslationKeys.GUI_SLIDER_SMOOTHING_RADIUS)
				.range(0.0F, 5.0F)
				.bind((p) -> p.filters().smoothing.smoothingRadius, (p, v) -> p.filters().smoothing.smoothingRadius = v)
				.build(),
			FloatOption.builder("filters.smoothing.smoothingRate")
				.translation(UTFTranslationKeys.GUI_SLIDER_SMOOTHING_RATE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.filters().smoothing.smoothingRate, (p, v) -> p.filters().smoothing.smoothingRate = v)
				.build()
		)
	);

	// Miscellaneous; the hidden options are kept in the file but the current generator doesn't read them

	public static final Page MISCELLANEOUS = Page.of("miscellaneous", UTFTranslationKeys.GUI_MISCELLANEOUS_SETTINGS_TITLE,
		Category.of("general", null,
			BoolOption.builder("miscellaneous.smoothLayerDecorator")
				.translation(UTFTranslationKeys.GUI_BUTTON_SMOOTH_LAYER_DECORATOR)
				.bind((p) -> p.miscellaneous().smoothLayerDecorator, (p, v) -> p.miscellaneous().smoothLayerDecorator = v)
				.build(),
			BoolOption.builder("miscellaneous.naturalSnowDecorator")
				.translation(UTFTranslationKeys.GUI_BUTTON_NATURAL_SNOW_DECORATOR)
				.bind((p) -> p.miscellaneous().naturalSnowDecorator, (p, v) -> p.miscellaneous().naturalSnowDecorator = v)
				.build(),
			BoolOption.builder("miscellaneous.riverBanks")
				.translation(UTFTranslationKeys.GUI_BUTTON_RIVER_BANKS)
				.bind((p) -> p.miscellaneous().riverBanks, (p, v) -> p.miscellaneous().riverBanks = v)
				.build(),
			BoolOption.builder("miscellaneous.customBiomeFeatures")
				.translation(UTFTranslationKeys.GUI_BUTTON_CUSTOM_BIOME_FEATURES)
				.bind((p) -> p.miscellaneous().customBiomeFeatures, (p, v) -> p.miscellaneous().customBiomeFeatures = v)
				.build(),
			BoolOption.builder("miscellaneous.vanillaSprings")
				.translation(UTFTranslationKeys.GUI_BUTTON_VANILLA_SPRINGS)
				.bind((p) -> p.miscellaneous().vanillaSprings, (p, v) -> p.miscellaneous().vanillaSprings = v)
				.build(),
			BoolOption.builder("miscellaneous.vanillaLavaLakes")
				.translation(UTFTranslationKeys.GUI_BUTTON_VANILLA_LAVA_LAKES)
				.bind((p) -> p.miscellaneous().vanillaLavaLakes, (p, v) -> p.miscellaneous().vanillaLavaLakes = v)
				.build(),
			BoolOption.builder("miscellaneous.vanillaLavaSprings")
				.translation(UTFTranslationKeys.GUI_BUTTON_VANILLA_LAVA_SPRINGS)
				.bind((p) -> p.miscellaneous().vanillaLavaSprings, (p, v) -> p.miscellaneous().vanillaLavaSprings = v)
				.build(),
			FloatOption.builder("miscellaneous.mountainBiomeUsage")
				.translation(UTFTranslationKeys.GUI_SLIDER_MOUNTAIN_BIOME_USAGE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.miscellaneous().mountainBiomeUsage, (p, v) -> p.miscellaneous().mountainBiomeUsage = v)
				.hidden()
				.build(),
			FloatOption.builder("miscellaneous.volcanoBiomeUsage")
				.translation(UTFTranslationKeys.GUI_SLIDER_VOLCANO_BIOME_USAGE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.miscellaneous().volcanoBiomeUsage, (p, v) -> p.miscellaneous().volcanoBiomeUsage = v)
				.hidden()
				.build()
		)
	);

	// Landforms

	private static final Predicate<Preset> BUTTES = (preset) -> preset.landforms().buttes.enabled;
	private static final Predicate<Preset> CANYONS = (preset) -> preset.landforms().canyons.enabled;
	private static final Predicate<Preset> SEA_CLIFFS = (preset) -> preset.landforms().seaCliffs.enabled;
	private static final Predicate<Preset> FJORDS = (preset) -> preset.landforms().fjords.enabled;
	private static final Predicate<Preset> ATOLLS = (preset) -> preset.landforms().atolls.enabled;
	private static final Predicate<Preset> DUNES = (preset) -> preset.landforms().dunes.enabled;
	private static final Predicate<Preset> TORS = (preset) -> preset.landforms().tors.enabled;
	private static final Predicate<Preset> SALT_FLATS = (preset) -> preset.landforms().saltFlats.enabled;
	private static final Predicate<Preset> ALLUVIAL_FANS = (preset) -> preset.landforms().alluvialFans.enabled;
	private static final Predicate<Preset> GLACIAL_VALLEYS = (preset) -> preset.landforms().glacialValleys.enabled;
	private static final Predicate<Preset> DRUMLINS = (preset) -> preset.landforms().drumlins.enabled;
	private static final Predicate<Preset> BARRIER_ISLANDS = (preset) -> preset.landforms().barrierIslands.enabled;
	private static final Predicate<Preset> KARST = (preset) -> preset.landforms().karst.enabled;
	private static final Predicate<Preset> DELTAS = (preset) -> preset.landforms().deltas.enabled;

	public static final Page LANDFORMS = Page.of("landforms", UTFTranslationKeys.GUI_LANDFORM_SETTINGS_TITLE,
		Category.of("buttes", UTFTranslationKeys.GUI_LABEL_BUTTES,
			BoolOption.builder("landforms.buttes.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_BUTTES)
				.bind((p) -> p.landforms().buttes.enabled, (p, v) -> p.landforms().buttes.enabled = v)
				.build(),
			FloatOption.builder("landforms.buttes.density")
				.translation(UTFTranslationKeys.GUI_SLIDER_BUTTE_DENSITY)
				.range(0.0F, 1.0F)
				.activeWhen(BUTTES)
				.bind((p) -> p.landforms().buttes.density, (p, v) -> p.landforms().buttes.density = v)
				.build(),
			IntOption.builder("landforms.buttes.height")
				.translation(UTFTranslationKeys.GUI_SLIDER_BUTTE_HEIGHT)
				.range(10, 120)
				.activeWhen(BUTTES)
				.bind((p) -> p.landforms().buttes.height, (p, v) -> p.landforms().buttes.height = v)
				.build(),
			FloatOption.builder("landforms.buttes.size")
				.translation(UTFTranslationKeys.GUI_SLIDER_BUTTE_SIZE)
				.range(0.5F, 2.5F)
				.activeWhen(BUTTES)
				.bind((p) -> p.landforms().buttes.size, (p, v) -> p.landforms().buttes.size = v)
				.build()
		),
		Category.of("canyons", UTFTranslationKeys.GUI_LABEL_CANYONS,
			BoolOption.builder("landforms.canyons.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_CANYONS)
				.bind((p) -> p.landforms().canyons.enabled, (p, v) -> p.landforms().canyons.enabled = v)
				.build(),
			IntOption.builder("landforms.canyons.depth")
				.translation(UTFTranslationKeys.GUI_SLIDER_CANYON_DEPTH)
				.range(10, 100)
				.activeWhen(CANYONS)
				.bind((p) -> p.landforms().canyons.depth, (p, v) -> p.landforms().canyons.depth = v)
				.build(),
			FloatOption.builder("landforms.canyons.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_CANYON_FREQUENCY)
				.range(0.2F, 3.0F)
				.activeWhen(CANYONS)
				.bind((p) -> p.landforms().canyons.frequency, (p, v) -> p.landforms().canyons.frequency = v)
				.build(),
			FloatOption.builder("landforms.canyons.width")
				.translation(UTFTranslationKeys.GUI_SLIDER_CANYON_WIDTH)
				.range(0.3F, 3.0F)
				.activeWhen(CANYONS)
				.bind((p) -> p.landforms().canyons.width, (p, v) -> p.landforms().canyons.width = v)
				.build()
		),
		Category.of("seaCliffs", UTFTranslationKeys.GUI_LABEL_SEA_CLIFFS,
			BoolOption.builder("landforms.seaCliffs.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_SEA_CLIFFS)
				.bind((p) -> p.landforms().seaCliffs.enabled, (p, v) -> p.landforms().seaCliffs.enabled = v)
				.build(),
			FloatOption.builder("landforms.seaCliffs.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_SEA_CLIFF_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(SEA_CLIFFS)
				.bind((p) -> p.landforms().seaCliffs.frequency, (p, v) -> p.landforms().seaCliffs.frequency = v)
				.build(),
			IntOption.builder("landforms.seaCliffs.height")
				.translation(UTFTranslationKeys.GUI_SLIDER_SEA_CLIFF_HEIGHT)
				.range(6, 80)
				.activeWhen(SEA_CLIFFS)
				.bind((p) -> p.landforms().seaCliffs.height, (p, v) -> p.landforms().seaCliffs.height = v)
				.build(),
			BoolOption.builder("landforms.seaCliffs.seaStacks")
				.translation(UTFTranslationKeys.GUI_BUTTON_SEA_STACKS)
				.activeWhen(SEA_CLIFFS)
				.bind((p) -> p.landforms().seaCliffs.seaStacks, (p, v) -> p.landforms().seaCliffs.seaStacks = v)
				.build(),
			FloatOption.builder("landforms.seaCliffs.gravelBeaches")
				.translation(UTFTranslationKeys.GUI_SLIDER_GRAVEL_BEACHES)
				.range(0.0F, 1.0F)
				.activeWhen(SEA_CLIFFS)
				.bind((p) -> p.landforms().seaCliffs.gravelBeaches, (p, v) -> p.landforms().seaCliffs.gravelBeaches = v)
				.build()
		),
		Category.of("fjords", UTFTranslationKeys.GUI_LABEL_FJORDS,
			BoolOption.builder("landforms.fjords.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_FJORDS)
				.bind((p) -> p.landforms().fjords.enabled, (p, v) -> p.landforms().fjords.enabled = v)
				.build(),
			IntOption.builder("landforms.fjords.depth")
				.translation(UTFTranslationKeys.GUI_SLIDER_FJORD_DEPTH)
				.range(10, 80)
				.activeWhen(FJORDS)
				.bind((p) -> p.landforms().fjords.depth, (p, v) -> p.landforms().fjords.depth = v)
				.build(),
			FloatOption.builder("landforms.fjords.reach")
				.translation(UTFTranslationKeys.GUI_SLIDER_FJORD_REACH)
				.range(0.3F, 2.5F)
				.activeWhen(FJORDS)
				.bind((p) -> p.landforms().fjords.reach, (p, v) -> p.landforms().fjords.reach = v)
				.build()
		),
		Category.of("atolls", UTFTranslationKeys.GUI_LABEL_ATOLLS,
			BoolOption.builder("landforms.atolls.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_ATOLLS)
				.bind((p) -> p.landforms().atolls.enabled, (p, v) -> p.landforms().atolls.enabled = v)
				.build(),
			FloatOption.builder("landforms.atolls.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_ATOLL_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(ATOLLS)
				.bind((p) -> p.landforms().atolls.frequency, (p, v) -> p.landforms().atolls.frequency = v)
				.build(),
			FloatOption.builder("landforms.atolls.size")
				.translation(UTFTranslationKeys.GUI_SLIDER_ATOLL_SIZE)
				.range(0.5F, 2.0F)
				.activeWhen(ATOLLS)
				.bind((p) -> p.landforms().atolls.size, (p, v) -> p.landforms().atolls.size = v)
				.build()
		),
		Category.of("dunes", UTFTranslationKeys.GUI_LABEL_DUNES,
			BoolOption.builder("landforms.dunes.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_DUNES)
				.bind((p) -> p.landforms().dunes.enabled, (p, v) -> p.landforms().dunes.enabled = v)
				.build(),
			IntOption.builder("landforms.dunes.height")
				.translation(UTFTranslationKeys.GUI_SLIDER_DUNE_HEIGHT)
				.range(3, 30)
				.activeWhen(DUNES)
				.bind((p) -> p.landforms().dunes.height, (p, v) -> p.landforms().dunes.height = v)
				.build(),
			FloatOption.builder("landforms.dunes.coverage")
				.translation(UTFTranslationKeys.GUI_SLIDER_DUNE_COVERAGE)
				.range(0.0F, 1.0F)
				.activeWhen(DUNES)
				.bind((p) -> p.landforms().dunes.coverage, (p, v) -> p.landforms().dunes.coverage = v)
				.build()
		),
		Category.of("tors", UTFTranslationKeys.GUI_LABEL_TORS,
			BoolOption.builder("landforms.tors.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_TORS)
				.bind((p) -> p.landforms().tors.enabled, (p, v) -> p.landforms().tors.enabled = v)
				.build(),
			FloatOption.builder("landforms.tors.density")
				.translation(UTFTranslationKeys.GUI_SLIDER_TOR_DENSITY)
				.range(0.0F, 1.0F)
				.activeWhen(TORS)
				.bind((p) -> p.landforms().tors.density, (p, v) -> p.landforms().tors.density = v)
				.build(),
			IntOption.builder("landforms.tors.height")
				.translation(UTFTranslationKeys.GUI_SLIDER_TOR_HEIGHT)
				.range(3, 20)
				.activeWhen(TORS)
				.bind((p) -> p.landforms().tors.height, (p, v) -> p.landforms().tors.height = v)
				.build()
		),
		Category.of("saltFlats", UTFTranslationKeys.GUI_LABEL_SALT_FLATS,
			BoolOption.builder("landforms.saltFlats.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_SALT_FLATS)
				.bind((p) -> p.landforms().saltFlats.enabled, (p, v) -> p.landforms().saltFlats.enabled = v)
				.build(),
			FloatOption.builder("landforms.saltFlats.coverage")
				.translation(UTFTranslationKeys.GUI_SLIDER_SALT_FLAT_COVERAGE)
				.range(0.0F, 1.0F)
				.activeWhen(SALT_FLATS)
				.bind((p) -> p.landforms().saltFlats.coverage, (p, v) -> p.landforms().saltFlats.coverage = v)
				.build()
		),
		Category.of("alluvialFans", UTFTranslationKeys.GUI_LABEL_ALLUVIAL_FANS,
			BoolOption.builder("landforms.alluvialFans.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_ALLUVIAL_FANS)
				.bind((p) -> p.landforms().alluvialFans.enabled, (p, v) -> p.landforms().alluvialFans.enabled = v)
				.build(),
			FloatOption.builder("landforms.alluvialFans.density")
				.translation(UTFTranslationKeys.GUI_SLIDER_ALLUVIAL_FAN_DENSITY)
				.range(0.0F, 1.0F)
				.activeWhen(ALLUVIAL_FANS)
				.bind((p) -> p.landforms().alluvialFans.density, (p, v) -> p.landforms().alluvialFans.density = v)
				.build(),
			FloatOption.builder("landforms.alluvialFans.size")
				.translation(UTFTranslationKeys.GUI_SLIDER_ALLUVIAL_FAN_SIZE)
				.range(0.5F, 2.0F)
				.activeWhen(ALLUVIAL_FANS)
				.bind((p) -> p.landforms().alluvialFans.size, (p, v) -> p.landforms().alluvialFans.size = v)
				.build()
		),
		Category.of("glacialValleys", UTFTranslationKeys.GUI_LABEL_GLACIAL_VALLEYS,
			BoolOption.builder("landforms.glacialValleys.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_GLACIAL_VALLEYS)
				.bind((p) -> p.landforms().glacialValleys.enabled, (p, v) -> p.landforms().glacialValleys.enabled = v)
				.build(),
			FloatOption.builder("landforms.glacialValleys.strength")
				.translation(UTFTranslationKeys.GUI_SLIDER_GLACIAL_VALLEY_STRENGTH)
				.range(0.2F, 1.5F)
				.activeWhen(GLACIAL_VALLEYS)
				.bind((p) -> p.landforms().glacialValleys.strength, (p, v) -> p.landforms().glacialValleys.strength = v)
				.build(),
			FloatOption.builder("landforms.glacialValleys.cirques")
				.translation(UTFTranslationKeys.GUI_SLIDER_CIRQUE_DENSITY)
				.range(0.0F, 1.0F)
				.activeWhen(GLACIAL_VALLEYS)
				.bind((p) -> p.landforms().glacialValleys.cirques, (p, v) -> p.landforms().glacialValleys.cirques = v)
				.build()
		),
		Category.of("drumlins", UTFTranslationKeys.GUI_LABEL_DRUMLINS,
			BoolOption.builder("landforms.drumlins.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_DRUMLINS)
				.bind((p) -> p.landforms().drumlins.enabled, (p, v) -> p.landforms().drumlins.enabled = v)
				.build(),
			FloatOption.builder("landforms.drumlins.density")
				.translation(UTFTranslationKeys.GUI_SLIDER_DRUMLIN_DENSITY)
				.range(0.0F, 1.0F)
				.activeWhen(DRUMLINS)
				.bind((p) -> p.landforms().drumlins.density, (p, v) -> p.landforms().drumlins.density = v)
				.build(),
			IntOption.builder("landforms.drumlins.height")
				.translation(UTFTranslationKeys.GUI_SLIDER_DRUMLIN_HEIGHT)
				.range(3, 20)
				.activeWhen(DRUMLINS)
				.bind((p) -> p.landforms().drumlins.height, (p, v) -> p.landforms().drumlins.height = v)
				.build()
		),
		Category.of("barrierIslands", UTFTranslationKeys.GUI_LABEL_BARRIER_ISLANDS,
			BoolOption.builder("landforms.barrierIslands.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_BARRIER_ISLANDS)
				.bind((p) -> p.landforms().barrierIslands.enabled, (p, v) -> p.landforms().barrierIslands.enabled = v)
				.build(),
			FloatOption.builder("landforms.barrierIslands.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_BARRIER_ISLAND_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(BARRIER_ISLANDS)
				.bind((p) -> p.landforms().barrierIslands.frequency, (p, v) -> p.landforms().barrierIslands.frequency = v)
				.build()
		),
		Category.of("karst", UTFTranslationKeys.GUI_LABEL_KARST,
			BoolOption.builder("landforms.karst.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_KARST)
				.bind((p) -> p.landforms().karst.enabled, (p, v) -> p.landforms().karst.enabled = v)
				.build(),
			FloatOption.builder("landforms.karst.coverage")
				.translation(UTFTranslationKeys.GUI_SLIDER_KARST_COVERAGE)
				.range(0.0F, 1.0F)
				.activeWhen(KARST)
				.bind((p) -> p.landforms().karst.coverage, (p, v) -> p.landforms().karst.coverage = v)
				.build(),
			IntOption.builder("landforms.karst.height")
				.translation(UTFTranslationKeys.GUI_SLIDER_KARST_HEIGHT)
				.range(10, 90)
				.activeWhen(KARST)
				.bind((p) -> p.landforms().karst.height, (p, v) -> p.landforms().karst.height = v)
				.build()
		),
		Category.of("deltas", UTFTranslationKeys.GUI_LABEL_DELTAS,
			BoolOption.builder("landforms.deltas.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_DELTAS)
				.bind((p) -> p.landforms().deltas.enabled, (p, v) -> p.landforms().deltas.enabled = v)
				.build(),
			FloatOption.builder("landforms.deltas.size")
				.translation(UTFTranslationKeys.GUI_SLIDER_DELTA_SIZE)
				.range(0.5F, 2.0F)
				.activeWhen(DELTAS)
				.bind((p) -> p.landforms().deltas.size, (p, v) -> p.landforms().deltas.size = v)
				.build()
		),
		Category.of("volcanoes", UTFTranslationKeys.GUI_LABEL_VOLCANOES,
			BoolOption.builder("landforms.volcanicSurface")
				.translation(UTFTranslationKeys.GUI_BUTTON_VOLCANIC_SURFACE)
				.bind((p) -> p.landforms().volcanicSurface, (p, v) -> p.landforms().volcanicSurface = v)
				.build()
		)
	);

	// Coasts

	private static final Predicate<Preset> HEADLANDS = (preset) -> preset.coasts().headlands.enabled;
	private static final Predicate<Preset> PENINSULAS = (preset) -> preset.coasts().peninsulas.enabled;
	private static final Predicate<Preset> COASTAL_ISLANDS = (preset) -> preset.coasts().coastalIslands.enabled;
	private static final Predicate<Preset> SPITS = (preset) -> preset.coasts().spits.enabled;
	private static final Predicate<Preset> SKERRIES = (preset) -> preset.coasts().skerries.enabled;
	private static final Predicate<Preset> ISLAND_ARCS = (preset) -> preset.coasts().islandArcs.enabled;
	private static final Predicate<Preset> RIVER_ISLANDS = (preset) -> preset.coasts().riverIslands.enabled;

	public static final Page COASTS = Page.of("coasts", UTFTranslationKeys.GUI_COASTS_SETTINGS_TITLE,
		Category.of("headlands", UTFTranslationKeys.GUI_LABEL_HEADLANDS,
			BoolOption.builder("coasts.headlands.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_HEADLANDS)
				.bind((p) -> p.coasts().headlands.enabled, (p, v) -> p.coasts().headlands.enabled = v)
				.build(),
			FloatOption.builder("coasts.headlands.strength")
				.translation(UTFTranslationKeys.GUI_SLIDER_HEADLAND_STRENGTH)
				.range(0.0F, 2.0F)
				.activeWhen(HEADLANDS)
				.bind((p) -> p.coasts().headlands.strength, (p, v) -> p.coasts().headlands.strength = v)
				.build()
		),
		Category.of("peninsulas", UTFTranslationKeys.GUI_LABEL_PENINSULAS,
			BoolOption.builder("coasts.peninsulas.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_PENINSULAS)
				.bind((p) -> p.coasts().peninsulas.enabled, (p, v) -> p.coasts().peninsulas.enabled = v)
				.build(),
			FloatOption.builder("coasts.peninsulas.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_PENINSULA_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(PENINSULAS)
				.bind((p) -> p.coasts().peninsulas.frequency, (p, v) -> p.coasts().peninsulas.frequency = v)
				.build(),
			FloatOption.builder("coasts.peninsulas.size")
				.translation(UTFTranslationKeys.GUI_SLIDER_PENINSULA_SIZE)
				.range(0.4F, 2.0F)
				.activeWhen(PENINSULAS)
				.bind((p) -> p.coasts().peninsulas.size, (p, v) -> p.coasts().peninsulas.size = v)
				.build()
		),
		Category.of("coastalIslands", UTFTranslationKeys.GUI_LABEL_COASTAL_ISLANDS,
			BoolOption.builder("coasts.coastalIslands.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_COASTAL_ISLANDS)
				.bind((p) -> p.coasts().coastalIslands.enabled, (p, v) -> p.coasts().coastalIslands.enabled = v)
				.build(),
			FloatOption.builder("coasts.coastalIslands.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_COASTAL_ISLAND_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(COASTAL_ISLANDS)
				.bind((p) -> p.coasts().coastalIslands.frequency, (p, v) -> p.coasts().coastalIslands.frequency = v)
				.build(),
			FloatOption.builder("coasts.coastalIslands.size")
				.translation(UTFTranslationKeys.GUI_SLIDER_COASTAL_ISLAND_SIZE)
				.range(0.4F, 2.0F)
				.activeWhen(COASTAL_ISLANDS)
				.bind((p) -> p.coasts().coastalIslands.size, (p, v) -> p.coasts().coastalIslands.size = v)
				.build(),
			FloatOption.builder("coasts.coastalIslands.tombolos")
				.translation(UTFTranslationKeys.GUI_SLIDER_TOMBOLO_SHARE)
				.range(0.0F, 1.0F)
				.activeWhen(COASTAL_ISLANDS)
				.bind((p) -> p.coasts().coastalIslands.tombolos, (p, v) -> p.coasts().coastalIslands.tombolos = v)
				.build()
		),
		Category.of("spits", UTFTranslationKeys.GUI_LABEL_SPITS,
			BoolOption.builder("coasts.spits.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_SPITS)
				.bind((p) -> p.coasts().spits.enabled, (p, v) -> p.coasts().spits.enabled = v)
				.build(),
			FloatOption.builder("coasts.spits.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_SPIT_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(SPITS)
				.bind((p) -> p.coasts().spits.frequency, (p, v) -> p.coasts().spits.frequency = v)
				.build()
		),
		Category.of("skerries", UTFTranslationKeys.GUI_LABEL_SKERRIES,
			BoolOption.builder("coasts.skerries.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_SKERRIES)
				.bind((p) -> p.coasts().skerries.enabled, (p, v) -> p.coasts().skerries.enabled = v)
				.build(),
			FloatOption.builder("coasts.skerries.density")
				.translation(UTFTranslationKeys.GUI_SLIDER_SKERRY_DENSITY)
				.range(0.0F, 1.0F)
				.activeWhen(SKERRIES)
				.bind((p) -> p.coasts().skerries.density, (p, v) -> p.coasts().skerries.density = v)
				.build()
		),
		Category.of("islandArcs", UTFTranslationKeys.GUI_LABEL_ISLAND_ARCS,
			BoolOption.builder("coasts.islandArcs.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_ISLAND_ARCS)
				.bind((p) -> p.coasts().islandArcs.enabled, (p, v) -> p.coasts().islandArcs.enabled = v)
				.build(),
			FloatOption.builder("coasts.islandArcs.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_ISLAND_ARC_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(ISLAND_ARCS)
				.bind((p) -> p.coasts().islandArcs.frequency, (p, v) -> p.coasts().islandArcs.frequency = v)
				.build()
		),
		Category.of("riverIslands", UTFTranslationKeys.GUI_LABEL_RIVER_ISLANDS,
			BoolOption.builder("coasts.riverIslands.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_RIVER_ISLANDS)
				.bind((p) -> p.coasts().riverIslands.enabled, (p, v) -> p.coasts().riverIslands.enabled = v)
				.build(),
			FloatOption.builder("coasts.riverIslands.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_RIVER_ISLAND_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(RIVER_ISLANDS)
				.bind((p) -> p.coasts().riverIslands.frequency, (p, v) -> p.coasts().riverIslands.frequency = v)
				.build()
		)
	);

	// Cave Features

	private static final Predicate<Preset> UNDERGROUND_RIVERS = (preset) -> preset.caveFeatures().undergroundRivers.enabled;
	private static final Predicate<Preset> SPRINGS = (preset) -> preset.caveFeatures().springs.enabled;
	private static final Predicate<Preset> KARST_CAVES = (preset) -> preset.caveFeatures().karstCaves.enabled;
	private static final Predicate<Preset> LAVA_TUBES = (preset) -> preset.caveFeatures().lavaTubes.enabled;
	private static final Predicate<Preset> SEA_CAVES = (preset) -> preset.caveFeatures().seaCaves.enabled;
	private static final Predicate<Preset> LAYER_CAVES = (preset) -> preset.caveFeatures().layerCaves.enabled;
	private static final Predicate<Preset> GIANT_CAVERNS = (preset) -> preset.caveFeatures().giantCaverns.enabled;
	private static final Predicate<Preset> ROCK_SHELTERS = (preset) -> preset.caveFeatures().rockShelters.enabled;
	private static final Predicate<Preset> GLACIER_CAVES = (preset) -> preset.caveFeatures().glacierCaves.enabled;
	private static final Predicate<Preset> CAVE_MOUTHS = (preset) -> preset.caveFeatures().caveMouths.enabled;

	public static final Page CAVE_FEATURES = Page.of("caveFeatures", UTFTranslationKeys.GUI_CAVE_FEATURES_SETTINGS_TITLE,
		Category.of("undergroundRivers", UTFTranslationKeys.GUI_LABEL_UNDERGROUND_RIVERS,
			BoolOption.builder("caveFeatures.undergroundRivers.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_UNDERGROUND_RIVERS)
				.bind((p) -> p.caveFeatures().undergroundRivers.enabled, (p, v) -> p.caveFeatures().undergroundRivers.enabled = v)
				.build(),
			FloatOption.builder("caveFeatures.undergroundRivers.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_UNDERGROUND_RIVER_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(UNDERGROUND_RIVERS)
				.bind((p) -> p.caveFeatures().undergroundRivers.frequency, (p, v) -> p.caveFeatures().undergroundRivers.frequency = v)
				.build()
		),
		Category.of("springs", UTFTranslationKeys.GUI_LABEL_SPRINGS,
			BoolOption.builder("caveFeatures.springs.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_SPRINGS)
				.bind((p) -> p.caveFeatures().springs.enabled, (p, v) -> p.caveFeatures().springs.enabled = v)
				.build(),
			FloatOption.builder("caveFeatures.springs.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_SPRING_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(SPRINGS)
				.bind((p) -> p.caveFeatures().springs.frequency, (p, v) -> p.caveFeatures().springs.frequency = v)
				.build()
		),
		Category.of("karstCaves", UTFTranslationKeys.GUI_LABEL_KARST_CAVES,
			BoolOption.builder("caveFeatures.karstCaves.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_KARST_CAVES)
				.bind((p) -> p.caveFeatures().karstCaves.enabled, (p, v) -> p.caveFeatures().karstCaves.enabled = v)
				.build(),
			FloatOption.builder("caveFeatures.karstCaves.size")
				.translation(UTFTranslationKeys.GUI_SLIDER_KARST_CAVE_SIZE)
				.range(0.5F, 2.0F)
				.activeWhen(KARST_CAVES)
				.bind((p) -> p.caveFeatures().karstCaves.size, (p, v) -> p.caveFeatures().karstCaves.size = v)
				.build()
		),
		Category.of("lavaTubes", UTFTranslationKeys.GUI_LABEL_LAVA_TUBES,
			BoolOption.builder("caveFeatures.lavaTubes.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_LAVA_TUBES)
				.bind((p) -> p.caveFeatures().lavaTubes.enabled, (p, v) -> p.caveFeatures().lavaTubes.enabled = v)
				.build(),
			FloatOption.builder("caveFeatures.lavaTubes.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_LAVA_TUBE_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(LAVA_TUBES)
				.bind((p) -> p.caveFeatures().lavaTubes.frequency, (p, v) -> p.caveFeatures().lavaTubes.frequency = v)
				.build()
		),
		Category.of("seaCaves", UTFTranslationKeys.GUI_LABEL_SEA_CAVES,
			BoolOption.builder("caveFeatures.seaCaves.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_SEA_CAVES)
				.bind((p) -> p.caveFeatures().seaCaves.enabled, (p, v) -> p.caveFeatures().seaCaves.enabled = v)
				.build(),
			FloatOption.builder("caveFeatures.seaCaves.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_SEA_CAVE_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(SEA_CAVES)
				.bind((p) -> p.caveFeatures().seaCaves.frequency, (p, v) -> p.caveFeatures().seaCaves.frequency = v)
				.build()
		),
		Category.of("layerCaves", UTFTranslationKeys.GUI_LABEL_LAYER_CAVES,
			BoolOption.builder("caveFeatures.layerCaves.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_LAYER_CAVES)
				.bind((p) -> p.caveFeatures().layerCaves.enabled, (p, v) -> p.caveFeatures().layerCaves.enabled = v)
				.build(),
			FloatOption.builder("caveFeatures.layerCaves.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_LAYER_CAVE_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(LAYER_CAVES)
				.bind((p) -> p.caveFeatures().layerCaves.frequency, (p, v) -> p.caveFeatures().layerCaves.frequency = v)
				.build()
		),
		Category.of("giantCaverns", UTFTranslationKeys.GUI_LABEL_GIANT_CAVERNS,
			BoolOption.builder("caveFeatures.giantCaverns.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_GIANT_CAVERNS)
				.bind((p) -> p.caveFeatures().giantCaverns.enabled, (p, v) -> p.caveFeatures().giantCaverns.enabled = v)
				.build(),
			FloatOption.builder("caveFeatures.giantCaverns.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_GIANT_CAVERN_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(GIANT_CAVERNS)
				.bind((p) -> p.caveFeatures().giantCaverns.frequency, (p, v) -> p.caveFeatures().giantCaverns.frequency = v)
				.build()
		),
		Category.of("rockShelters", UTFTranslationKeys.GUI_LABEL_ROCK_SHELTERS,
			BoolOption.builder("caveFeatures.rockShelters.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_ROCK_SHELTERS)
				.bind((p) -> p.caveFeatures().rockShelters.enabled, (p, v) -> p.caveFeatures().rockShelters.enabled = v)
				.build(),
			FloatOption.builder("caveFeatures.rockShelters.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_ROCK_SHELTER_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(ROCK_SHELTERS)
				.bind((p) -> p.caveFeatures().rockShelters.frequency, (p, v) -> p.caveFeatures().rockShelters.frequency = v)
				.build()
		),
		Category.of("glacierCaves", UTFTranslationKeys.GUI_LABEL_GLACIER_CAVES,
			BoolOption.builder("caveFeatures.glacierCaves.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_GLACIER_CAVES)
				.bind((p) -> p.caveFeatures().glacierCaves.enabled, (p, v) -> p.caveFeatures().glacierCaves.enabled = v)
				.build(),
			FloatOption.builder("caveFeatures.glacierCaves.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_GLACIER_CAVE_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(GLACIER_CAVES)
				.bind((p) -> p.caveFeatures().glacierCaves.frequency, (p, v) -> p.caveFeatures().glacierCaves.frequency = v)
				.build()
		),
		Category.of("caveMouths", UTFTranslationKeys.GUI_LABEL_CAVE_MOUTHS,
			BoolOption.builder("caveFeatures.caveMouths.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_CAVE_MOUTHS)
				.bind((p) -> p.caveFeatures().caveMouths.enabled, (p, v) -> p.caveFeatures().caveMouths.enabled = v)
				.build(),
			FloatOption.builder("caveFeatures.caveMouths.strength")
				.translation(UTFTranslationKeys.GUI_SLIDER_CAVE_MOUTH_STRENGTH)
				.range(0.0F, 2.0F)
				.activeWhen(CAVE_MOUTHS)
				.bind((p) -> p.caveFeatures().caveMouths.strength, (p, v) -> p.caveFeatures().caveMouths.strength = v)
				.build()
		)
	);

	// Oceans

	private static final Predicate<Preset> SHELVES = (preset) -> preset.oceans().shelves.enabled;
	private static final Predicate<Preset> SUBMARINE_CANYONS = (preset) -> preset.oceans().submarineCanyons.enabled;
	private static final Predicate<Preset> TRENCHES = (preset) -> preset.oceans().trenches.enabled;
	private static final Predicate<Preset> SEAMOUNTS = (preset) -> preset.oceans().seamounts.enabled;
	private static final Predicate<Preset> RIDGES = (preset) -> preset.oceans().ridges.enabled;
	private static final Predicate<Preset> BLUE_HOLES = (preset) -> preset.oceans().blueHoles.enabled;
	private static final Predicate<Preset> CORAL_REEFS = (preset) -> preset.oceans().coralReefs.enabled;
	private static final Predicate<Preset> SAND_WAVES = (preset) -> preset.oceans().sandWaves.enabled;
	private static final Predicate<Preset> WHALE_FALLS = (preset) -> preset.oceans().whaleFalls.enabled;

	public static final Page OCEANS = Page.of("oceans", UTFTranslationKeys.GUI_OCEANS_SETTINGS_TITLE,
		Category.of("sediment", UTFTranslationKeys.GUI_LABEL_SEA_FLOOR_SEDIMENT,
			BoolOption.builder("oceans.sediment")
				.translation(UTFTranslationKeys.GUI_BUTTON_SEA_FLOOR_SEDIMENT)
				.bind((p) -> p.oceans().sediment, (p, v) -> p.oceans().sediment = v)
				.build()
		),
		Category.of("shelves", UTFTranslationKeys.GUI_LABEL_SHELVES,
			BoolOption.builder("oceans.shelves.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_SHELVES)
				.bind((p) -> p.oceans().shelves.enabled, (p, v) -> p.oceans().shelves.enabled = v)
				.build(),
			FloatOption.builder("oceans.shelves.width")
				.translation(UTFTranslationKeys.GUI_SLIDER_SHELF_WIDTH)
				.range(0.3F, 2.0F)
				.activeWhen(SHELVES)
				.bind((p) -> p.oceans().shelves.width, (p, v) -> p.oceans().shelves.width = v)
				.build()
		),
		Category.of("submarineCanyons", UTFTranslationKeys.GUI_LABEL_SUBMARINE_CANYONS,
			BoolOption.builder("oceans.submarineCanyons.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_SUBMARINE_CANYONS)
				.bind((p) -> p.oceans().submarineCanyons.enabled, (p, v) -> p.oceans().submarineCanyons.enabled = v)
				.build(),
			FloatOption.builder("oceans.submarineCanyons.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_SUBMARINE_CANYON_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(SUBMARINE_CANYONS)
				.bind((p) -> p.oceans().submarineCanyons.frequency, (p, v) -> p.oceans().submarineCanyons.frequency = v)
				.build()
		),
		Category.of("trenches", UTFTranslationKeys.GUI_LABEL_TRENCHES,
			BoolOption.builder("oceans.trenches.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_TRENCHES)
				.bind((p) -> p.oceans().trenches.enabled, (p, v) -> p.oceans().trenches.enabled = v)
				.build(),
			FloatOption.builder("oceans.trenches.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_TRENCH_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(TRENCHES)
				.bind((p) -> p.oceans().trenches.frequency, (p, v) -> p.oceans().trenches.frequency = v)
				.build(),
			IntOption.builder("oceans.trenches.depth")
				.translation(UTFTranslationKeys.GUI_SLIDER_TRENCH_DEPTH)
				.range(15, 80)
				.activeWhen(TRENCHES)
				.bind((p) -> p.oceans().trenches.depth, (p, v) -> p.oceans().trenches.depth = v)
				.build()
		),
		Category.of("seamounts", UTFTranslationKeys.GUI_LABEL_SEAMOUNTS,
			BoolOption.builder("oceans.seamounts.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_SEAMOUNTS)
				.bind((p) -> p.oceans().seamounts.enabled, (p, v) -> p.oceans().seamounts.enabled = v)
				.build(),
			FloatOption.builder("oceans.seamounts.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_SEAMOUNT_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(SEAMOUNTS)
				.bind((p) -> p.oceans().seamounts.frequency, (p, v) -> p.oceans().seamounts.frequency = v)
				.build()
		),
		Category.of("ridges", UTFTranslationKeys.GUI_LABEL_RIDGES,
			BoolOption.builder("oceans.ridges.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_RIDGES)
				.bind((p) -> p.oceans().ridges.enabled, (p, v) -> p.oceans().ridges.enabled = v)
				.build(),
			FloatOption.builder("oceans.ridges.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_RIDGE_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(RIDGES)
				.bind((p) -> p.oceans().ridges.frequency, (p, v) -> p.oceans().ridges.frequency = v)
				.build()
		),
		Category.of("blueHoles", UTFTranslationKeys.GUI_LABEL_BLUE_HOLES,
			BoolOption.builder("oceans.blueHoles.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_BLUE_HOLES)
				.bind((p) -> p.oceans().blueHoles.enabled, (p, v) -> p.oceans().blueHoles.enabled = v)
				.build(),
			FloatOption.builder("oceans.blueHoles.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_BLUE_HOLE_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(BLUE_HOLES)
				.bind((p) -> p.oceans().blueHoles.frequency, (p, v) -> p.oceans().blueHoles.frequency = v)
				.build()
		),
		Category.of("coralReefs", UTFTranslationKeys.GUI_LABEL_CORAL_REEFS,
			BoolOption.builder("oceans.coralReefs.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_CORAL_REEFS)
				.bind((p) -> p.oceans().coralReefs.enabled, (p, v) -> p.oceans().coralReefs.enabled = v)
				.build(),
			FloatOption.builder("oceans.coralReefs.coverage")
				.translation(UTFTranslationKeys.GUI_SLIDER_CORAL_REEF_COVERAGE)
				.range(0.0F, 1.0F)
				.activeWhen(CORAL_REEFS)
				.bind((p) -> p.oceans().coralReefs.coverage, (p, v) -> p.oceans().coralReefs.coverage = v)
				.build()
		),
		Category.of("sandWaves", UTFTranslationKeys.GUI_LABEL_SAND_WAVES,
			BoolOption.builder("oceans.sandWaves.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_SAND_WAVES)
				.bind((p) -> p.oceans().sandWaves.enabled, (p, v) -> p.oceans().sandWaves.enabled = v)
				.build(),
			IntOption.builder("oceans.sandWaves.height")
				.translation(UTFTranslationKeys.GUI_SLIDER_SAND_WAVE_HEIGHT)
				.range(1, 5)
				.activeWhen(SAND_WAVES)
				.bind((p) -> p.oceans().sandWaves.height, (p, v) -> p.oceans().sandWaves.height = v)
				.build()
		),
		Category.of("whaleFalls", UTFTranslationKeys.GUI_LABEL_WHALE_FALLS,
			BoolOption.builder("oceans.whaleFalls.enabled")
				.translation(UTFTranslationKeys.GUI_BUTTON_WHALE_FALLS)
				.bind((p) -> p.oceans().whaleFalls.enabled, (p, v) -> p.oceans().whaleFalls.enabled = v)
				.build(),
			FloatOption.builder("oceans.whaleFalls.frequency")
				.translation(UTFTranslationKeys.GUI_SLIDER_WHALE_FALL_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(WHALE_FALLS)
				.bind((p) -> p.oceans().whaleFalls.frequency, (p, v) -> p.oceans().whaleFalls.frequency = v)
				.build()
		)
	);

	public static final List<Page> PAGES = List.of(WORLD, SURFACE, CAVES, CAVE_FEATURES, CLIMATE, TERRAIN, RIVERS, LANDFORMS, COASTS, OCEANS, FILTERS, MISCELLANEOUS);

	private static final Map<String, Option<?>> BY_PATH = PAGES.stream().flatMap(Page::options).collect(Collectors.toUnmodifiableMap(Option::path, Function.identity()));

	// the few settings shown directly in the Create World screen, most noticeable first, with labels that make sense
	// without their page and category
	public record SimpleSetting(Option<?> option, String labelKey) {
	}

	public static final List<SimpleSetting> SIMPLE = List.of(
		simple("world.continent.continentScale", UTFTranslationKeys.SIMPLE_CONTINENT_SIZE),
		simple("climate.biomeShape.biomeSize", UTFTranslationKeys.SIMPLE_BIOME_SIZE),
		simple("terrain.general.terrainRegionSize", UTFTranslationKeys.SIMPLE_TERRAIN_AREA_SIZE),
		simple("terrain.mountains.weight", UTFTranslationKeys.SIMPLE_MOUNTAINS),
		simple("rivers.riverCount", UTFTranslationKeys.SIMPLE_RIVERS),
		simple("rivers.lakes.chance", UTFTranslationKeys.SIMPLE_LAKES),
		simple("world.properties.seaLevel", UTFTranslationKeys.SIMPLE_SEA_LEVEL),
		simple("world.properties.worldHeight", UTFTranslationKeys.SIMPLE_WORLD_HEIGHT),
		simple("caves.cheeseCaveProbability", UTFTranslationKeys.SIMPLE_CAVES)
	);

	private static SimpleSetting simple(String path, String labelKey) {
		return new SimpleSetting(byPath(path).orElseThrow(() -> new IllegalStateException("No option " + path)), labelKey);
	}

	public static Stream<Option<?>> all() {
		return PAGES.stream().flatMap(Page::options);
	}

	public static Optional<Option<?>> byPath(String path) {
		return Optional.ofNullable(BY_PATH.get(path));
	}

	private static FloatOption caveChance(String name, String translationKey, Function<Preset, Float> getter, BiConsumer<Preset, Float> setter) {
		return FloatOption.builder("caves." + name)
			.translation(translationKey)
			.range(0.0F, 1.0F)
			.bind(getter, setter)
			.build();
	}

	private static Category climateRange(String id, String label, Function<Preset, ClimateSettings.RangeValue> range, String scaleKey, String falloffKey, String minKey, String maxKey, String biasKey) {
		String path = "climate." + id + ".";
		return Category.of(id, label,
			IntOption.builder(path + "seedOffset")
				.translation(UTFTranslationKeys.GUI_BUTTON_CLIMATE_SEED_OFFSET)
				.seed()
				.bind((p) -> range.apply(p).seedOffset, (p, v) -> range.apply(p).seedOffset = v)
				.build(),
			IntOption.builder(path + "scale")
				.translation(scaleKey)
				.range(1, 20)
				.bind((p) -> range.apply(p).scale, (p, v) -> range.apply(p).scale = v)
				.build(),
			IntOption.builder(path + "falloff")
				.translation(falloffKey)
				.range(1, 10)
				.bind((p) -> range.apply(p).falloff, (p, v) -> range.apply(p).falloff = v)
				.build(),
			FloatOption.builder(path + "min")
				.translation(minKey)
				.range(0.0F, 1.0F)
				.bind((p) -> range.apply(p).min, (p, v) -> range.apply(p).min = v)
				.build(),
			FloatOption.builder(path + "max")
				.translation(maxKey)
				.range(0.0F, 1.0F)
				.bind((p) -> range.apply(p).max, (p, v) -> range.apply(p).max = v)
				.build(),
			FloatOption.builder(path + "bias")
				.translation(biasKey)
				.range(-1.0F, 1.0F)
				.bind((p) -> range.apply(p).bias, (p, v) -> range.apply(p).bias = v)
				.build()
		);
	}

	// some terrain types' generators ignore their scales; those options are hidden rather than shown doing nothing
	private static Category terrainType(String id, String label, Function<Preset, TerrainSettings.Terrain> terrain, boolean usesHorizontalScale, boolean usesBaseAndVerticalScale) {
		String path = "terrain." + id + ".";
		return Category.of(id, label,
			FloatOption.builder(path + "weight")
				.translation(UTFTranslationKeys.GUI_SLIDER_TERRAIN_WEIGHT)
				.range(0.0F, 10.0F)
				.bind((p) -> terrain.apply(p).weight, (p, v) -> terrain.apply(p).weight = v)
				.build(),
			hiddenUnless(usesBaseAndVerticalScale, FloatOption.builder(path + "baseScale")
				.translation(UTFTranslationKeys.GUI_SLIDER_TERRAIN_BASE_SCALE)
				.range(0.0F, 2.0F)
				.bind((p) -> terrain.apply(p).baseScale, (p, v) -> terrain.apply(p).baseScale = v)
			).build(),
			hiddenUnless(usesBaseAndVerticalScale, FloatOption.builder(path + "verticalScale")
				.translation(UTFTranslationKeys.GUI_SLIDER_TERRAIN_VERTICAL_SCALE)
				.range(0.0F, 10.0F)
				.bind((p) -> terrain.apply(p).verticalScale, (p, v) -> terrain.apply(p).verticalScale = v)
			).build(),
			hiddenUnless(usesHorizontalScale, FloatOption.builder(path + "horizontalScale")
				.translation(UTFTranslationKeys.GUI_SLIDER_TERRAIN_HORIZONTAL_SCALE)
				.range(0.0F, 10.0F)
				.bind((p) -> terrain.apply(p).horizontalScale, (p, v) -> terrain.apply(p).horizontalScale = v)
			).build()
		);
	}

	private static FloatOption.Builder hiddenUnless(boolean visible, FloatOption.Builder builder) {
		return visible ? builder : builder.hidden();
	}

	private static Category river(String id, String label, Function<Preset, RiverSettings.River> river, int maxBedDepth, int maxBedWidth, int maxBankWidth) {
		String path = "rivers." + id + ".";
		return Category.of(id, label,
			IntOption.builder(path + "bedDepth")
				.translation(UTFTranslationKeys.GUI_SLIDER_RIVER_BED_DEPTH)
				.range(1, maxBedDepth)
				.bind((p) -> river.apply(p).bedDepth, (p, v) -> river.apply(p).bedDepth = v)
				.build(),
			// river bank heights are kept in the file but the river carver doesn't use them at the moment
			IntOption.builder(path + "minBankHeight")
				.translation(UTFTranslationKeys.GUI_SLIDER_RIVER_MIN_BANK_HEIGHT)
				.range(0, 20)
				.atMost((p) -> river.apply(p).maxBankHeight)
				.bind((p) -> river.apply(p).minBankHeight, (p, v) -> river.apply(p).minBankHeight = v)
				.hidden()
				.build(),
			IntOption.builder(path + "maxBankHeight")
				.translation(UTFTranslationKeys.GUI_SLIDER_RIVER_MAX_BANK_HEIGHT)
				.range(1, 20)
				.atLeast((p) -> river.apply(p).minBankHeight)
				.bind((p) -> river.apply(p).maxBankHeight, (p, v) -> river.apply(p).maxBankHeight = v)
				.hidden()
				.build(),
			IntOption.builder(path + "bedWidth")
				.translation(UTFTranslationKeys.GUI_SLIDER_RIVER_BED_WIDTH)
				.range(1, maxBedWidth)
				.bind((p) -> river.apply(p).bedWidth, (p, v) -> river.apply(p).bedWidth = v)
				.build(),
			IntOption.builder(path + "bankWidth")
				.translation(UTFTranslationKeys.GUI_SLIDER_RIVER_BANK_WIDTH)
				.range(1, maxBankWidth)
				.bind((p) -> river.apply(p).bankWidth, (p, v) -> river.apply(p).bankWidth = v)
				.build(),
			FloatOption.builder(path + "fade")
				.translation(UTFTranslationKeys.GUI_SLIDER_RIVER_FADE)
				.range(0.0F, 1.0F)
				.bind((p) -> river.apply(p).fade, (p, v) -> river.apply(p).fade = v)
				.build()
		);
	}

	private PresetOptions() {
	}
}
