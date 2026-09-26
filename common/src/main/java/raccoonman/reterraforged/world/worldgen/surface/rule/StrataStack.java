package raccoonman.reterraforged.world.worldgen.surface.rule;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One sequence of rock layers, bottom to top, like the bands in a canyon wall. A world has a number of these, and
 * each strata region uses one.
 */
public final class StrataStack {
	// plain stone makes up about this share of the layers however many rocks mods add, so the underground still
	// reads as stone
	private static final float STONE_SHARE = 0.45F;
	// each stack uses stone and only a few of the other rocks, like a real region's geology; with many mods adding
	// rocks this keeps every region coherent instead of a mix of everything
	private static final int MIN_ROCKS_PER_STACK = 2;
	private static final int MAX_ROCKS_PER_STACK = 4;

	// the height at which each layer ends, counted from the bottom of the stack, in increasing order
	private final int[] tops;
	private final BlockState[] states;

	private StrataStack(int[] tops, BlockState[] states) {
		this.tops = tops;
		this.states = states;
	}

	/**
	 * Stacks layers of the given rocks until they are {@code height} blocks tall. Neighbouring layers are always of
	 * different rock, otherwise the boundary between them wouldn't show.
	 */
	public static StrataStack generate(RandomSource random, List<BlockState> materials, int height, int minThickness, int maxThickness) {
		if (materials.isEmpty()) {
			throw new IllegalArgumentException("No strata materials");
		}
		materials = pickRocks(random, materials);
		long others = materials.stream().filter((material) -> !material.is(Blocks.STONE)).count();
		float stoneWeight = others == 0 ? 1.0F : others * STONE_SHARE / (1.0F - STONE_SHARE);
		float[] weights = new float[materials.size()];
		float total = 0.0F;
		for (int i = 0; i < weights.length; i++) {
			total += materials.get(i).is(Blocks.STONE) ? stoneWeight : 1.0F;
			weights[i] = total;
		}
		List<Integer> tops = new ArrayList<>();
		List<BlockState> states = new ArrayList<>();
		int top = 0;
		BlockState previous = null;
		while (top < height) {
			BlockState state = pick(random, materials, weights, total);
			// a single material can't alternate with itself
			for (int attempt = 0; attempt < 8 && state == previous && materials.size() > 1; attempt++) {
				state = pick(random, materials, weights, total);
			}
			if (state == previous) {
				// only one material, or very unlucky; grow the previous layer instead
				top += minThickness + random.nextInt(maxThickness - minThickness + 1);
				tops.set(tops.size() - 1, top);
				continue;
			}
			top += minThickness + random.nextInt(maxThickness - minThickness + 1);
			tops.add(top);
			states.add(state);
			previous = state;
		}
		return new StrataStack(tops.stream().mapToInt(Integer::intValue).toArray(), states.toArray(BlockState[]::new));
	}

	// plain stone, if available, and a few others
	private static List<BlockState> pickRocks(RandomSource random, List<BlockState> materials) {
		List<BlockState> stone = new ArrayList<>();
		List<BlockState> others = new ArrayList<>();
		for (BlockState material : materials) {
			(material.is(Blocks.STONE) ? stone : others).add(material);
		}
		int count = Math.min(others.size(), MIN_ROCKS_PER_STACK + random.nextInt(MAX_ROCKS_PER_STACK - MIN_ROCKS_PER_STACK + 1));
		// the first few of a shuffled copy
		for (int i = 0; i < count; i++) {
			int swap = i + random.nextInt(others.size() - i);
			BlockState rock = others.get(swap);
			others.set(swap, others.get(i));
			others.set(i, rock);
		}
		List<BlockState> picked = new ArrayList<>(stone);
		picked.addAll(others.subList(0, count));
		return picked;
	}

	private static BlockState pick(RandomSource random, List<BlockState> materials, float[] cumulativeWeights, float total) {
		float value = random.nextFloat() * total;
		for (int i = 0; i < cumulativeWeights.length; i++) {
			if (value < cumulativeWeights[i]) {
				return materials.get(i);
			}
		}
		return materials.get(materials.size() - 1);
	}

	public int layerCount() {
		return this.states.length;
	}

	public int layerTop(int index) {
		return this.tops[index];
	}

	public BlockState layerState(int index) {
		return this.states[index];
	}

	/**
	 * The rock at the given height above the bottom of the stack. Heights past either end continue the outermost
	 * layer.
	 */
	public BlockState at(float height) {
		int low = 0;
		int high = this.tops.length - 1;
		// the first layer whose top is above the height
		while (low < high) {
			int mid = (low + high) >>> 1;
			if (this.tops[mid] > height) {
				high = mid;
			} else {
				low = mid + 1;
			}
		}
		return this.states[low];
	}
}
