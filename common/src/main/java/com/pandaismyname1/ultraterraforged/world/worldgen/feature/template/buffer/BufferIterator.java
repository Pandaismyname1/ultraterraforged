package com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.buffer;

public interface BufferIterator {
    boolean isEmpty();

    boolean next();

    int nextIndex();
}