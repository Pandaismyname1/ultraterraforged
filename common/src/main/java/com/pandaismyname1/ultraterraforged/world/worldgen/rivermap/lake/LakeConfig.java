package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.lake;

import com.pandaismyname1.ultraterraforged.data.preset.settings.RiverSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;

public class LakeConfig {
    public float depth;
    public float chance;
    public float sizeMin;
    public float sizeMax;
    public float sizeRange;
    public float bankMin;
    public float bankMax;
    // the sea level these heights are measured from
    public float water;
    public float unit;
    
    private LakeConfig(Builder builder) {
        this.depth = builder.depth;
        this.chance = builder.chance;
        this.sizeMin = builder.sizeMin;
        this.sizeMax = builder.sizeMax;
        this.sizeRange = this.sizeMax - this.sizeMin;
        this.bankMin = builder.bankMin;
        this.bankMax = builder.bankMax;
        this.water = builder.water;
        this.unit = builder.unit;
    }
    
    public static LakeConfig of(RiverSettings.Lake settings, Levels levels) {
        Builder builder = new Builder();
        builder.chance = settings.chance;
        builder.sizeMin = settings.sizeMin;
        builder.sizeMax = settings.sizeMax;
        builder.depth = levels.water(-settings.depth);
        builder.bankMin = levels.water(settings.minBankHeight);
        builder.bankMax = levels.water(settings.maxBankHeight);
        builder.water = levels.water;
        builder.unit = levels.unit;
        return new LakeConfig(builder);
    }
    
    public static class Builder {
        public float chance;
        public float depth;
        public float sizeMin;
        public float sizeMax;
        public float bankMin;
        public float bankMax;
        public float water;
        public float unit;
        
        public Builder() {
            this.depth = 10.0F;
            this.sizeMin = 30.0F;
            this.sizeMax = 100.0F;
            this.bankMin = 1.0F;
            this.bankMax = 8.0F;
        }
    }
}
