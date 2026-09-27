package com.pandaismyname1.ultraterraforged.preset.option;

import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;

// Extra information surfaced next to an option, e.g. in its tooltip
public enum OptionTag {
	EXPERIMENTAL(UTFTranslationKeys.OPTION_TAG_EXPERIMENTAL),
	MEDIUM_PERFORMANCE_IMPACT(UTFTranslationKeys.OPTION_TAG_MEDIUM_PERFORMANCE_IMPACT),
	HEAVY_PERFORMANCE_IMPACT(UTFTranslationKeys.OPTION_TAG_HEAVY_PERFORMANCE_IMPACT);

	private final String translationKey;

	private OptionTag(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return this.translationKey;
	}
}
