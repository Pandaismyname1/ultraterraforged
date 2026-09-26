package raccoonman.reterraforged.preset.option;

public class IntOption extends NumberOption<Integer> {
	private final int step;
	private final boolean seed;

	private IntOption(Builder builder) {
		super(builder);
		this.step = builder.step;
		this.seed = builder.seed;
	}

	public int step() {
		return this.step;
	}

	/**
	 * Seed offsets are arbitrary ints that UIs offer to randomize rather than slide.
	 */
	public boolean isSeed() {
		return this.seed;
	}

	@Override
	protected Integer snap(Integer value) {
		if (this.step <= 1) {
			return value;
		}
		// round down to a multiple of the step, but never below the minimum
		int snapped = Math.floorDiv(value, this.step) * this.step;
		return Math.max(snapped, this.min());
	}

	public static Builder builder(String path) {
		return new Builder(path);
	}

	public static class Builder extends NumberOption.Builder<Integer, Builder> {
		private int step = 1;
		private boolean seed;

		private Builder(String path) {
			super(path);
		}

		public Builder step(int step) {
			this.step = step;
			return this;
		}

		public Builder seed() {
			this.seed = true;
			return this.range(Integer.MIN_VALUE, Integer.MAX_VALUE);
		}

		@Override
		public IntOption build() {
			return new IntOption(this);
		}
	}
}
