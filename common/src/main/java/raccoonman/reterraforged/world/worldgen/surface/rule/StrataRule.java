package raccoonman.reterraforged.world.worldgen.surface.rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.surface.RTFSurfaceSystem;

/**
 * Replaces stone with layers of different rock, like the bands in a canyon wall. The layers run roughly level but
 * rise, fall and thicken gently across the land, and each strata region has its own sequence of rocks.
 *
 * @param selector picks the region's stack, values from its minimum to its maximum spread over all stacks
 * @param offset how far the layers are raised or lowered at a position, in blocks
 * @param thickness how much the layers are stretched at a position, 1 keeps them as generated
 * @param materials the rocks to layer; mods' stones join through the vanilla stone tags this tag includes
 * @param excluded rocks never to layer, even though a mod tagged them as stone
 * @param minY below this the rule leaves the stone alone, e.g. for the deepslate transition
 */
public record StrataRule(ResourceLocation cacheId, Holder<Noise> selector, Holder<Noise> offset, Holder<Noise> thickness, TagKey<Block> materials, TagKey<Block> excluded, int variants, int minThickness, int maxThickness, int minY) implements SurfaceRules.RuleSource {
	public static final Codec<StrataRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("cache_id").forGetter(StrataRule::cacheId),
		Noise.CODEC.fieldOf("selector").forGetter(StrataRule::selector),
		Noise.CODEC.fieldOf("offset").forGetter(StrataRule::offset),
		Noise.CODEC.fieldOf("thickness").forGetter(StrataRule::thickness),
		TagKey.hashedCodec(Registries.BLOCK).fieldOf("materials").forGetter(StrataRule::materials),
		TagKey.hashedCodec(Registries.BLOCK).fieldOf("excluded").forGetter(StrataRule::excluded),
		Codec.intRange(1, 1024).fieldOf("variants").forGetter(StrataRule::variants),
		Codec.intRange(1, 256).fieldOf("min_thickness").forGetter(StrataRule::minThickness),
		Codec.intRange(1, 256).fieldOf("max_thickness").forGetter(StrataRule::maxThickness),
		Codec.INT.fieldOf("min_y").forGetter(StrataRule::minY)
	).apply(instance, StrataRule::new));

	// how far the stacks reach past the build limits, so offset and stretched layers never run out
	private static final int MARGIN = 128;
	// layers are stretched around this height, so they stay put near sea level and tilt gently above and below
	private static final int PIVOT_Y = 64;

	@Override
	public SurfaceRules.SurfaceRule apply(Context ctx) {
		if (!((Object) ctx.system instanceof RTFSurfaceSystem surfaceSystem) || !((Object) ctx.randomState instanceof RTFRandomState randomState)) {
			throw new IllegalStateException("Strata need ReTerraForged's surface system");
		}
		int height = ctx.chunk.getMaxBuildHeight() - this.minY + MARGIN * 2;
		List<StrataStack> stacks = surfaceSystem.getOrCreateStrata(this.cacheId, (random) -> this.generate(random, height));
		if (stacks.isEmpty()) {
			return (x, y, z) -> null;
		}
		// the noises are seeded by the world, so every world has its own regions and folds
		return new Rule(stacks, randomState.wrap(this.selector.value()), randomState.wrap(this.offset.value()), randomState.wrap(this.thickness.value()));
	}

	@Override
	public KeyDispatchDataCodec<StrataRule> codec() {
		return new KeyDispatchDataCodec<>(CODEC);
	}

	private List<StrataStack> generate(RandomSource random, int height) {
		List<BlockState> materials = this.findMaterials();
		RTFCommon.LOGGER.info("Rock layers use {}", materials.stream().map((state) -> BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()).toList());
		if (materials.isEmpty()) {
			return List.of();
		}
		List<StrataStack> stacks = new ArrayList<>(this.variants);
		for (int i = 0; i < this.variants; i++) {
			// layers can be stretched to half their thickness at the least, see PresetStrataNoise
			stacks.add(StrataStack.generate(random, materials, height * 2, this.minThickness, Math.max(this.minThickness, this.maxThickness)));
		}
		return stacks;
	}

	/**
	 * Every block in the materials tag that can stand in for stone. Mods tag all sorts of things as stone, so skip what
	 * would break or look wrong in a thick layer. Sorted by id, so the layers don't depend on the order mods load in.
	 */
	private List<BlockState> findMaterials() {
		Set<Block> blocks = new TreeSet<>(Comparator.comparing((Block block) -> BuiltInRegistries.BLOCK.getKey(block)));
		BuiltInRegistries.BLOCK.getTag(this.materials).ifPresent((tag) -> tag.forEach((holder) -> blocks.add(holder.value())));
		List<BlockState> materials = new ArrayList<>();
		for (Block block : blocks) {
			BlockState state = block.defaultBlockState();
			if (state.is(this.excluded) || block instanceof FallingBlock || block instanceof EntityBlock || !state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
				continue;
			}
			materials.add(state);
		}
		return materials;
	}

	private class Rule implements SurfaceRules.SurfaceRule {
		private final List<StrataStack> stacks;
		private final Noise selector;
		private final Noise offset;
		private final Noise thickness;
		private final float selectorMin;
		private final float selectorRange;
		@Nullable
		private StrataStack stack;
		private float columnOffset;
		private float columnThickness;
		private int lastX = Integer.MIN_VALUE;
		private int lastZ = Integer.MIN_VALUE;

		Rule(List<StrataStack> stacks, Noise selector, Noise offset, Noise thickness) {
			this.stacks = stacks;
			this.selector = selector;
			this.offset = offset;
			this.thickness = thickness;
			this.selectorMin = selector.minValue();
			this.selectorRange = Math.max(1.0E-6F, selector.maxValue() - selector.minValue());
		}

		private void updateColumn(int x, int z) {
			float select = (this.selector.compute(x, z, 0) - this.selectorMin) / this.selectorRange;
			int index = Math.min(this.stacks.size() - 1, Math.max(0, (int) (select * this.stacks.size())));
			this.stack = this.stacks.get(index);
			this.columnOffset = this.offset.compute(x, z, 0);
			this.columnThickness = Math.max(0.5F, this.thickness.compute(x, z, 0));
		}

		@Override
		public BlockState tryApply(int x, int y, int z) {
			if (y < StrataRule.this.minY) {
				return null;
			}
			if (x != this.lastX || z != this.lastZ) {
				this.updateColumn(x, z);
				this.lastX = x;
				this.lastZ = z;
			}
			int minY = StrataRule.this.minY;
			float fromPivot = y + this.columnOffset - PIVOT_Y;
			return this.stack.at(MARGIN + (PIVOT_Y - minY) + fromPivot / this.columnThickness);
		}
	}
}
