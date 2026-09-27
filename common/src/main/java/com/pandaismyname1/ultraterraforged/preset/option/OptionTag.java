package com.pandaismyname1.ultraterraforged.preset.option;

import com.pandaismyname1.ultraterraforged.client.data.RTFTranslationKeys;

// Extra information surfaced next to an option, e.g. in its tooltip
public enum OptionTag {
	EXPERIMENTAL(RTFTranslationKeys.OPTION_TAG_EXPERIMENTAL),
	MEDIUM_PERFORMANCE_IMPACT(RTFTranslationKeys.OPTION_TAG_MEDIUM_PERFORMANCE_IMPACT),
	HEAVY_PERFORMANCE_IMPACT(RTFTranslationKeys.OPTION_TAG_HEAVY_PERFORMANCE_IMPACT);

	private final String translationKey;

	private OptionTag(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return this.translationKey;
	}
}
