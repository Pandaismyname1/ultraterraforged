package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;

@Mixin(ChunkGenerator.class)
class MixinChunkGenerator {

	// a locate (the command, explorer maps, eyes of ender, dolphins) only looks: it asks for the height in many places
	// nobody has been near, and a quick guess at their terrain does there, where making each one's tile would take a
	// while. The structures it finds are placed later with the real terrain
	@WrapMethod(method = "findNearestMapStructure")
	private Pair<BlockPos, Holder<Structure>> ultraterraforged$findNearestMapStructure(ServerLevel level, HolderSet<Structure> structures, BlockPos pos, int searchRadius, boolean skipKnownStructures, Operation<Pair<BlockPos, Holder<Structure>>> original) {
		return WorldGenFlags.approximately(() -> original.call(level, structures, pos, searchRadius, skipKnownStructures));
	}
}
