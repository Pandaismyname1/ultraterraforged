package com.pandaismyname1.ultraterraforged.world.worldgen.feature.chance;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.WorldGenLevel;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

class BiomeEdgeChanceModifier extends RangeChanceModifier {
	public static final Codec<BiomeEdgeChanceModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.FLOAT.fieldOf("from").forGetter((o) -> o.from),
		Codec.FLOAT.fieldOf("to").forGetter((o) -> o.to),
		Codec.BOOL.fieldOf("exclusive").forGetter((o) -> o.exclusive)
	).apply(instance, BiomeEdgeChanceModifier::new));
	
	public BiomeEdgeChanceModifier(float from, float to, boolean exclusive) {
		super(from, to, exclusive);
	}

	@Override
	public Codec<BiomeEdgeChanceModifier> codec() {
		return CODEC;
	}

	@Override
	protected float getValue(ChanceContext chanceCtx, WorldGenLevel level, BlockPos pos) {
		@Nullable
		GeneratorContext generatorContext;
		if((Object) level.getLevel().getChunkSource().randomState() instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			int x = pos.getX();
			int z = pos.getZ();
			int chunkX = SectionPos.blockToSectionCoord(x);
			int chunkZ = SectionPos.blockToSectionCoord(z);
			Tile.Chunk chunk = generatorContext.cache.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ);
			return chunk.getCell(x, z).biomeRegionEdge;
		} else {
			throw new UnsupportedOperationException();
		}
	}
}
