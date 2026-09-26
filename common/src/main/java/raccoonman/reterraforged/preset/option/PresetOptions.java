package raccoonman.reterraforged.preset.option;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.data.preset.settings.ClimateSettings;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.data.preset.settings.RiverSettings;
import raccoonman.reterraforged.data.preset.settings.TerrainSettings;
import raccoonman.reterraforged.world.worldgen.biome.spawn.SpawnType;
import raccoonman.reterraforged.world.worldgen.continent.ContinentType;
import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;

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
		.translation(RTFTranslationKeys.GUI_BUTTON_CONTINENT_TYPE)
		.values(List.of(ContinentType.MULTI, ContinentType.SINGLE, ContinentType.MULTI_IMPROVED, ContinentType.EXPERIMENTAL))
		.bind((p) -> p.world().continent.continentType, (p, v) -> p.world().continent.continentType = v)
		.build();

	public static final EnumOption<DistanceFunction> CONTINENT_SHAPE = EnumOption.<DistanceFunction>builder("world.continent.continentShape")
		.translation(RTFTranslationKeys.GUI_BUTTON_CONTINENT_SHAPE)
		.values(List.of(DistanceFunction.values()))
		.bind((p) -> p.world().continent.continentShape, (p, v) -> p.world().continent.continentShape = v)
		.activeWhen(CLASSIC_CONTINENTS)
		.build();

	public static final IntOption CONTINENT_SCALE = IntOption.builder("world.continent.continentScale")
		.translation(RTFTranslationKeys.GUI_SLIDER_CONTINENT_SCALE)
		.range(100, 10000)
		.bind((p) -> p.world().continent.continentScale, (p, v) -> p.world().continent.continentScale = v)
		.build();

	public static final FloatOption CONTINENT_JITTER = FloatOption.builder("world.continent.continentJitter")
		.translation(RTFTranslationKeys.GUI_SLIDER_CONTINENT_JITTER)
		.range(0.5F, 1.0F)
		.bind((p) -> p.world().continent.continentJitter, (p, v) -> p.world().continent.continentJitter = v)
		.build();

	public static final FloatOption CONTINENT_SKIPPING = FloatOption.builder("world.continent.continentSkipping")
		.translation(RTFTranslationKeys.GUI_SLIDER_CONTINENT_SKIPPING)
		.range(0.0F, 1.0F)
		.bind((p) -> p.world().continent.continentSkipping, (p, v) -> p.world().continent.continentSkipping = v)
		.activeWhen(IMPROVED_CONTINENTS)
		.build();

	public static final FloatOption CONTINENT_SIZE_VARIANCE = FloatOption.builder("world.continent.continentSizeVariance")
		.translation(RTFTranslationKeys.GUI_SLIDER_CONTINENT_SIZE_VARIANCE)
		.range(0.0F, 0.75F)
		.bind((p) -> p.world().continent.continentSizeVariance, (p, v) -> p.world().continent.continentSizeVariance = v)
		.activeWhen(IMPROVED_CONTINENTS)
		.build();

	public static final IntOption CONTINENT_NOISE_OCTAVES = IntOption.builder("world.continent.continentNoiseOctaves")
		.translation(RTFTranslationKeys.GUI_SLIDER_CONTINENT_NOISE_OCTAVES)
		.range(1, 5)
		.bind((p) -> p.world().continent.continentNoiseOctaves, (p, v) -> p.world().continent.continentNoiseOctaves = v)
		.activeWhen(IMPROVED_CONTINENTS)
		.build();

	public static final FloatOption CONTINENT_NOISE_GAIN = FloatOption.builder("world.continent.continentNoiseGain")
		.translation(RTFTranslationKeys.GUI_SLIDER_CONTINENT_NOISE_GAIN)
		.range(0.0F, 0.5F)
		.bind((p) -> p.world().continent.continentNoiseGain, (p, v) -> p.world().continent.continentNoiseGain = v)
		.activeWhen(IMPROVED_CONTINENTS)
		.build();

	public static final FloatOption CONTINENT_NOISE_LACUNARITY = FloatOption.builder("world.continent.continentNoiseLacunarity")
		.translation(RTFTranslationKeys.GUI_SLIDER_CONTINENT_NOISE_LACUNARITY)
		.range(1.0F, 10.0F)
		.bind((p) -> p.world().continent.continentNoiseLacunarity, (p, v) -> p.world().continent.continentNoiseLacunarity = v)
		.activeWhen(IMPROVED_CONTINENTS)
		.build();

	// island control points are kept in the file but the current generator doesn't use them; legacy presets use -1 for no islands
	public static final FloatOption ISLAND_INLAND = FloatOption.builder("world.controlPoints.islandInland")
		.translation(RTFTranslationKeys.GUI_SLIDER_ISLAND_INLAND)
		.range(-1.0F, 1.0F)
		.atMost((p) -> p.world().controlPoints.islandCoast)
		.bind((p) -> p.world().controlPoints.islandInland, (p, v) -> p.world().controlPoints.islandInland = v)
		.hidden()
		.build();

	public static final FloatOption ISLAND_COAST = FloatOption.builder("world.controlPoints.islandCoast")
		.translation(RTFTranslationKeys.GUI_SLIDER_ISLAND_COAST)
		.range(-1.0F, 1.0F)
		.atLeast((p) -> p.world().controlPoints.islandInland)
		.bind((p) -> p.world().controlPoints.islandCoast, (p, v) -> p.world().controlPoints.islandCoast = v)
		.hidden()
		.build();

	// ocean to inland transition points must stay in order
	public static final FloatOption DEEP_OCEAN = FloatOption.builder("world.controlPoints.deepOcean")
		.translation(RTFTranslationKeys.GUI_SLIDER_DEEP_OCEAN)
		.range(0.0F, 1.0F)
		.atMost((p) -> p.world().controlPoints.shallowOcean)
		.bind((p) -> p.world().controlPoints.deepOcean, (p, v) -> p.world().controlPoints.deepOcean = v)
		.build();

	public static final FloatOption SHALLOW_OCEAN = FloatOption.builder("world.controlPoints.shallowOcean")
		.translation(RTFTranslationKeys.GUI_SLIDER_SHALLOW_OCEAN)
		.range(0.0F, 1.0F)
		.atLeast((p) -> p.world().controlPoints.deepOcean)
		.atMost((p) -> p.world().controlPoints.beach)
		.bind((p) -> p.world().controlPoints.shallowOcean, (p, v) -> p.world().controlPoints.shallowOcean = v)
		.build();

	public static final FloatOption BEACH = FloatOption.builder("world.controlPoints.beach")
		.translation(RTFTranslationKeys.GUI_SLIDER_BEACH)
		.range(0.0F, 1.0F)
		.atLeast((p) -> p.world().controlPoints.shallowOcean)
		.atMost((p) -> p.world().controlPoints.coast)
		.bind((p) -> p.world().controlPoints.beach, (p, v) -> p.world().controlPoints.beach = v)
		.build();

	public static final FloatOption COAST = FloatOption.builder("world.controlPoints.coast")
		.translation(RTFTranslationKeys.GUI_SLIDER_COAST)
		.range(0.0F, 1.0F)
		.atLeast((p) -> p.world().controlPoints.beach)
		.atMost((p) -> p.world().controlPoints.nearInland)
		.bind((p) -> p.world().controlPoints.coast, (p, v) -> p.world().controlPoints.coast = v)
		.build();

	public static final FloatOption NEAR_INLAND = FloatOption.builder("world.controlPoints.inland")
		.translation(RTFTranslationKeys.GUI_SLIDER_NEAR_INLAND)
		.range(0.0F, 5.0F)
		.atLeast((p) -> p.world().controlPoints.coast)
		.bind((p) -> p.world().controlPoints.nearInland, (p, v) -> p.world().controlPoints.nearInland = v)
		.build();

	public static final FloatOption MID_INLAND = FloatOption.builder("world.controlPoints.midInland")
		.translation(RTFTranslationKeys.GUI_SLIDER_MID_INLAND)
		.range(0.0F, 5.0F)
		.bind((p) -> p.world().controlPoints.midInland, (p, v) -> p.world().controlPoints.midInland = v)
		.build();

	public static final FloatOption FAR_INLAND = FloatOption.builder("world.controlPoints.farInland")
		.translation(RTFTranslationKeys.GUI_SLIDER_FAR_INLAND)
		.range(0.0F, 5.0F)
		.bind((p) -> p.world().controlPoints.farInland, (p, v) -> p.world().controlPoints.farInland = v)
		.build();

	public static final EnumOption<SpawnType> SPAWN_TYPE = EnumOption.<SpawnType>builder("world.properties.spawnType")
		.translation(RTFTranslationKeys.GUI_BUTTON_SPAWN_TYPE)
		.values(List.of(SpawnType.values()))
		.bind((p) -> p.world().properties.spawnType, (p, v) -> p.world().properties.spawnType = v)
		.build();

	public static final IntOption WORLD_HEIGHT = IntOption.builder("world.properties.worldHeight")
		.translation(RTFTranslationKeys.GUI_SLIDER_WORLD_HEIGHT)
		.range(16, 1024)
		.step(16)
		.tag(OptionTag.MEDIUM_PERFORMANCE_IMPACT)
		.bind((p) -> p.world().properties.worldHeight, (p, v) -> p.world().properties.worldHeight = v)
		.build();

	public static final IntOption WORLD_DEPTH = IntOption.builder("world.properties.worldDepth")
		.translation(RTFTranslationKeys.GUI_SLIDER_WORLD_DEPTH)
		.range(0, 1024)
		.step(16)
		.tag(OptionTag.MEDIUM_PERFORMANCE_IMPACT)
		.bind((p) -> p.world().properties.worldDepth, (p, v) -> p.world().properties.worldDepth = v)
		.build();

	public static final IntOption SEA_LEVEL = IntOption.builder("world.properties.seaLevel")
		.translation(RTFTranslationKeys.GUI_SLIDER_SEA_LEVEL)
		.range(0, 255)
		.bind((p) -> p.world().properties.seaLevel, (p, v) -> p.world().properties.seaLevel = v)
		.build();

	public static final IntOption LAVA_LEVEL = IntOption.builder("world.properties.lavaLevel")
		.translation(RTFTranslationKeys.GUI_SLIDER_LAVA_LEVEL)
		.range(-1024, 1024)
		.bind((p) -> p.world().properties.lavaLevel, (p, v) -> p.world().properties.lavaLevel = v)
		.build();

	public static final Page WORLD = Page.of("world", RTFTranslationKeys.GUI_WORLD_SETTINGS_TITLE,
		Category.of("continent", RTFTranslationKeys.GUI_LABEL_CONTINENT,
			CONTINENT_TYPE, CONTINENT_SHAPE, CONTINENT_SCALE, CONTINENT_JITTER, CONTINENT_SKIPPING, CONTINENT_SIZE_VARIANCE,
			CONTINENT_NOISE_OCTAVES, CONTINENT_NOISE_GAIN, CONTINENT_NOISE_LACUNARITY
		),
		Category.of("controlPoints", RTFTranslationKeys.GUI_LABEL_CONTROL_POINTS,
			ISLAND_INLAND, ISLAND_COAST, DEEP_OCEAN, SHALLOW_OCEAN, BEACH, COAST, NEAR_INLAND, MID_INLAND, FAR_INLAND
		),
		Category.of("properties", RTFTranslationKeys.GUI_LABEL_PROPERTIES,
			SPAWN_TYPE, WORLD_HEIGHT, WORLD_DEPTH, SEA_LEVEL, LAVA_LEVEL
		)
	);

	// Surface

	private static final Predicate<Preset> EROSION = (preset) -> preset.miscellaneous().erosionDecorator;
	private static final Predicate<Preset> STRATA = (preset) -> preset.miscellaneous().strataDecorator;

	public static final Page SURFACE = Page.of("surface", RTFTranslationKeys.GUI_SURFACE_SETTINGS_TITLE,
		Category.of("strata", RTFTranslationKeys.GUI_LABEL_STRATA,
			BoolOption.builder("miscellaneous.strataDecorator")
				.translation(RTFTranslationKeys.GUI_BUTTON_STRATA_DECORATOR)
				.bind((p) -> p.miscellaneous().strataDecorator, (p, v) -> p.miscellaneous().strataDecorator = v)
				.build(),
			IntOption.builder("miscellaneous.strataRegionSize")
				.translation(RTFTranslationKeys.GUI_SLIDER_STRATA_REGION_SIZE)
				.range(50, 1000)
				.activeWhen(STRATA)
				.bind((p) -> p.miscellaneous().strataRegionSize, (p, v) -> p.miscellaneous().strataRegionSize = v)
				.build(),
			BoolOption.builder("miscellaneous.oreCompatibleStoneOnly")
				.translation(RTFTranslationKeys.GUI_BUTTON_ORE_COMPATIBLE_STONE_ONLY)
				.activeWhen(STRATA)
				.bind((p) -> p.miscellaneous().oreCompatibleStoneOnly, (p, v) -> p.miscellaneous().oreCompatibleStoneOnly = v)
				.build(),
			BoolOption.builder("miscellaneous.plainStoneErosion")
				.translation(RTFTranslationKeys.GUI_BUTTON_PLAIN_STONE_EROSION)
				.activeWhen(STRATA)
				.activeWhen(EROSION)
				.bind((p) -> p.miscellaneous().plainStoneErosion, (p, v) -> p.miscellaneous().plainStoneErosion = v)
				.build()
		),
		Category.of("erosion", RTFTranslationKeys.GUI_LABEL_SURFACE_EROSION,
			BoolOption.builder("miscellaneous.erosionDecorator")
				.translation(RTFTranslationKeys.GUI_BUTTON_EROSION_DECORATOR)
				.bind((p) -> p.miscellaneous().erosionDecorator, (p, v) -> p.miscellaneous().erosionDecorator = v)
				.build(),
			IntOption.builder("surface.erosion.rockVariance")
				.translation(RTFTranslationKeys.GUI_SLIDER_ROCK_VARIANCE)
				.range(0, 255)
				.bind((p) -> p.surface().erosion().rockVariance, (p, v) -> p.surface().erosion().rockVariance = v)
				.activeWhen(EROSION)
				.build(),
			IntOption.builder("surface.erosion.rockMin")
				.translation(RTFTranslationKeys.GUI_SLIDER_ROCK_MIN)
				.range(-1024, 1024)
				.bind((p) -> p.surface().erosion().rockMin, (p, v) -> p.surface().erosion().rockMin = v)
				.activeWhen(EROSION)
				.build(),
			IntOption.builder("surface.erosion.dirtVariance")
				.translation(RTFTranslationKeys.GUI_SLIDER_DIRT_VARIANCE)
				.range(0, 255)
				.bind((p) -> p.surface().erosion().dirtVariance, (p, v) -> p.surface().erosion().dirtVariance = v)
				.activeWhen(EROSION)
				.build(),
			IntOption.builder("surface.erosion.dirtMin")
				.translation(RTFTranslationKeys.GUI_SLIDER_DIRT_MIN)
				.range(-1024, 1024)
				.bind((p) -> p.surface().erosion().dirtMin, (p, v) -> p.surface().erosion().dirtMin = v)
				.activeWhen(EROSION)
				.build(),
			// steepness thresholds must stay ordered scree <= dirt <= rock
			FloatOption.builder("surface.erosion.rockSteepness")
				.translation(RTFTranslationKeys.GUI_SLIDER_ROCK_STEEPNESS)
				.range(0.0F, 1.0F)
				.atLeast((p) -> p.surface().erosion().dirtSteepness)
				.bind((p) -> p.surface().erosion().rockSteepness, (p, v) -> p.surface().erosion().rockSteepness = v)
				.activeWhen(EROSION)
				.build(),
			FloatOption.builder("surface.erosion.dirtSteepness")
				.translation(RTFTranslationKeys.GUI_SLIDER_DIRT_STEEPNESS)
				.range(0.0F, 1.0F)
				.atLeast((p) -> p.surface().erosion().screeSteepness)
				.atMost((p) -> p.surface().erosion().rockSteepness)
				.bind((p) -> p.surface().erosion().dirtSteepness, (p, v) -> p.surface().erosion().dirtSteepness = v)
				.activeWhen(EROSION)
				.build(),
			FloatOption.builder("surface.erosion.screeSteepness")
				.translation(RTFTranslationKeys.GUI_SLIDER_SCREE_STEEPNESS)
				.range(0.0F, 1.0F)
				.atMost((p) -> p.surface().erosion().dirtSteepness)
				.bind((p) -> p.surface().erosion().screeSteepness, (p, v) -> p.surface().erosion().screeSteepness = v)
				.activeWhen(EROSION)
				.build(),
			FloatOption.builder("surface.erosion.snowSteepness")
				.translation(RTFTranslationKeys.GUI_SLIDER_SNOW_STEEPNESS)
				.range(0.0F, 1.0F)
				.bind((p) -> p.surface().erosion().snowSteepness, (p, v) -> p.surface().erosion().snowSteepness = v)
				.build(),
			FloatOption.builder("surface.erosion.heightModifier")
				.translation(RTFTranslationKeys.GUI_SLIDER_HEIGHT_MODIFIER)
				.range(0.0F, 255.0F)
				.bind((p) -> p.surface().erosion().heightModifier, (p, v) -> p.surface().erosion().heightModifier = v)
				.build(),
			FloatOption.builder("surface.erosion.slopeModifier")
				.translation(RTFTranslationKeys.GUI_SLIDER_SLOPE_MODIFIER)
				.range(0.0F, 255.0F)
				.bind((p) -> p.surface().erosion().slopeModifier, (p, v) -> p.surface().erosion().slopeModifier = v)
				.build()
		)
	);

	// Caves

	public static final Page CAVES = Page.of("caves", RTFTranslationKeys.GUI_CAVE_SETTINGS_TITLE,
		Category.of("noiseCaves", RTFTranslationKeys.GUI_LABEL_NOISE_CAVES,
			caveChance("entranceCaveProbability", RTFTranslationKeys.GUI_SLIDER_ENTRANCE_CAVE_CHANCE, (p) -> p.caves().entranceCaveProbability, (p, v) -> p.caves().entranceCaveProbability = v),
			FloatOption.builder("caves.cheeseCaveDepthOffset")
				.translation(RTFTranslationKeys.GUI_SLIDER_SURFACE_DENSITY_THRESHOLD)
				.range(1.5625F, 10.0F)
				.bind((p) -> p.caves().cheeseCaveDepthOffset, (p, v) -> p.caves().cheeseCaveDepthOffset = v)
				.build(),
			caveChance("cheeseCaveProbability", RTFTranslationKeys.GUI_SLIDER_CHEESE_CAVE_CHANCE, (p) -> p.caves().cheeseCaveProbability, (p, v) -> p.caves().cheeseCaveProbability = v),
			caveChance("spaghettiCaveProbability", RTFTranslationKeys.GUI_SLIDER_SPAGHETTI_CAVE_CHANCE, (p) -> p.caves().spaghettiCaveProbability, (p, v) -> p.caves().spaghettiCaveProbability = v),
			caveChance("noodleCaveProbability", RTFTranslationKeys.GUI_SLIDER_NOODLE_CAVE_CHANCE, (p) -> p.caves().noodleCaveProbability, (p, v) -> p.caves().noodleCaveProbability = v),
			BoolOption.builder("caves.largeOreVeins")
				.translation(RTFTranslationKeys.GUI_BUTTON_LARGE_ORE_VEINS)
				.bind((p) -> p.caves().largeOreVeins, (p, v) -> p.caves().largeOreVeins = v)
				.build()
		),
		Category.of("carvers", RTFTranslationKeys.GUI_LABEL_CARVERS,
			caveChance("caveCarverProbability", RTFTranslationKeys.GUI_SLIDER_CAVE_CARVER_CHANCE, (p) -> p.caves().caveCarverProbability, (p, v) -> p.caves().caveCarverProbability = v),
			FloatOption.builder("caves.deepCaveCarverProbability")
				.translation(RTFTranslationKeys.GUI_SLIDER_DEEP_CAVE_CARVER_CHANCE)
				.range(0.0F, 1.0F)
				// legacy distribution has no deep cave carvers
				.activeWhen((p) -> !p.caves().legacyCarverDistribution)
				.bind((p) -> p.caves().deepCaveCarverProbability, (p, v) -> p.caves().deepCaveCarverProbability = v)
				.build(),
			caveChance("ravineCarverProbability", RTFTranslationKeys.GUI_SLIDER_RAVINE_CARVER_CHANCE, (p) -> p.caves().ravineCarverProbability, (p, v) -> p.caves().ravineCarverProbability = v),
			BoolOption.builder("caves.legacyCarverDistribution")
				.translation(RTFTranslationKeys.GUI_BUTTON_LEGACY_CARVER_DISTRIBUTION)
				.bind((p) -> p.caves().legacyCarverDistribution, (p, v) -> p.caves().legacyCarverDistribution = v)
				.build()
		)
	);

	// Climate

	public static final Page CLIMATE = Page.of("climate", RTFTranslationKeys.GUI_CLIMATE_SETTINGS_TITLE,
		climateRange("temperature", RTFTranslationKeys.GUI_LABEL_TEMPERATURE, (p) -> p.climate().temperature,
			RTFTranslationKeys.GUI_SLIDER_TEMPERATURE_SCALE, RTFTranslationKeys.GUI_SLIDER_TEMPERATURE_FALLOFF,
			RTFTranslationKeys.GUI_SLIDER_TEMPERATURE_MIN, RTFTranslationKeys.GUI_SLIDER_TEMPERATURE_MAX, RTFTranslationKeys.GUI_SLIDER_TEMPERATURE_BIAS
		),
		climateRange("moisture", RTFTranslationKeys.GUI_LABEL_MOISTURE, (p) -> p.climate().moisture,
			RTFTranslationKeys.GUI_SLIDER_MOISTURE_SCALE, RTFTranslationKeys.GUI_SLIDER_MOISTURE_FALLOFF,
			RTFTranslationKeys.GUI_SLIDER_MOISTURE_MIN, RTFTranslationKeys.GUI_SLIDER_MOISTURE_MAX, RTFTranslationKeys.GUI_SLIDER_MOISTURE_BIAS
		),
		Category.of("biomeShape", RTFTranslationKeys.GUI_LABEL_BIOME_SHAPE,
			IntOption.builder("climate.biomeShape.biomeSize")
				.translation(RTFTranslationKeys.GUI_SLIDER_BIOME_SIZE)
				.range(50, 2000)
				.bind((p) -> p.climate().biomeShape.biomeSize, (p, v) -> p.climate().biomeShape.biomeSize = v)
				.build(),
			IntOption.builder("climate.biomeShape.macroNoiseSize")
				.translation(RTFTranslationKeys.GUI_SLIDER_MACRO_NOISE_SIZE)
				.range(1, 20)
				.bind((p) -> p.climate().biomeShape.macroNoiseSize, (p, v) -> p.climate().biomeShape.macroNoiseSize = v)
				.build(),
			IntOption.builder("climate.biomeShape.biomeWarpScale")
				.translation(RTFTranslationKeys.GUI_SLIDER_BIOME_WARP_SCALE)
				.range(1, 500)
				.bind((p) -> p.climate().biomeShape.biomeWarpScale, (p, v) -> p.climate().biomeShape.biomeWarpScale = v)
				.build(),
			IntOption.builder("climate.biomeShape.biomeWarpStrength")
				.translation(RTFTranslationKeys.GUI_SLIDER_BIOME_WARP_STRENGTH)
				.range(1, 500)
				.bind((p) -> p.climate().biomeShape.biomeWarpStrength, (p, v) -> p.climate().biomeShape.biomeWarpStrength = v)
				.build()
		),
		Category.of("biomeEdgeShape", RTFTranslationKeys.GUI_LABEL_BIOME_EDGE_SHAPE,
			EnumOption.<ClimateSettings.BiomeNoise.EdgeType>builder("climate.biomeEdgeShape.type")
				.translation(RTFTranslationKeys.GUI_BUTTON_BIOME_EDGE_TYPE)
				.values(List.of(ClimateSettings.BiomeNoise.EdgeType.values()))
				.bind((p) -> p.climate().biomeEdgeShape.type, (p, v) -> p.climate().biomeEdgeShape.type = v)
				.build(),
			IntOption.builder("climate.biomeEdgeShape.scale")
				.translation(RTFTranslationKeys.GUI_SLIDER_BIOME_EDGE_SCALE)
				.range(1, 500)
				.bind((p) -> p.climate().biomeEdgeShape.scale, (p, v) -> p.climate().biomeEdgeShape.scale = v)
				.build(),
			IntOption.builder("climate.biomeEdgeShape.octaves")
				.translation(RTFTranslationKeys.GUI_SLIDER_BIOME_EDGE_OCTAVES)
				.range(1, 5)
				.bind((p) -> p.climate().biomeEdgeShape.octaves, (p, v) -> p.climate().biomeEdgeShape.octaves = v)
				.build(),
			FloatOption.builder("climate.biomeEdgeShape.gain")
				.translation(RTFTranslationKeys.GUI_SLIDER_BIOME_EDGE_GAIN)
				.range(0.0F, 5.5F)
				.bind((p) -> p.climate().biomeEdgeShape.gain, (p, v) -> p.climate().biomeEdgeShape.gain = v)
				.build(),
			FloatOption.builder("climate.biomeEdgeShape.lacunarity")
				.translation(RTFTranslationKeys.GUI_SLIDER_BIOME_EDGE_LACUNARITY)
				.range(0.0F, 10.5F)
				.bind((p) -> p.climate().biomeEdgeShape.lacunarity, (p, v) -> p.climate().biomeEdgeShape.lacunarity = v)
				.build(),
			IntOption.builder("climate.biomeEdgeShape.strength")
				.translation(RTFTranslationKeys.GUI_SLIDER_BIOME_EDGE_STRENGTH)
				.range(1, 500)
				.bind((p) -> p.climate().biomeEdgeShape.strength, (p, v) -> p.climate().biomeEdgeShape.strength = v)
				.build()
		)
	);

	// Terrain

	public static final Page TERRAIN = Page.of("terrain", RTFTranslationKeys.GUI_TERRAIN_SETTINGS_TITLE,
		Category.of("general", RTFTranslationKeys.GUI_LABEL_GENERAL,
			IntOption.builder("terrain.general.terrainSeedOffset")
				.translation(RTFTranslationKeys.GUI_BUTTON_TERRAIN_SEED_OFFSET)
				.seed()
				.bind((p) -> p.terrain().general.terrainSeedOffset, (p, v) -> p.terrain().general.terrainSeedOffset = v)
				.build(),
			IntOption.builder("terrain.general.terrainRegionSize")
				.translation(RTFTranslationKeys.GUI_SLIDER_TERRAIN_REGION_SIZE)
				.range(125, 5000)
				.bind((p) -> p.terrain().general.terrainRegionSize, (p, v) -> p.terrain().general.terrainRegionSize = v)
				.build(),
			// the global scales were never shown in the editor
			FloatOption.builder("terrain.general.globalVerticalScale")
				.translation(RTFTranslationKeys.GUI_SLIDER_GLOBAL_VERTICAL_SCALE)
				.range(0.01F, 1.0F)
				.bind((p) -> p.terrain().general.globalVerticalScale, (p, v) -> p.terrain().general.globalVerticalScale = v)
				.hidden()
				.build(),
			FloatOption.builder("terrain.general.globalHorizontalScale")
				.translation(RTFTranslationKeys.GUI_SLIDER_GLOBAL_HORIZONTAL_SCALE)
				.range(0.01F, 5.0F)
				.bind((p) -> p.terrain().general.globalHorizontalScale, (p, v) -> p.terrain().general.globalHorizontalScale = v)
				.hidden()
				.build(),
			BoolOption.builder("terrain.general.fancyMountains")
				.translation(RTFTranslationKeys.GUI_BUTTON_FANCY_MOUNTAINS)
				.bind((p) -> p.terrain().general.fancyMountains, (p, v) -> p.terrain().general.fancyMountains = v)
				.build()
		),
		terrainType("steppe", RTFTranslationKeys.GUI_LABEL_STEPPE, (p) -> p.terrain().steppe, true, true),
		terrainType("plains", RTFTranslationKeys.GUI_LABEL_PLAINS, (p) -> p.terrain().plains, true, false),
		terrainType("hills", RTFTranslationKeys.GUI_LABEL_HILLS, (p) -> p.terrain().hills, false, true),
		terrainType("dales", RTFTranslationKeys.GUI_LABEL_DALES, (p) -> p.terrain().dales, false, true),
		terrainType("plateau", RTFTranslationKeys.GUI_LABEL_PLATEAU, (p) -> p.terrain().plateau, false, true),
		terrainType("badlands", RTFTranslationKeys.GUI_LABEL_BADLANDS, (p) -> p.terrain().badlands, false, true),
		terrainType("torridonian", RTFTranslationKeys.GUI_LABEL_TORRIDONIAN, (p) -> p.terrain().torridonian, false, true),
		terrainType("mountains", RTFTranslationKeys.GUI_LABEL_MOUNTAINS, (p) -> p.terrain().mountains, true, true),
		terrainType("volcano", RTFTranslationKeys.GUI_LABEL_VOLCANO, (p) -> p.terrain().volcano, false, false)
	);

	// Rivers

	public static final Page RIVERS = Page.of("rivers", RTFTranslationKeys.GUI_RIVER_SETTINGS_TITLE,
		Category.of("general", null,
			IntOption.builder("rivers.seedOffset")
				.translation(RTFTranslationKeys.GUI_BUTTON_RIVER_SEED_OFFSET)
				.seed()
				.bind((p) -> p.rivers().seedOffset, (p, v) -> p.rivers().seedOffset = v)
				.build(),
			IntOption.builder("rivers.riverCount")
				.translation(RTFTranslationKeys.GUI_SLIDER_RIVER_COUNT)
				.range(0, 30)
				.bind((p) -> p.rivers().riverCount, (p, v) -> p.rivers().riverCount = v)
				.build()
		),
		river("mainRivers", RTFTranslationKeys.GUI_LABEL_MAIN_RIVERS, (p) -> p.rivers().mainRivers, 50, 200, 150),
		river("branchRivers", RTFTranslationKeys.GUI_LABEL_BRANCH_RIVERS, (p) -> p.rivers().branchRivers, 20, 50, 100),
		Category.of("lakes", RTFTranslationKeys.GUI_LABEL_LAKES,
			FloatOption.builder("rivers.lakes.chance")
				.translation(RTFTranslationKeys.GUI_SLIDER_LAKE_CHANCE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.rivers().lakes.chance, (p, v) -> p.rivers().lakes.chance = v)
				.build(),
			IntOption.builder("rivers.lakes.depth")
				.translation(RTFTranslationKeys.GUI_SLIDER_LAKE_DEPTH)
				.range(1, 20)
				.bind((p) -> p.rivers().lakes.depth, (p, v) -> p.rivers().lakes.depth = v)
				.build(),
			IntOption.builder("rivers.lakes.sizeMin")
				.translation(RTFTranslationKeys.GUI_SLIDER_LAKE_SIZE_MIN)
				.range(1, 100)
				.atMost((p) -> p.rivers().lakes.sizeMax)
				.bind((p) -> p.rivers().lakes.sizeMin, (p, v) -> p.rivers().lakes.sizeMin = v)
				.build(),
			IntOption.builder("rivers.lakes.sizeMax")
				.translation(RTFTranslationKeys.GUI_SLIDER_LAKE_SIZE_MAX)
				.range(1, 500)
				.atLeast((p) -> p.rivers().lakes.sizeMin)
				.bind((p) -> p.rivers().lakes.sizeMax, (p, v) -> p.rivers().lakes.sizeMax = v)
				.build(),
			IntOption.builder("rivers.lakes.minBankHeight")
				.translation(RTFTranslationKeys.GUI_SLIDER_LAKE_MIN_BANK_HEIGHT)
				.range(1, 10)
				.atMost((p) -> p.rivers().lakes.maxBankHeight)
				.bind((p) -> p.rivers().lakes.minBankHeight, (p, v) -> p.rivers().lakes.minBankHeight = v)
				.build(),
			IntOption.builder("rivers.lakes.maxBankHeight")
				.translation(RTFTranslationKeys.GUI_SLIDER_LAKE_MAX_BANK_HEIGHT)
				.range(1, 10)
				.atLeast((p) -> p.rivers().lakes.minBankHeight)
				.bind((p) -> p.rivers().lakes.maxBankHeight, (p, v) -> p.rivers().lakes.maxBankHeight = v)
				.build()
		),
		Category.of("wetlands", RTFTranslationKeys.GUI_LABEL_WETLANDS,
			FloatOption.builder("rivers.wetlands.chance")
				.translation(RTFTranslationKeys.GUI_SLIDER_WETLAND_CHANCE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.rivers().wetlands.chance, (p, v) -> p.rivers().wetlands.chance = v)
				.build(),
			IntOption.builder("rivers.wetlands.sizeMin")
				.translation(RTFTranslationKeys.GUI_SLIDER_WETLAND_SIZE_MIN)
				.range(50, 500)
				.atMost((p) -> p.rivers().wetlands.sizeMax)
				.bind((p) -> p.rivers().wetlands.sizeMin, (p, v) -> p.rivers().wetlands.sizeMin = v)
				.build(),
			IntOption.builder("rivers.wetlands.sizeMax")
				.translation(RTFTranslationKeys.GUI_SLIDER_WETLAND_SIZE_MAX)
				.range(50, 500)
				.atLeast((p) -> p.rivers().wetlands.sizeMin)
				.bind((p) -> p.rivers().wetlands.sizeMax, (p, v) -> p.rivers().wetlands.sizeMax = v)
				.build()
		)
	);

	// Filters

	public static final Page FILTERS = Page.of("filters", RTFTranslationKeys.GUI_FILTER_SETTINGS_TITLE,
		Category.of("erosion", RTFTranslationKeys.GUI_LABEL_EROSION,
			IntOption.builder("filters.erosion.dropletsPerChunk")
				.translation(RTFTranslationKeys.GUI_SLIDER_EROSION_DROPLETS_PER_CHUNK)
				.range(10, 250)
				.tag(OptionTag.HEAVY_PERFORMANCE_IMPACT)
				.bind((p) -> p.filters().erosion.dropletsPerChunk, (p, v) -> p.filters().erosion.dropletsPerChunk = v)
				.build(),
			IntOption.builder("filters.erosion.dropletLifetime")
				.translation(RTFTranslationKeys.GUI_SLIDER_EROSION_DROPLET_LIFETIME)
				.range(1, 32)
				.tag(OptionTag.HEAVY_PERFORMANCE_IMPACT)
				.bind((p) -> p.filters().erosion.dropletLifetime, (p, v) -> p.filters().erosion.dropletLifetime = v)
				.build(),
			FloatOption.builder("filters.erosion.dropletVolume")
				.translation(RTFTranslationKeys.GUI_SLIDER_EROSION_DROPLET_VOLUME)
				.range(0.0F, 1.0F)
				.bind((p) -> p.filters().erosion.dropletVolume, (p, v) -> p.filters().erosion.dropletVolume = v)
				.build(),
			FloatOption.builder("filters.erosion.dropletVelocity")
				.translation(RTFTranslationKeys.GUI_SLIDER_EROSION_DROPLET_VELOCITY)
				.range(0.1F, 1.0F)
				.bind((p) -> p.filters().erosion.dropletVelocity, (p, v) -> p.filters().erosion.dropletVelocity = v)
				.build(),
			FloatOption.builder("filters.erosion.erosionRate")
				.translation(RTFTranslationKeys.GUI_SLIDER_EROSION_RATE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.filters().erosion.erosionRate, (p, v) -> p.filters().erosion.erosionRate = v)
				.build(),
			FloatOption.builder("filters.erosion.depositeRate")
				.translation(RTFTranslationKeys.GUI_SLIDER_DEPOSITE_RATE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.filters().erosion.depositeRate, (p, v) -> p.filters().erosion.depositeRate = v)
				.build()
		),
		Category.of("smoothing", RTFTranslationKeys.GUI_LABEL_SMOOTHING,
			IntOption.builder("filters.smoothing.iterations")
				.translation(RTFTranslationKeys.GUI_SLIDER_SMOOTHING_ITERATIONS)
				.range(0, 5)
				.tag(OptionTag.MEDIUM_PERFORMANCE_IMPACT)
				.bind((p) -> p.filters().smoothing.iterations, (p, v) -> p.filters().smoothing.iterations = v)
				.build(),
			FloatOption.builder("filters.smoothing.smoothingRadius")
				.translation(RTFTranslationKeys.GUI_SLIDER_SMOOTHING_RADIUS)
				.range(0.0F, 5.0F)
				.bind((p) -> p.filters().smoothing.smoothingRadius, (p, v) -> p.filters().smoothing.smoothingRadius = v)
				.build(),
			FloatOption.builder("filters.smoothing.smoothingRate")
				.translation(RTFTranslationKeys.GUI_SLIDER_SMOOTHING_RATE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.filters().smoothing.smoothingRate, (p, v) -> p.filters().smoothing.smoothingRate = v)
				.build()
		)
	);

	// Miscellaneous; the hidden options are kept in the file but the current generator doesn't read them

	public static final Page MISCELLANEOUS = Page.of("miscellaneous", RTFTranslationKeys.GUI_MISCELLANEOUS_SETTINGS_TITLE,
		Category.of("general", null,
			BoolOption.builder("miscellaneous.smoothLayerDecorator")
				.translation(RTFTranslationKeys.GUI_BUTTON_SMOOTH_LAYER_DECORATOR)
				.bind((p) -> p.miscellaneous().smoothLayerDecorator, (p, v) -> p.miscellaneous().smoothLayerDecorator = v)
				.build(),
			BoolOption.builder("miscellaneous.naturalSnowDecorator")
				.translation(RTFTranslationKeys.GUI_BUTTON_NATURAL_SNOW_DECORATOR)
				.bind((p) -> p.miscellaneous().naturalSnowDecorator, (p, v) -> p.miscellaneous().naturalSnowDecorator = v)
				.build(),
			BoolOption.builder("miscellaneous.customBiomeFeatures")
				.translation(RTFTranslationKeys.GUI_BUTTON_CUSTOM_BIOME_FEATURES)
				.bind((p) -> p.miscellaneous().customBiomeFeatures, (p, v) -> p.miscellaneous().customBiomeFeatures = v)
				.build(),
			BoolOption.builder("miscellaneous.vanillaSprings")
				.translation(RTFTranslationKeys.GUI_BUTTON_VANILLA_SPRINGS)
				.bind((p) -> p.miscellaneous().vanillaSprings, (p, v) -> p.miscellaneous().vanillaSprings = v)
				.build(),
			BoolOption.builder("miscellaneous.vanillaLavaLakes")
				.translation(RTFTranslationKeys.GUI_BUTTON_VANILLA_LAVA_LAKES)
				.bind((p) -> p.miscellaneous().vanillaLavaLakes, (p, v) -> p.miscellaneous().vanillaLavaLakes = v)
				.build(),
			BoolOption.builder("miscellaneous.vanillaLavaSprings")
				.translation(RTFTranslationKeys.GUI_BUTTON_VANILLA_LAVA_SPRINGS)
				.bind((p) -> p.miscellaneous().vanillaLavaSprings, (p, v) -> p.miscellaneous().vanillaLavaSprings = v)
				.build(),
			FloatOption.builder("miscellaneous.mountainBiomeUsage")
				.translation(RTFTranslationKeys.GUI_SLIDER_MOUNTAIN_BIOME_USAGE)
				.range(0.0F, 1.0F)
				.bind((p) -> p.miscellaneous().mountainBiomeUsage, (p, v) -> p.miscellaneous().mountainBiomeUsage = v)
				.hidden()
				.build(),
			FloatOption.builder("miscellaneous.volcanoBiomeUsage")
				.translation(RTFTranslationKeys.GUI_SLIDER_VOLCANO_BIOME_USAGE)
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

	public static final Page LANDFORMS = Page.of("landforms", RTFTranslationKeys.GUI_LANDFORM_SETTINGS_TITLE,
		Category.of("buttes", RTFTranslationKeys.GUI_LABEL_BUTTES,
			BoolOption.builder("landforms.buttes.enabled")
				.translation(RTFTranslationKeys.GUI_BUTTON_BUTTES)
				.bind((p) -> p.landforms().buttes.enabled, (p, v) -> p.landforms().buttes.enabled = v)
				.build(),
			FloatOption.builder("landforms.buttes.density")
				.translation(RTFTranslationKeys.GUI_SLIDER_BUTTE_DENSITY)
				.range(0.0F, 1.0F)
				.activeWhen(BUTTES)
				.bind((p) -> p.landforms().buttes.density, (p, v) -> p.landforms().buttes.density = v)
				.build(),
			IntOption.builder("landforms.buttes.height")
				.translation(RTFTranslationKeys.GUI_SLIDER_BUTTE_HEIGHT)
				.range(10, 120)
				.activeWhen(BUTTES)
				.bind((p) -> p.landforms().buttes.height, (p, v) -> p.landforms().buttes.height = v)
				.build(),
			FloatOption.builder("landforms.buttes.size")
				.translation(RTFTranslationKeys.GUI_SLIDER_BUTTE_SIZE)
				.range(0.5F, 2.5F)
				.activeWhen(BUTTES)
				.bind((p) -> p.landforms().buttes.size, (p, v) -> p.landforms().buttes.size = v)
				.build()
		),
		Category.of("canyons", RTFTranslationKeys.GUI_LABEL_CANYONS,
			BoolOption.builder("landforms.canyons.enabled")
				.translation(RTFTranslationKeys.GUI_BUTTON_CANYONS)
				.bind((p) -> p.landforms().canyons.enabled, (p, v) -> p.landforms().canyons.enabled = v)
				.build(),
			IntOption.builder("landforms.canyons.depth")
				.translation(RTFTranslationKeys.GUI_SLIDER_CANYON_DEPTH)
				.range(10, 100)
				.activeWhen(CANYONS)
				.bind((p) -> p.landforms().canyons.depth, (p, v) -> p.landforms().canyons.depth = v)
				.build(),
			FloatOption.builder("landforms.canyons.frequency")
				.translation(RTFTranslationKeys.GUI_SLIDER_CANYON_FREQUENCY)
				.range(0.2F, 3.0F)
				.activeWhen(CANYONS)
				.bind((p) -> p.landforms().canyons.frequency, (p, v) -> p.landforms().canyons.frequency = v)
				.build(),
			FloatOption.builder("landforms.canyons.width")
				.translation(RTFTranslationKeys.GUI_SLIDER_CANYON_WIDTH)
				.range(0.3F, 3.0F)
				.activeWhen(CANYONS)
				.bind((p) -> p.landforms().canyons.width, (p, v) -> p.landforms().canyons.width = v)
				.build()
		),
		Category.of("seaCliffs", RTFTranslationKeys.GUI_LABEL_SEA_CLIFFS,
			BoolOption.builder("landforms.seaCliffs.enabled")
				.translation(RTFTranslationKeys.GUI_BUTTON_SEA_CLIFFS)
				.bind((p) -> p.landforms().seaCliffs.enabled, (p, v) -> p.landforms().seaCliffs.enabled = v)
				.build(),
			FloatOption.builder("landforms.seaCliffs.frequency")
				.translation(RTFTranslationKeys.GUI_SLIDER_SEA_CLIFF_FREQUENCY)
				.range(0.0F, 1.0F)
				.activeWhen(SEA_CLIFFS)
				.bind((p) -> p.landforms().seaCliffs.frequency, (p, v) -> p.landforms().seaCliffs.frequency = v)
				.build(),
			IntOption.builder("landforms.seaCliffs.height")
				.translation(RTFTranslationKeys.GUI_SLIDER_SEA_CLIFF_HEIGHT)
				.range(6, 80)
				.activeWhen(SEA_CLIFFS)
				.bind((p) -> p.landforms().seaCliffs.height, (p, v) -> p.landforms().seaCliffs.height = v)
				.build(),
			BoolOption.builder("landforms.seaCliffs.seaStacks")
				.translation(RTFTranslationKeys.GUI_BUTTON_SEA_STACKS)
				.activeWhen(SEA_CLIFFS)
				.bind((p) -> p.landforms().seaCliffs.seaStacks, (p, v) -> p.landforms().seaCliffs.seaStacks = v)
				.build()
		),
		Category.of("volcanoes", RTFTranslationKeys.GUI_LABEL_VOLCANOES,
			BoolOption.builder("landforms.volcanicSurface")
				.translation(RTFTranslationKeys.GUI_BUTTON_VOLCANIC_SURFACE)
				.bind((p) -> p.landforms().volcanicSurface, (p, v) -> p.landforms().volcanicSurface = v)
				.build()
		)
	);

	public static final List<Page> PAGES = List.of(WORLD, SURFACE, CAVES, CLIMATE, TERRAIN, RIVERS, LANDFORMS, FILTERS, MISCELLANEOUS);

	private static final Map<String, Option<?>> BY_PATH = PAGES.stream().flatMap(Page::options).collect(Collectors.toUnmodifiableMap(Option::path, Function.identity()));

	// the few settings shown directly in the Create World screen, most noticeable first, with labels that make sense
	// without their page and category
	public record SimpleSetting(Option<?> option, String labelKey) {
	}

	public static final List<SimpleSetting> SIMPLE = List.of(
		simple("world.continent.continentScale", RTFTranslationKeys.SIMPLE_CONTINENT_SIZE),
		simple("climate.biomeShape.biomeSize", RTFTranslationKeys.SIMPLE_BIOME_SIZE),
		simple("terrain.general.terrainRegionSize", RTFTranslationKeys.SIMPLE_TERRAIN_AREA_SIZE),
		simple("terrain.mountains.weight", RTFTranslationKeys.SIMPLE_MOUNTAINS),
		simple("rivers.riverCount", RTFTranslationKeys.SIMPLE_RIVERS),
		simple("rivers.lakes.chance", RTFTranslationKeys.SIMPLE_LAKES),
		simple("world.properties.seaLevel", RTFTranslationKeys.SIMPLE_SEA_LEVEL),
		simple("world.properties.worldHeight", RTFTranslationKeys.SIMPLE_WORLD_HEIGHT),
		simple("caves.cheeseCaveProbability", RTFTranslationKeys.SIMPLE_CAVES)
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
				.translation(RTFTranslationKeys.GUI_BUTTON_CLIMATE_SEED_OFFSET)
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
				.translation(RTFTranslationKeys.GUI_SLIDER_TERRAIN_WEIGHT)
				.range(0.0F, 10.0F)
				.bind((p) -> terrain.apply(p).weight, (p, v) -> terrain.apply(p).weight = v)
				.build(),
			hiddenUnless(usesBaseAndVerticalScale, FloatOption.builder(path + "baseScale")
				.translation(RTFTranslationKeys.GUI_SLIDER_TERRAIN_BASE_SCALE)
				.range(0.0F, 2.0F)
				.bind((p) -> terrain.apply(p).baseScale, (p, v) -> terrain.apply(p).baseScale = v)
			).build(),
			hiddenUnless(usesBaseAndVerticalScale, FloatOption.builder(path + "verticalScale")
				.translation(RTFTranslationKeys.GUI_SLIDER_TERRAIN_VERTICAL_SCALE)
				.range(0.0F, 10.0F)
				.bind((p) -> terrain.apply(p).verticalScale, (p, v) -> terrain.apply(p).verticalScale = v)
			).build(),
			hiddenUnless(usesHorizontalScale, FloatOption.builder(path + "horizontalScale")
				.translation(RTFTranslationKeys.GUI_SLIDER_TERRAIN_HORIZONTAL_SCALE)
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
				.translation(RTFTranslationKeys.GUI_SLIDER_RIVER_BED_DEPTH)
				.range(1, maxBedDepth)
				.bind((p) -> river.apply(p).bedDepth, (p, v) -> river.apply(p).bedDepth = v)
				.build(),
			// river bank heights are kept in the file but the river carver doesn't use them at the moment
			IntOption.builder(path + "minBankHeight")
				.translation(RTFTranslationKeys.GUI_SLIDER_RIVER_MIN_BANK_HEIGHT)
				.range(0, 20)
				.atMost((p) -> river.apply(p).maxBankHeight)
				.bind((p) -> river.apply(p).minBankHeight, (p, v) -> river.apply(p).minBankHeight = v)
				.hidden()
				.build(),
			IntOption.builder(path + "maxBankHeight")
				.translation(RTFTranslationKeys.GUI_SLIDER_RIVER_MAX_BANK_HEIGHT)
				.range(1, 20)
				.atLeast((p) -> river.apply(p).minBankHeight)
				.bind((p) -> river.apply(p).maxBankHeight, (p, v) -> river.apply(p).maxBankHeight = v)
				.hidden()
				.build(),
			IntOption.builder(path + "bedWidth")
				.translation(RTFTranslationKeys.GUI_SLIDER_RIVER_BED_WIDTH)
				.range(1, maxBedWidth)
				.bind((p) -> river.apply(p).bedWidth, (p, v) -> river.apply(p).bedWidth = v)
				.build(),
			IntOption.builder(path + "bankWidth")
				.translation(RTFTranslationKeys.GUI_SLIDER_RIVER_BANK_WIDTH)
				.range(1, maxBankWidth)
				.bind((p) -> river.apply(p).bankWidth, (p, v) -> river.apply(p).bankWidth = v)
				.build(),
			FloatOption.builder(path + "fade")
				.translation(RTFTranslationKeys.GUI_SLIDER_RIVER_FADE)
				.range(0.0F, 1.0F)
				.bind((p) -> river.apply(p).fade, (p, v) -> river.apply(p).fade = v)
				.build()
		);
	}

	private PresetOptions() {
	}
}
