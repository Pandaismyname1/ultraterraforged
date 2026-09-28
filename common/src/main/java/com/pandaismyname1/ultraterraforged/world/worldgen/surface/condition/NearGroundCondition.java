package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.UTFMaterialContext;

/**
 * Within the given number of blocks under the ground the terrain was generated with, and no more than a block above
 * it: the surface of the land or the sea floor, not the floor of a cave. Unlike vanilla's preliminary surface, it holds
 * on the deep sea floor too.
 */
class NearGroundCondition extends MaterialRuleContext.LazyYCondition {
	private final int depth;
	@Nullable
	private final ChunkCellReader cells;
	@Nullable
	private final GeneratorContext generatorContext;

	NearGroundCondition(MaterialRuleContext context, int depth) {
		super(context);
		this.depth = depth;
		this.generatorContext = UTFMaterialContext.generatorContext(context);
		this.cells = this.generatorContext != null ? new ChunkCellReader(this.generatorContext) : null;
	}

	@Override
	protected boolean compute() {
		if (this.cells == null) {
			return false;
		}
		int ground = this.generatorContext.levels.scale(this.cells.getCell(this.context.blockX(), this.context.blockZ()).height);
		int y = this.context.blockY();
		return y <= ground + 1 && y >= ground - this.depth;
	}

	public record Source(int depth) implements MaterialCondition {
		public static final Codec<Source> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("depth").forGetter(Source::depth)
		).apply(instance, Source::new));

		@Override
		public NearGroundCondition compile(MaterialRuleContext ctx) {
			return new NearGroundCondition(ctx, this.depth);
		}

		@Override
		public MapCodec<Source> codec() {
			return UTFCodecs.asMap(CODEC);
		}
	}
}
