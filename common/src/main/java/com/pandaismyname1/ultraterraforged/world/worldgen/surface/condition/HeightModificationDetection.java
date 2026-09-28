package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.UTFMaterialContext;

public class HeightModificationDetection extends CellCondition {
	private Target target;

	private HeightModificationDetection(MaterialRuleContext context, Target target) {
		super(context);

		this.target = target;
	}

	@Override
	public boolean test(Cell cell, int blockX, int blockZ) {
		return this.target.test(cell, blockX, blockZ, this.context, this.generatorContext);
	}

	public record Source(Target target) implements MaterialCondition {
		public static final Codec<Source> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Target.CODEC.fieldOf("target").forGetter(Source::target)
		).apply(instance, Source::new));

		@Override
		public HeightModificationDetection compile(MaterialRuleContext ctx) {
			return new HeightModificationDetection(ctx, this.target);
		}

		@Override
		public MapCodec<Source> codec() {
			return UTFCodecs.asMap(CODEC);
		}
	}

	public enum Target implements StringRepresentable {
		STRUCTURE_BEARDIFIER("structure_beardifier") {

			@Override
			public boolean test(Cell cell, int blockX, int blockZ, MaterialRuleContext surfaceContext, GeneratorContext generatorContext) {
				ChunkAccess chunk = UTFMaterialContext.chunk();
				return chunk != null && surfaceContext.blockY() == chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, blockX & 0xF, blockZ & 0xF) && surfaceContext.blockY() != generatorContext.levels.scale(cell.height);
			}
		};

		public static final Codec<Target> CODEC = StringRepresentable.fromEnum(Target::values);

		private String name;

		private Target(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public abstract boolean test(Cell cell, int blockX, int blockZ, MaterialRuleContext surfaceContext, GeneratorContext generatorContext);
	}
}
