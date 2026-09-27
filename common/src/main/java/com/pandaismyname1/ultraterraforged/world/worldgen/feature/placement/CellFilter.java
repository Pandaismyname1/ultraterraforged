package com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.RTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;

abstract class CellFilter extends PlacementFilter {
	
	@Override
	protected boolean shouldPlace(PlacementContext ctx, RandomSource rand, BlockPos pos) {
		WorldGenLevel level = ctx.getLevel();
		RandomState randomState = level.getLevel().getChunkSource().randomState();
		
		@Nullable
		GeneratorContext generatorContext;
		if((Object) randomState instanceof RTFRandomState rtfRandomState && (generatorContext = rtfRandomState.generatorContext()) != null) {
			Cell cell = new Cell();
			generatorContext.lookup.apply(cell, pos.getX(), pos.getZ());
			return this.shouldPlace(cell, ctx, rand, pos);
		}
		return false;
	}
	
	protected abstract boolean shouldPlace(Cell cell, PlacementContext ctx, RandomSource rand, BlockPos pos);
}
