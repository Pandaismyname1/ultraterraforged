package com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

record ClampToNearestUnit(DensityFunction function, int resolution) implements DensityFunction {
	public static final Codec<ClampToNearestUnit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		DensityFunction.HOLDER_HELPER_CODEC.fieldOf("function").forGetter(ClampToNearestUnit::function),
		Codec.INT.fieldOf("resolution").forGetter(ClampToNearestUnit::resolution)
	).apply(instance, ClampToNearestUnit::new));
	
	@Override
	public double compute(FunctionContext ctx) {
		return this.computeClamped(this.function.compute(ctx));
	}

	@Override
	public void fillArray(double[] arr, ContextProvider ctx) {
		this.function.fillArray(arr, ctx);
		for(int i = 0; i < arr.length; i++) {
			arr[i] = this.computeClamped(arr[i]);
		}
	}

	@Override
	public DensityFunction mapAll(Visitor visitor) {
		return visitor.apply(new ClampToNearestUnit(this.function.mapAll(visitor), this.resolution));
	}

	@Override
	public double minValue() {
		return this.clampedBound(this.function.minValue());
	}

	@Override
	public double maxValue() {
		return this.clampedBound(this.function.maxValue());
	}

	@Override
	public KeyDispatchDataCodec<ClampToNearestUnit> codec() {
		return UTFCodecs.keyDispatch(CODEC);
	}
	
	// an unbounded function stays unbounded: rounding an infinite bound overflowed, and the maximum came out a large
	// negative number, which density compilers that go by bounds (Generator Accelerator's) took at its word
	private double clampedBound(double value) {
		return Double.isInfinite(value) ? value : this.computeClamped(value);
	}

	private double computeClamped(double value) {
		float scaled = (int) (value * this.resolution) + 1;
		return (scaled / this.resolution);
	}
}
