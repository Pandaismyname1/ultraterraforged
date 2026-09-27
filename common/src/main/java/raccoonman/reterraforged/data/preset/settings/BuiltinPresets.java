package raccoonman.reterraforged.data.preset.settings;

import raccoonman.reterraforged.data.preset.settings.ClimateSettings.BiomeNoise;
import raccoonman.reterraforged.data.preset.settings.ClimateSettings.BiomeShape;
import raccoonman.reterraforged.data.preset.settings.ClimateSettings.RangeValue;
import raccoonman.reterraforged.data.preset.settings.FilterSettings.Erosion;
import raccoonman.reterraforged.data.preset.settings.FilterSettings.Smoothing;
import raccoonman.reterraforged.data.preset.settings.RiverSettings.Lake;
import raccoonman.reterraforged.data.preset.settings.RiverSettings.River;
import raccoonman.reterraforged.data.preset.settings.RiverSettings.Wetland;
import raccoonman.reterraforged.data.preset.settings.TerrainSettings.General;
import raccoonman.reterraforged.data.preset.settings.TerrainSettings.Terrain;
import raccoonman.reterraforged.data.preset.settings.WorldSettings.Continent;
import raccoonman.reterraforged.data.preset.settings.WorldSettings.ControlPoints;
import raccoonman.reterraforged.data.preset.settings.WorldSettings.Properties;
import raccoonman.reterraforged.world.worldgen.biome.spawn.SpawnType;
import raccoonman.reterraforged.world.worldgen.continent.ContinentType;
import raccoonman.reterraforged.world.worldgen.continent.IslandPopulator;
import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;

//1.07, 4.975
//1.095, 5
//1.095, 4.097
public class BuiltinPresets {
	
	public static Preset makeDefault() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI_IMPROVED, DistanceFunction.EUCLIDEAN, 3000, 0.7F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(IslandPopulator.DEFAULT_INLAND_POINT, IslandPopulator.DEFAULT_COAST_POINT, 0.1F, 0.25F, 0.327F, 0.448F, 0.502F, 1.327F, 2.758F), 
				new Properties(SpawnType.CONTINENT_CENTER, 512, 64, 63, -54)
			), 
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 1024, 40, 95, 95, 0.65F, 0.475F, 0.4F, 0.45F, 6.0F, 3.0F, SurfaceSettings.Erosion.DEFAULT_SNOW_ASPECT)),
			new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.14285715F, 0.07F, 0.01F, true, true),
			new ClimateSettings(
				new RangeValue(0, 6, 2, 0.0F, 0.98F, 0.05F), 
				new RangeValue(0, 6, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(225, 8, 150, 80),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
			), 
			new TerrainSettings(
				new General(0, 1200, 0.98F, 1.0F, true),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F)
			), 
			new RiverSettings(
				0, 8, 
				new River(5, 2, 6, 20, 8, 0.75F),
				new River(4, 1, 4, 14, 5, 0.975F), 
				new Lake(0.3F, 10, 75, 150, 2, 10),
				new Wetland(0.6F, 175, 225)
			), 
			new FilterSettings(
				new Erosion(135, 12, 0.7F, 0.7F, 0.5F, 0.5F),
				new Smoothing(1, 1.8F, 0.9F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(true, 600, true, true, true, false, true, true, true, true, true, 0.4F, 0.4F, true),
			LandformSettings.makeDefault(),
			CoastSettings.makeDefault(),
			CaveFeatureSettings.makeDefault(),
			OceanSettings.makeDefault()
		); 
	}
	
	@Deprecated
	public static Preset makeLegacyDefault() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI_IMPROVED, DistanceFunction.EUCLIDEAN, 3000, 0.7F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(IslandPopulator.DEFAULT_INLAND_POINT, IslandPopulator.DEFAULT_COAST_POINT, 0.1F, 0.25F, 0.327F, 0.448F, 0.502F, 1.327F, 2.758F), 
				new Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54)
			), 
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 95, 0.65F, 0.475F, 0.4F, 0.45F, 6.0F, 3.0F, 0)),
			new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.14285715F, 0.07F, 0.02F, true, false),
			new ClimateSettings(
				new RangeValue(0, 6, 2, 0.0F, 0.98F, 0.05F), 
				new RangeValue(0, 6, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(225, 8, 150, 80),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
			), 
			new TerrainSettings(
				new General(0, 1200, 0.98F, 1.0F, true),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(5.0F, 1.0F, 1.0F, 1.0F)
		    ), 
			new RiverSettings(
				0, 8, 
				new River(5, 2, 6, 20, 8, 0.75F),
				new River(4, 1, 4, 14, 5, 0.975F), 
				new Lake(0.3F, 10, 75, 150, 2, 10),
				new Wetland(0.6F, 175, 225), false
			), 
			new FilterSettings(
				new Erosion(135, 12, 0.7F, 0.7F, 0.5F, 0.5F),
				new Smoothing(1, 1.8F, 0.9F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(true, 600, true, true, true, false, true, true, true, true, true, 0.4F, 0.4F, false),
			LandformSettings.makeNone(),
			CoastSettings.makeNone(),
			CaveFeatureSettings.makeNone(),
			OceanSettings.makeNone()
		); 
	}

	@Deprecated
	public static Preset makeLegacyVanillaish() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI, DistanceFunction.EUCLIDEAN, 2000, 0.763F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(IslandPopulator.DEFAULT_INLAND_POINT, IslandPopulator.DEFAULT_COAST_POINT, 0.1F, 0.25F, 0.326F, 0.448F, 0.5F, 1.327F, 2.758F), 
				new Properties(SpawnType.WORLD_ORIGIN, 320, 64, 63, -54)
			), 
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 95, 0.65F, 0.475F, 0.4F, 0.45F, 6.0F, 3.0F, 0)),
			new CaveSettings(1.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.15F, 0.07F, 0.021F, true, false),
			new ClimateSettings(
				new RangeValue(0, 4, 1, 0.0F, 0.98F, 0.05F), 
				new RangeValue(0, 5, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(176, 6, 150, 80),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
			), 
			new TerrainSettings(
				new General(0, 690, 0.629F, 0.629F, false),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.25F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(3.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(3.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(6.0F, 1.0F, 1.0F, 1.0F)
			), 
			new RiverSettings(
				0, 11, 
				new River(5, 2, 6, 20, 8, 0.507F),
				new River(4, 1, 4, 14, 5, 0.493F), 
				new Lake(0.462F, 10, 75, 150, 2, 10),
				new Wetland(0.796F, 196, 255), false
			), 
			new FilterSettings(
				new Erosion(100, 12, 0.699F, 0.699F, 0.5F, 0.5F),
				new Smoothing(2, 1.8F, 0.75F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(false, 600, false, true, false, false, false, false, true, true, true, 1.0F, 0.75F, false),
			LandformSettings.makeNone(),
			CoastSettings.makeNone(),
			CaveFeatureSettings.makeNone(),
			OceanSettings.makeNone()
		); 
	}
	
	@Deprecated
	public static Preset makeLegacyBeautiful() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI, DistanceFunction.EUCLIDEAN, 3000, 0.8F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(IslandPopulator.DEFAULT_INLAND_POINT, IslandPopulator.DEFAULT_COAST_POINT, 0.1F, 0.25F, 0.326F, 0.448F, 0.5F, 1.327F, 2.758F), 
				new Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54)
			), 
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 95, 0.65F, 0.475F, 0.4F, 0.45F, 6.0F, 3.0F, 0)),
			new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.14285715F, 0.07F, 0.02F, true, false),
			new ClimateSettings(
				new RangeValue(0, 7, 1, 0.0F, 1.0F, -0.004F), 
				new RangeValue(0, 6, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(185, 8, 150, 80),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
			), 
			new TerrainSettings(
				new General(0, 1356, 1.0F, 1.175F, true),
				new Terrain(1.519F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.164F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.706F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.184F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.576F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.493F, 1.0F, 1.0F, 1.0F), 
				new Terrain(3.555F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.911F, 1.0F, 1.0F, 1.0F),
				new Terrain(7.5F, 1.0F, 1.0F, 1.0F)
			), 
			new RiverSettings(
				0, 14, 
				new River(5, 1, 4, 20, 5, 0.504F),
				new River(3, 1, 2, 12, 4, 0.5F), 
				new Lake(0.671F, 8, 75, 150, 2, 7),
				new Wetland(0.865F, 134, 201), false
			), 
			new FilterSettings(
				new Erosion(175, 12, 0.648F, 0.657F, 0.5F, 0.5F),
				new Smoothing(1, 1.855F, 0.916F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(true, 684, true, true, true, false, true, true, true, true, false, 0.853F, 0.855F, false),
			LandformSettings.makeNone(),
			CoastSettings.makeNone(),
			CaveFeatureSettings.makeNone(),
			OceanSettings.makeNone()
		); 
	}

	@Deprecated
	public static Preset makeLegacyLite() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI, DistanceFunction.EUCLIDEAN, 2000, 0.765F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(-1.0F, -1.0F, 0.1F, 0.25F, 0.326F, 0.448F, 0.5F, 1.327F, 2.758F), 
				new Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54)
			),
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 95, 0.65F, 0.475F, 0.4F, 0.45F, 6.0F, 3.0F, 0)),
			new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.14285715F, 0.07F, 0.02F, false, false),
			new ClimateSettings(
				new RangeValue(0, 4, 1, 0.0F, 0.98F, 0.05F),
				new RangeValue(0, 5, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(176, 6, 150, 80),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
			), 
			new TerrainSettings(
				new General(0, 690, 0.629F, 0.629F, false),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.25F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(3.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(3.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(6.0F, 1.0F, 1.0F, 1.0F)
			), 
			new RiverSettings(
				0, 11, 
				new River(5, 2, 6, 20, 8, 0.507F),
				new River(4, 1, 4, 14, 5, 0.493F), 
				new Lake(0.462F, 10, 75, 150, 2, 10),
				new Wetland(0.796F, 196, 255), false
			), 
			new FilterSettings(
				new Erosion(100, 12, 0.699F, 0.699F, 0.5F, 0.5F),
				new Smoothing(2, 1.799F, 0.75F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(true, 600, false, true, true, false, true, true, true, true, true, 1.0F, 0.75F, false),
			LandformSettings.makeNone(),
			CoastSettings.makeNone(),
			CaveFeatureSettings.makeNone(),
			OceanSettings.makeNone()
		);
	}

	@Deprecated
	public static Preset makeLegacyHugeBiomes() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI, DistanceFunction.EUCLIDEAN, 4029, 0.8F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(-1.0F, -1.0F, 0.1F, 0.25F, 0.326F, 0.448F, 0.5F, 1.327F, 2.758F), 
				new Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54)
			), 
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 95, 0.65F, 0.475F, 0.4F, 0.45F, 6.0F, 3.0F, 0)),
			new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.14285715F, 0.07F, 0.02F, true, false),
			new ClimateSettings(
				new RangeValue(0, 4, 2, 0.0F, 1.0F, 0.097F), 
				new RangeValue(0, 3, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(402, 5, 180, 110),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.PERLIN2, 24, 1, 0.5F, 2.65F, 60)
			), 
			new TerrainSettings(
				new General(0, 1507, 1.0F, 1.175F, true),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(5.0F, 1.0F, 1.0F, 1.0F)
			), 
			new RiverSettings(
				0, 13, 
				new River(5, 1, 5, 28, 9, 0.27F),
				new River(3, 1, 3, 18, 3, 0.7F), 
				new Lake(0.595F, 10, 75, 150, 2, 10),
				new Wetland(0.86F, 175, 225), false
			), 
			new FilterSettings(
				new Erosion(165, 15, 0.612F, 0.652F, 0.5F, 0.5F),
				new Smoothing(1, 1.799F, 0.898F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(true, 721, true, true, true, false, true, true, true, true, false, 0.902F, 0.945F, false),
			LandformSettings.makeNone(),
			CoastSettings.makeNone(),
			CaveFeatureSettings.makeNone(),
			OceanSettings.makeNone()
		);
	}

	// Curated presets: each starts from the Default preset and changes the few settings that shape it most

	public static Preset makeArchipelago() {
		Preset preset = makeDefault();
		preset.world().continent.continentScale = 800;
		preset.world().continent.continentSkipping = 0.5F;
		controlPoints(preset, 0.25F, 0.4F, 0.47F, 0.55F, 0.62F);
		preset.climate().temperature.bias = 0.3F;
		return preset;
	}

	public static Preset makeSupercontinent() {
		Preset preset = makeDefault();
		preset.world().continent.continentScale = 9000;
		preset.world().continent.continentSkipping = 0.0F;
		preset.world().continent.continentSizeVariance = 0.1F;
		controlPoints(preset, 0.03F, 0.08F, 0.12F, 0.2F, 0.26F);
		preset.rivers().riverCount = 14;
		return preset;
	}

	public static Preset makeHighlands() {
		Preset preset = makeDefault();
		TerrainSettings terrain = preset.terrain();
		terrain.mountains.weight = 6.0F;
		terrain.mountains.verticalScale = 1.6F;
		terrain.hills.weight = 3.0F;
		terrain.hills.verticalScale = 1.5F;
		terrain.plateau.weight = 3.0F;
		terrain.plateau.verticalScale = 1.5F;
		terrain.torridonian.verticalScale = 1.3F;
		terrain.dales.verticalScale = 1.2F;
		terrain.plains.weight = 0.5F;
		terrain.steppe.weight = 0.3F;
		return preset;
	}

	public static Preset makePrairie() {
		Preset preset = makeDefault();
		TerrainSettings terrain = preset.terrain();
		terrain.plains.weight = 5.0F;
		terrain.steppe.weight = 4.0F;
		terrain.dales.weight = 2.0F;
		terrain.hills.weight = 0.5F;
		terrain.plateau.weight = 0.3F;
		terrain.badlands.weight = 0.2F;
		terrain.torridonian.weight = 0.3F;
		terrain.mountains.weight = 0.3F;
		terrain.volcano.weight = 0.0F;
		preset.rivers().riverCount = 10;
		return preset;
	}

	public static Preset makeBadlands() {
		Preset preset = makeDefault();
		TerrainSettings terrain = preset.terrain();
		terrain.badlands.weight = 6.0F;
		terrain.plateau.weight = 4.0F;
		terrain.torridonian.weight = 3.0F;
		preset.climate().temperature.bias = 0.8F;
		preset.climate().temperature.min = 0.3F;
		preset.climate().moisture.bias = -0.8F;
		preset.climate().moisture.max = 0.6F;
		preset.rivers().lakes.chance = 0.1F;
		preset.rivers().wetlands.chance = 0.1F;
		return preset;
	}

	public static Preset makeFrozenNorth() {
		Preset preset = makeDefault();
		preset.climate().temperature.bias = -0.8F;
		preset.climate().temperature.max = 0.6F;
		return preset;
	}

	public static Preset makeTropics() {
		Preset preset = makeDefault();
		preset.climate().temperature.bias = 0.8F;
		preset.climate().temperature.min = 0.4F;
		preset.climate().moisture.bias = 0.5F;
		return preset;
	}

	public static Preset makeVolcanicIsles() {
		Preset preset = makeDefault();
		preset.world().continent.continentScale = 1500;
		controlPoints(preset, 0.2F, 0.33F, 0.4F, 0.48F, 0.54F);
		preset.terrain().volcano.weight = 10.0F;
		preset.terrain().mountains.weight = 3.0F;
		return preset;
	}

	public static Preset makeWaterlands() {
		Preset preset = makeDefault();
		RiverSettings rivers = preset.rivers();
		rivers.riverCount = 24;
		rivers.mainRivers.bedWidth = 14;
		rivers.mainRivers.bankWidth = 34;
		rivers.mainRivers.bedDepth = 6;
		rivers.branchRivers.bedWidth = 9;
		rivers.branchRivers.bankWidth = 22;
		rivers.lakes.chance = 0.9F;
		rivers.lakes.sizeMin = 100;
		rivers.lakes.sizeMax = 300;
		rivers.wetlands.chance = 1.0F;
		rivers.wetlands.sizeMin = 250;
		rivers.wetlands.sizeMax = 400;
		preset.climate().moisture.bias = 0.4F;
		// lowland terrain, so the water has somewhere to spread out
		preset.terrain().plains.weight = 3.0F;
		preset.terrain().mountains.weight = 1.0F;
		return preset;
	}

	public static Preset makePatchwork() {
		Preset preset = makeDefault();
		preset.world().continent.continentScale = 2000;
		preset.climate().biomeShape.biomeSize = 70;
		preset.terrain().general.terrainRegionSize = 300;
		return preset;
	}

	// the ocean to inland transition points, which have to stay in increasing order
	private static void controlPoints(Preset preset, float deepOcean, float shallowOcean, float beach, float coast, float inland) {
		ControlPoints points = preset.world().controlPoints;
		points.deepOcean = deepOcean;
		points.shallowOcean = shallowOcean;
		points.beach = beach;
		points.coast = coast;
		points.nearInland = inland;
	}
}
