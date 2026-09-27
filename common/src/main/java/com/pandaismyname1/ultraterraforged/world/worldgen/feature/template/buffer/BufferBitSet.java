package com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.buffer;

import java.util.BitSet;

public class BufferBitSet {
    private int minX;
    private int minY;
    private int minZ;
    private int sizeX;
    private int sizeY;
    private int sizeZ;
    private int sizeXZ;
    private BitSet bitSet;

    public void set(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        // the bounds of a template with no blocks lie at the ends of the int range, and their difference overflows
        long sizeX = (long) Math.max(x1, x2) - this.minX;
        long sizeY = (long) Math.max(y1, y2) - this.minY;
        long sizeZ = (long) Math.max(z1, z2) - this.minZ;
        long volume = sizeX * sizeY * sizeZ;
        if (sizeX < 0 || sizeY < 0 || sizeZ < 0 || volume > Integer.MAX_VALUE) {
        	sizeX = sizeY = sizeZ = volume = 0;
        }
        this.sizeX = (int) sizeX;
        this.sizeY = (int) sizeY;
        this.sizeZ = (int) sizeZ;
        this.sizeXZ = this.sizeX * this.sizeZ;
        int size = (int) volume;
        if (this.bitSet == null || this.bitSet.length() < size) {
        	this.bitSet = new BitSet(size);
        } else {
        	this.bitSet.clear();
        }
    }

    public void clear() {
        if (this.bitSet != null) {
        	this.bitSet.clear();
        }
    }

    public void set(int x, int y, int z) {
        int index = this.indexOf(x - this.minX, y - this.minY, z - this.minZ);
        if (index >= 0) {
        	this.bitSet.set(index);
        }
    }

    public void unset(int x, int y, int z) {
        int index = this.indexOf(x - this.minX, y - this.minY, z - this.minZ);
        this.bitSet.set(index, false);
    }

    public boolean test(int x, int y, int z) {
        int index = this.indexOf(x - this.minX, y - this.minY, z - this.minZ);
        if (index < 0 || index >= this.bitSet.length()) {
            return false;
        }
        return this.bitSet.get(index);
    }

    private int indexOf(int x, int y, int z) {
        return (y * this.sizeXZ) + (z * this.sizeX) + x;
    }
}
