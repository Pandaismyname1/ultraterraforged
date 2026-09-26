package raccoonman.reterraforged.preset.option;

public class BoolOption extends Option<Boolean> {

	private BoolOption(Builder builder) {
		super(builder);
	}

	public static Builder builder(String path) {
		return new Builder(path);
	}

	public static class Builder extends Option.Builder<Boolean, Builder> {

		private Builder(String path) {
			super(path);
		}

		@Override
		public BoolOption build() {
			return new BoolOption(this);
		}
	}
}
