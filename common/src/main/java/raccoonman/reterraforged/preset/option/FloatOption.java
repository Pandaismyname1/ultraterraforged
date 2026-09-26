package raccoonman.reterraforged.preset.option;

public class FloatOption extends NumberOption<Float> {

	private FloatOption(Builder builder) {
		super(builder);
	}

	public static Builder builder(String path) {
		return new Builder(path);
	}

	public static class Builder extends NumberOption.Builder<Float, Builder> {

		private Builder(String path) {
			super(path);
		}

		@Override
		public FloatOption build() {
			return new FloatOption(this);
		}
	}
}
