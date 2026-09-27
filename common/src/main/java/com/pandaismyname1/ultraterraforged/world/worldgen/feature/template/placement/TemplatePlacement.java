package com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.placement;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.template.Dimensions;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.template.TemplateContext;

public interface TemplatePlacement<T extends TemplateContext> {
    public static final Codec<TemplatePlacement<?>> CODEC = UTFCodecs.dispatch(UTFBuiltInRegistries.TEMPLATE_PLACEMENT_TYPE, TemplatePlacement::codec);
    
    boolean canPlaceAt(LevelAccessor world, BlockPos pos, Dimensions dimensions);

    boolean canReplaceAt(LevelAccessor world, BlockPos pos);
    
    T createContext();
    
    Codec<? extends TemplatePlacement<T>> codec();
}
