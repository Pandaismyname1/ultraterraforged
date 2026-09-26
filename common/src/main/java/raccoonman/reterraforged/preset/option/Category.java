package raccoonman.reterraforged.preset.option;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

// A titled group of options within a page; labelKey is null for an untitled group
public record Category(String id, @Nullable String labelKey, List<Option<?>> options) {

	public Category {
		options = List.copyOf(options);
	}

	public Optional<String> label() {
		return Optional.ofNullable(this.labelKey);
	}

	public static Category of(String id, @Nullable String labelKey, Option<?>... options) {
		return new Category(id, labelKey, List.of(options));
	}
}
