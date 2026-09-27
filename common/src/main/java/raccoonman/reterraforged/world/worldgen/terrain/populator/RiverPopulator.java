package raccoonman.reterraforged.world.worldgen.terrain.populator;

import java.util.Random;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.function.CurveFunction;
import raccoonman.reterraforged.world.worldgen.noise.function.CurveFunctions;
import raccoonman.reterraforged.world.worldgen.noise.module.Line;
import raccoonman.reterraforged.world.worldgen.rivermap.RiverRoutes;
import raccoonman.reterraforged.world.worldgen.rivermap.river.Range;
import raccoonman.reterraforged.world.worldgen.rivermap.river.River;
import raccoonman.reterraforged.world.worldgen.rivermap.river.RiverConfig;
import raccoonman.reterraforged.world.worldgen.rivermap.river.RiverWarp;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

public class RiverPopulator implements Comparable<RiverPopulator> {
    // above the sea, the banks rise this many blocks above the water at their edge
    private static final float BANK_RISE = 2.0F;
    // and beyond it the valley sides rise no steeper than this, in blocks per block, so gorges can be climbed
    private static final float SIDE_SLOPE = 1.0F;
    // the outer part of the banks, as a share of their width, is raised into a low embankment where the land beside a
    // river is lower than its water
    private static final float LEVEE = 0.3F;
    public boolean main;
    private boolean connecting;
    private float fade;
    private float fadeInv;
    private Range bedWidth;
    private Range banksWidth;
    private Range valleyWidth;
    private Range bedDepth;
    private Range banksDepth;
    private float waterLine;
    public River river;
    public RiverWarp warp;
    public RiverConfig config;
    public CurveFunction valleyCurve;
    // where along its parent this fork flows in, or -1 for a river that runs to the sea
    public float junction = -1.0F;
    private Levels levels;
    // the water's surface from the source to the mouth, for rivers above the sea; null for rivers at sea level
    private float[] waterLevels;
    // how far the river is moved sideways off its straight line, from the source to the mouth, so it follows the
    // land; null if it isn't
    private float[] route;
    // map places in the river's own space, and in the space its forks, lakes and wetlands lie in, to the world; set
    // once the rivermap is prepared
    public RiverRoutes.Frame frame;
    public RiverRoutes.Frame innerFrame;
    
    public RiverPopulator(River river, RiverWarp warp, RiverConfig config, Settings settings, Levels levels) {
        this.fade = settings.fadeIn;
        this.fadeInv = 1.0F / settings.fadeIn;
        this.bedWidth = new Range(0.25F, config.bedWidth * config.bedWidth);
        this.banksWidth = new Range(1.5625F, config.bankWidth * config.bankWidth);
        this.valleyWidth = new Range(settings.valleySize * settings.valleySize, settings.valleySize * settings.valleySize);
        this.river = river;
        this.warp = warp;
        this.config = config;
        this.main = config.main;
        this.connecting = settings.connecting;
        this.waterLine = levels.water;
        this.bedDepth = new Range(levels.water, config.bedHeight);
        this.banksDepth = new Range(config.minBankHeight, config.maxBankHeight);
        this.valleyCurve = settings.valleyCurve;
        this.levels = levels;
    }
    
    public void setWaterLevels(float[] waterLevels) {
    	this.waterLevels = waterLevels;
    }
    
    public void setRoute(float[] route) {
    	this.route = route;
    }

    // how far the river is moved sideways off its line at this point along it, towards its normal
    public float routeOffset(float t) {
    	if (this.route == null) {
    		return 0.0F;
    	}
    	float position = NoiseUtil.clamp(t, 0.0F, 1.0F) * (this.route.length - 1);
    	int index = Math.min((int) position, this.route.length - 2);
    	return NoiseUtil.lerp(this.route[index], this.route[index + 1], position - index);
    }

    public boolean hasWaterLevels() {
    	return this.waterLevels != null;
    }
    
    // the water's surface at this point along the river, from 0 at the source to 1 at the mouth
    public float waterLevelAt(float t) {
    	return this.waterLevels == null ? this.waterLine : sample(this.waterLevels, t);
    }
    
    // the water stays level from one sample to the next, so it changes in steps: small waterfalls
    private static float sample(float[] values, float t) {
    	int index = (int) (NoiseUtil.clamp(t, 0.0F, 1.0F) * (values.length - 1));
    	return values[Math.min(index, values.length - 1)];
    }

    @Override
    public int compareTo(RiverPopulator o) {
        return Integer.compare(this.config.order, o.config.order);
    }
    
    public void apply(Cell cell, float px, float pz, float pt, float x, float z, float t) {
        float d2 = this.getDistance2(x, z, t);
        float pd2 = this.getDistance2(px, pz, pt);
        float valleyAlpha = this.getDistanceAlpha(pt, Math.min(d2, pd2), this.valleyWidth);
        if (valleyAlpha == 0.0F) {
            return;
        }
        float bankHeight = this.getScaledSize(t, this.banksDepth);
        valleyAlpha = this.valleyCurve.apply(valleyAlpha);
        
        float mouthModifier = getMouthModifier(cell);
        // the bed lies as far below the river's own water as it would below the sea
        float level = this.waterLevelAt(t);
        float bedHeight = this.getScaledSize(t, this.bedDepth) + (level - this.waterLine);
        
        float riverDistance = Math.min(cell.riverDistance, 1.0F - valleyAlpha);
        cell.riverDistance = riverDistance;
//        cell.terrainMask *= riverDistance;
        
//        cell.height = Math.min(NoiseUtil.lerp(cell.height, bankHeight, valleyAlpha), cell.height);

        float banks = d2 * mouthModifier;
        float banksSize = this.getScaledSize(t, this.banksWidth);
        float bank = (float) Math.sqrt(banks / banksSize);
        if (bank < cell.riverBank) {
        	cell.riverBank = bank;
        	cell.riverWidth = (float) Math.sqrt(banksSize);
        }
        if (this.waterLevels != null) {
        	this.carveRaised(cell, banks, banksSize, bedHeight, level);
        	return;
        }
        if (banks >= banksSize) {
            return;
        }
        float banksAlpha = 1.0F - banks / banksSize;
        if (cell.height > bedHeight) {
            cell.height = Math.min(NoiseUtil.lerp(cell.height, bedHeight, banksAlpha), cell.height);
            this.tag(cell, bedHeight, level);
        }
        
        float bedAlpha = this.getDistanceAlpha(t, d2, this.bedWidth);

        if (bedAlpha != 0.0F && cell.height > bedHeight) {
            cell.height = NoiseUtil.lerp(cell.height, bedHeight, bedAlpha);
            this.tag(cell, bedHeight, level);
        }
    }
    
    // a channel down to the bed, as below the sea, with banks rising a little above the water at its edge; beyond that
    // the valley's sides rise at a climbable slope until they meet the land, so a river cutting through high ground
    // runs in a valley as wide as it is deep
    private void carveRaised(Cell cell, float banks, float banksSize, float bedHeight, float level) {
    	float width = (float) Math.sqrt(banksSize);
    	float distance = (float) Math.sqrt(banks);
    	float bankTop = level + BANK_RISE * this.levels.unit;
    	float target;
    	if (distance < width) {
    		target = NoiseUtil.lerp(bedHeight, bankTop, banks / banksSize);
    	} else {
    		target = bankTop + (distance - width) * SIDE_SLOPE * this.levels.unit;
    	}
    	if (cell.height > target) {
    		cell.height = target;
    		this.tag(cell, bedHeight, level);
    	}
    	if (level <= this.levels.water(1)) {
    		return;
    	}
    	// (not across another river's water, like the parent a fork flows into)
    	boolean water = cell.waterLevel > 0.0F || cell.terrain.isRiver() || cell.terrain.isLake() || cell.height < this.waterLine;
    	if (distance > width * (1.0F - LEVEE) && distance < width + 2.0F && cell.height < level + this.levels.unit && !water) {
    		// the land beside the river is lower than its water: an embankment keeps it in
    		cell.height = level + this.levels.unit;
    		cell.erosionMask = true;
    	} else if (cell.height < level && distance < width) {
    		// only the channel holds water; land lower than the river further out stays dry
    		cell.waterLevel = cell.waterLevel > 0.0F ? Math.min(cell.waterLevel, level) : level;
    		// erosion would pile the bed up into islands
    		cell.erosionMask = true;
    	}
    }

    public RiverConfig createForkConfig(float t, Levels levels) {
        int bedHeight = levels.scale(this.getScaledSize(t, this.bedDepth));
        int bedWidth = (int) Math.round(Math.sqrt(this.getScaledSize(t, this.bedWidth)) * 0.75);
        int bankWidth = (int) Math.round(Math.sqrt(this.getScaledSize(t, this.banksWidth)) * 0.75);
        bedWidth = Math.max(1, bedWidth);
        bankWidth = Math.max(bedWidth + 1, bankWidth);
        return this.config.createFork(bedHeight, bedWidth, bankWidth, levels);
    }
    
    private float getDistance2(float x, float y, float t) {
        if (t <= 0.0F) {
            return Line.distSq(x, y, this.river.x1, this.river.z1);
        }
        if (t >= 1.0F) {
            return Line.distSq(x, y, this.river.x2, this.river.z2);
        }
        float px = this.river.x1 + t * this.river.dx;
        float py = this.river.z1 + t * this.river.dz;
        return Line.distSq(x, y, px, py);
    }
    
    private float getDistanceAlpha(float t, float dist2, Range range) {
        float size2 = this.getScaledSize(t, range);
        if (dist2 >= size2) {
            return 0.0F;
        }
        return 1.0F - dist2 / size2;
    }
    
    private float getScaledSize(float t, Range range) {
        if (t < 0.0F) {
            return range.min();
        }
        if (t > 1.0F) {
            return range.max();
        }
        if (range.min() == range.max()) {
            return range.min();
        }
        if (t >= this.fade) {
            return range.max();
        }
        return NoiseUtil.lerp(range.min(), range.max(), t * this.fadeInv);
    }
    
    private void tag(Cell cell, float bedHeight, float level) {
        if (cell.terrain.overridesRiver() && (cell.height < bedHeight || cell.height > level)) {
            return;
        }
        cell.erosionMask = true;
        // under the water; above the sea, land level with the water is dry
        if (level > this.waterLine ? cell.height < level : cell.height <= level) {
            cell.terrain = TerrainType.RIVER;
        }
    }
    
    private static float getMouthModifier(Cell cell) {
        float modifier = NoiseUtil.map(cell.continentEdge, 0.0F, 0.5F, 0.5F);
        modifier *= modifier;
        return modifier;
    }
    
    public static CurveFunction getValleyType(Random random) {
        int value = random.nextInt(100);
        if (value < 5) {
            return CurveFunctions.scurve(0.4F, 1.0F);
        }
        if (value < 30) {
            return CurveFunctions.scurve(4.0F, 5.0F);
        }
        if (value < 50) {
            return CurveFunctions.scurve(3.0F, 0.25F);
        }
        return CurveFunctions.scurve(2.0F, -0.5F);
    }
    
    public static RiverPopulator create(float x1, float z1, float x2, float z2, RiverConfig config, Levels levels, Random random) {
        River river = new River(x1, z1, x2, z2);
        RiverWarp warp = RiverWarp.create(0.35F, random);
        float valleyWidth = 275.0F * River.MAIN_VALLEY.next(random);
        Settings settings = creatSettings(random);
        settings.connecting = false;
        settings.fadeIn = config.fade;
        settings.valleySize = valleyWidth;
        return new RiverPopulator(river, warp, config, settings, levels);
    }
    
    private static Settings creatSettings(Random random) {
        Settings settings = new Settings();
        settings.valleyCurve = getValleyType(random);
        return settings;
    }
    
    public static class Settings {
        public float valleySize;
        public float fadeIn;
        public boolean connecting;
        public CurveFunction valleyCurve;
        
        public Settings() {
            this.valleySize = 275.0F;
            this.fadeIn = 0.7F;
            this.connecting = false;
            this.valleyCurve = CurveFunctions.scurve(2.0F, -0.5F);
        }
    }
}
