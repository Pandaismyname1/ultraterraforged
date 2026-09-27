package com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig;

import java.util.List;

import com.pandaismyname1.ultraterraforged.client.gui.createworld.PreviewSource;
import com.pandaismyname1.ultraterraforged.client.gui.createworld.TerrainPreview;
import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.preset.option.Page;

/**
 * One visit to the advanced editor: the preset being edited, what it looked like when the editor opened, and the
 * preview, which all pages share so zoom and position survive switching pages.
 */
final class EditorSession implements PreviewSource {
	final PresetConfigScreen screen;
	final PresetEntry entry;
	final Preset baseline;
	final List<Page> pages;
	final TerrainPreview preview;
	private int revision;

	EditorSession(PresetConfigScreen screen, PresetEntry entry, Preset baseline) {
		this.screen = screen;
		this.entry = entry;
		this.baseline = baseline;
		this.pages = OptionPage.pages(screen.getSettings());
		this.preview = new TerrainPreview(this);
	}

	void changed() {
		this.revision++;
	}

	@Override
	public Preset preset() {
		return this.entry.getPreset();
	}

	@Override
	public int revision() {
		return this.revision;
	}

	@Override
	public long seed() {
		return this.screen.seed();
	}
}
