package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;

class TerrainCondition extends CellCondition {
	private Set<Terrain> terrain;
	
	public TerrainCondition(MaterialRuleContext context, Set<Terrain> terrain) {
		super(context);
		
		this.terrain = terrain;
	}

	@Override
	public boolean test(Cell cell, int x, int z) {
		return this.terrain.contains(cell.terrain);
	}
	
	public record Source(Set<Terrain> terrain) implements MaterialCondition {
		public static final Codec<Source> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Terrain.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("terrain").forGetter(Source::terrain)
		).apply(instance, Source::new));

		@Override
		public TerrainCondition compile(MaterialRuleContext ctx) {
			return new TerrainCondition(ctx, this.terrain);
		}

		@Override
		public MapCodec<Source> codec() {
			return UTFCodecs.asMap(CODEC);
		}
	}
}
