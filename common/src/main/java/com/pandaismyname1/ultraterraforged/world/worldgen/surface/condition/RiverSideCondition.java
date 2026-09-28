package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;

/**
 * On the bed or banks of a river or lake: within the given share of the width of its water and banks from its middle.
 */
class RiverSideCondition extends CellCondition {
	private final float within;

	public RiverSideCondition(Context context, float within) {
		super(context);
		this.within = within;
	}

	@Override
	public boolean test(Cell cell, int x, int z) {
		return cell.riverBank < this.within;
	}

	public record Source(float within) implements SurfaceRules.ConditionSource {
		public static final Codec<Source> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.fieldOf("within").forGetter(Source::within)
		).apply(instance, Source::new));

		@Override
		public RiverSideCondition apply(Context ctx) {
			return new RiverSideCondition(ctx, this.within);
		}

		@Override
		public MapCodec<Source> codec() {
			return UTFCodecs.asMap(CODEC);
		}
	}
}
