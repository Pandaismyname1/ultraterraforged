package com.pandaismyname1.ultraterraforged.world.worldgen.heightmap;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import com.pandaismyname1.ultraterraforged.data.preset.PresetTerrainNoise;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.settings.TerrainSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings.ControlPoints;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.Continentalness;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellField;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.noise.CellSampler;
import com.pandaismyname1.ultraterraforged.world.worldgen.climate.Climate;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.CoastShaper;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.Continent;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.ContinentLerper2;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.ContinentLerper3;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.DistanceFunction;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.EdgeFunction;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.Interpolation;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Cache2d;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Landform;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Landforms;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.Rivermap;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.MountainChainPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainCategory;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainProvider;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.Populators;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.VolcanoPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.region.RegionLerper;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.region.RegionModule;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.region.RegionSelector;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.Seed;

//TODO rework this whole class
public record Heightmap(CellSampler.Provider cellProvider, CellPopulator terrain, CellPopulator region, Continent continent, Climate climate, Levels levels, ControlPoints controlPoints, float terrainFrequency, @Deprecated Noise mountainChainAlpha, @Deprecated Noise beachAlpha, Landform landforms) { //TODO move noise fields to RegionModule
	// the share of biome regions that get the variant biome, as in vanilla
	private static final float VARIANT_SHARE = 0.5F;
	// the middle of vanilla's most eroded level, where its swamps are
	private static final float SWAMP_EROSION = 0.775F;
	// how many blocks above the sea land in the sea's part of the continent turns from coast to inland
	private static final int ISLAND_INLAND = 6;
	
	//TODO move this to a factory or something instead
	public Heightmap cache() {
		//TODO map the rest of the noise as well once this is fully working
		CellSampler.Provider cellProvider = new CellSampler.Provider();
		Noise.Visitor visitor = (noise) -> {
			if(noise instanceof Cache2d	cache2d) {
				return new Cache2d.Cached(cache2d.noise());
			}
			return cellProvider.apply(noise);
		};
		return new Heightmap(cellProvider, this.terrain.mapNoise(visitor), this.region, this.continent, this.climate, this.levels, this.controlPoints, this.terrainFrequency, this.mountainChainAlpha, this.beachAlpha, this.landforms.mapNoise(visitor));
	}
	
	public void applyContinent(Cell cell, float x, float z) {
        this.continent.apply(cell, x, z);
	}
	
	public void applyTerrain(Cell cell, float x, float z, Rivermap rivermap) {
		this.applyTerrainTypes(cell, x, z);

        // rivers carve into the finished terrain; applied before it, as upstream 1.20.2 did, the terrain overwrote them
        rivermap.apply(cell, x, z);

        this.landforms.apply(cell, x, z, this);

        VolcanoPopulator.modifyVolcanoType(cell, this.levels);

        CoastShaper.label(cell, this.levels);
	}

	private void applyTerrainTypes(Cell cell, float x, float z) {
        cell.terrain = TerrainType.PLAINS;
        cell.riverDistance = 1.0F;
        cell.riverBank = Cell.FAR;
        cell.mountainChainAlpha = this.mountainChainAlpha.compute(x, z, 0);
        
        this.region.apply(cell, x, z);
        
        float mountainMask = NoiseUtil.map(cell.mountainChainAlpha, 0.45F, 0.65F);
        cell.terrainMask = Math.min(cell.terrainMask + mountainMask, 1.0F);
        
        this.terrain.apply(cell, x * this.terrainFrequency, z * this.terrainFrequency);
	}

	/**
	 * The terrain types alone at another position, without rivers, climate or landforms: cheaper than
	 * {@link #sampleTerrain}, for landforms that only need the lie of the land.
	 */
	public Cell sampleGround(float x, float z) {
		Cell previous = this.cellProvider.getCacheCell();
		Cell cell = new Cell();
		this.cellProvider.setCacheCell(cell);
		try {
			this.applyContinent(cell, x, z);
			this.applyTerrainTypes(cell, x, z);
			return cell;
		} finally {
			this.cellProvider.setCacheCell(previous);
		}
	}

	/**
	 * The terrain types, rivers and climate at another position, without landforms, e.g. for the ground a landform
	 * stands on.
	 */
	public Cell sampleTerrain(float x, float z) {
		Cell previous = this.cellProvider.getCacheCell();
		Cell cell = new Cell();
		this.cellProvider.setCacheCell(cell);
		try {
			this.applyContinent(cell, x, z);
			this.applyTerrainTypes(cell, x, z);
			Rivermap.get(cell, null, this).apply(cell, x, z);
			this.applyClimate(cell, x, z);
			return cell;
		} finally {
			this.cellProvider.setCacheCell(previous);
		}
	}
	
	public void applyClimate(Cell cell, float x, float z) {
        this.climate.apply(cell, x, z);

        // the size of the terrain's weirdness sets vanilla's slice (valley, low, middle, high or peak), and its sign picks
        // the slice's normal or variant biome: plains or sunflower plains, meadow or cherry grove, jungle or bamboo jungle.
        // Vanilla's weirdness changes sign every so often; here each biome region is one or the other.
        float weirdness = Math.abs(cell.weirdness);
        cell.weirdness = cell.biomeVariant < VARIANT_SHARE ? weirdness : -weirdness;

        if (cell.terrain.isWetland()) {
        	// vanilla's swamps and mangrove swamps are its most eroded lowlands
        	cell.erosion = SWAMP_EROSION;
        } else if (cell.terrain.isRiver()) {
        	// and its rivers the valleys
        	cell.weirdness = 0.0F;
        }
	}
	
	public void applyPost(Cell cell, float x, float z) {
		float deepOcean = this.controlPoints.deepOcean;
		float shallowOcean = this.controlPoints.shallowOcean;
		float beach = this.controlPoints.beach;
		float nearInland = this.controlPoints.nearInland;
		float midInland = this.controlPoints.midInland;
		float farInland = this.controlPoints.farInland;
		
		float continentNoise = cell.continentNoise;
		
		if(continentNoise <= deepOcean || cell.terrain.isDeepOcean()) {
			float alpha = NoiseUtil.map(continentNoise, 0.0F, deepOcean);
			cell.continentalness = Continentalness.DEEP_OCEAN.lerp(alpha);
		} else if(continentNoise <= shallowOcean || cell.terrain.isShallowOcean()) {
			float alpha = NoiseUtil.map(continentNoise, deepOcean, shallowOcean);
			cell.continentalness = Continentalness.OCEAN.lerp(alpha);
		} else if(continentNoise <= nearInland) {
			float alpha = NoiseUtil.map(continentNoise, shallowOcean, nearInland);
			cell.continentalness = Continentalness.NEAR_INLAND.lerp(alpha);
		} else if(continentNoise <= midInland) {
			float alpha = NoiseUtil.map(continentNoise, nearInland, midInland);
			cell.continentalness = NoiseUtil.lerp(Continentalness.MID_INLAND.min(), Continentalness.MID_INLAND.max(), alpha);
		} else {
			float alpha = NoiseUtil.map(continentNoise, midInland, farInland);
			alpha = Math.min(alpha, 1.0F);
			cell.continentalness = NoiseUtil.lerp(Continentalness.FAR_INLAND.min(), Continentalness.FAR_INLAND.max(), alpha);
		}
		
		if(cell.terrain.getDelegate() == TerrainCategory.BEACH && cell.height + this.beachAlpha.compute(x, z, 0) < this.levels.water(5)) {
			float alpha = NoiseUtil.clamp(cell.continentEdge, shallowOcean, beach);
			alpha = NoiseUtil.lerp(alpha, shallowOcean, beach, 0.0F, 1.0F);
			cell.continentalness = NoiseUtil.lerp(Continentalness.COAST.min(), Continentalness.COAST.max(), alpha);
		}

		// land the continent counts as sea, such as islands and peninsulas, is still land: with the sea's climate it got
		// ocean biomes, and ocean structures such as shipwrecks and ruins on dry ground. Coast at the water's edge,
		// inland from a few blocks up.
		if(cell.continentalness < Continentalness.COAST.min() && cell.height >= this.levels.ground) {
			float alpha = NoiseUtil.map(cell.height, this.levels.ground, this.levels.ground(ISLAND_INLAND));
			cell.continentalness = NoiseUtil.lerp(Continentalness.COAST.lerp(0.5F), Continentalness.NEAR_INLAND.lerp(0.5F), alpha);
		}
	}
	
	public static Heightmap make(GeneratorContext ctx) {
        Preset preset = ctx.preset;
        WorldSettings worldSettings = ctx.preset.world();
        ControlPoints controlPoints = worldSettings.controlPoints;

        TerrainSettings terrainSettings = preset.terrain();
        TerrainSettings.General general = terrainSettings.general;
        float globalVerticalScale = general.globalVerticalScale;
        
        Seed regionWarp = ctx.seed.offset(8934);
        int regionWarpScale = 400;
        int regionWarpStrength = 200;
        
        RegionConfig regionConfig = new RegionConfig(
        	ctx.seed.root() + 789124, 
        	general.terrainRegionSize, 
        	Noises.simplex(regionWarp.next(), regionWarpScale, 1),
        	Noises.simplex(regionWarp.next(), regionWarpScale, 1), 
        	regionWarpStrength
        );
        Levels levels = ctx.levels;
        float terrainFrequency = 1.0F / terrainSettings.general.globalHorizontalScale;
        CellPopulator region = new RegionModule(regionConfig);

        Seed mountainSeed = ctx.seed.offset(general.terrainSeedOffset);
        Noise mountainChainAlpha = Noises.worleyEdge(mountainSeed.next(), 1000, EdgeFunction.DISTANCE_2_ADD, DistanceFunction.EUCLIDEAN);
        mountainChainAlpha = Noises.warpPerlin(mountainChainAlpha, mountainSeed.next(), 333, 2, 250.0F);
        mountainChainAlpha = Noises.curve(mountainChainAlpha, Interpolation.CURVE3);
        mountainChainAlpha = Noises.clamp(mountainChainAlpha, 0.0F, 0.9F);
        mountainChainAlpha = Noises.map(mountainChainAlpha, 0.0F, 1.0F);

        int groundVariance = 25;
		Noise ground = Noises.cell(CellField.CONTINENT_NOISE);
		ground = Noises.clamp(ground, controlPoints.coast, controlPoints.farInland);
		ground = Noises.map(ground, 0.0F, 1.0F);
		ground = Noises.mul(ground, levels.scale(groundVariance));
		ground = Noises.add(ground, levels.ground);
		
        CellPopulator terrainRegions = new RegionSelector(TerrainProvider.generateTerrain(ctx.seed, terrainSettings, regionConfig, levels, ground));
        CellPopulator terrainRegionBorders = Populators.makeBorder(ctx.seed, ground, terrainSettings.plains, terrainSettings.steppe, globalVerticalScale);
        
        CellPopulator terrainBlend = new RegionLerper(terrainRegionBorders, terrainRegions);
        CellPopulator mountains = Populators.makeMountainChain(mountainSeed, ground, terrainSettings.mountains, globalVerticalScale, general.fancyMountains);
        // with its coastline reshaped: the natural shoreline lies just past the beach control point
        Continent continent = CoastShaper.wrap(worldSettings.continent.continentType.create(ctx.seed, ctx), ctx.seed.root() + 71237, controlPoints.beach + 0.011F, preset.coasts());
        Climate climate = Climate.make(continent, ctx);
        CellPopulator land = new MountainChainPopulator(terrainBlend, mountains, 0.3F, 0.8F);
        
        CellPopulator deepOcean = Populators.makeDeepOcean(ctx.seed.next(), levels.water);
        CellPopulator shallowOcean = Populators.makeShallowOcean(ctx.levels);
        CellPopulator coast = Populators.makeCoast(ctx.levels);
        
        CellPopulator oceans = new ContinentLerper3(deepOcean, shallowOcean, coast, controlPoints.deepOcean, controlPoints.shallowOcean, controlPoints.coast);
        CellPopulator terrain = new ContinentLerper2(oceans, land, controlPoints.shallowOcean, controlPoints.nearInland);
       
        Noise beachNoise = Noises.perlin2(ctx.seed.next(), 20, 1);
        beachNoise = Noises.mul(beachNoise, ctx.levels.scale(5));
        
        // the natural shoreline lies just past the beach control point
        Landform landforms = Landforms.make(ctx.seed.offset(55123), preset.landforms(), preset.coasts(), preset.oceans(), levels, controlPoints.beach + 0.011F);

        CellSampler.Provider cellProvider = new CellSampler.Provider();
        return new Heightmap(cellProvider, terrain.mapNoise(cellProvider), region, continent, climate, levels, controlPoints, terrainFrequency, mountainChainAlpha, beachNoise, landforms.mapNoise(cellProvider));
	}
}
