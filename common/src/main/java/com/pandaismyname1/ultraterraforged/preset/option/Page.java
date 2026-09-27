package com.pandaismyname1.ultraterraforged.preset.option;

import java.util.List;
import java.util.stream.Stream;

public record Page(String id, String titleKey, List<Category> categories) {

	public Page {
		categories = List.copyOf(categories);
	}

	public Stream<Option<?>> options() {
		return this.categories.stream().flatMap((category) -> category.options().stream());
	}

	public static Page of(String id, String titleKey, Category... categories) {
		return new Page(id, titleKey, List.of(categories));
	}
}
