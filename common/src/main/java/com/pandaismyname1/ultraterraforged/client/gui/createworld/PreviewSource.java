package com.pandaismyname1.ultraterraforged.client.gui.createworld;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

// What a TerrainPreview shows: the preset being edited and the seed of the world being created
public interface PreviewSource {

	Preset preset();

	// changes whenever the preset changes, so the preview knows to render again
	int revision();

	long seed();
}
