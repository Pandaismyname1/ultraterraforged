package com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.decorator;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;

import com.mojang.serialization.Codec;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import com.pandaismyname1.ultraterraforged.registries.UTFBuiltInRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.template.TemplateContext;

public interface TemplateDecorator<T extends TemplateContext> {
    public static final Codec<TemplateDecorator<?>> CODEC = UTFCodecs.dispatch(UTFBuiltInRegistries.TEMPLATE_DECORATOR_TYPE, TemplateDecorator::codec);
    
    void apply(LevelAccessor level, T buffer, RandomSource random, boolean modified);
    
    Codec<? extends TemplateDecorator<T>> codec();
}
