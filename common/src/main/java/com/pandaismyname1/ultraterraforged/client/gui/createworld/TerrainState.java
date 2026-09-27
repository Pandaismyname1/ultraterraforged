package com.pandaismyname1.ultraterraforged.client.gui.createworld;

import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.WorldOptions;
import com.pandaismyname1.ultraterraforged.data.preset.PresetLibrary;
import com.pandaismyname1.ultraterraforged.data.preset.RTFWorldPresets;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

/**
 * What the player picked for UltraTerraForged in one Create World screen. Lives as long as the screen, so switching
 * tabs or resizing keeps edits.
 */
public class TerrainState implements PreviewSource {
	private final CreateWorldScreen screen;
	private PresetLibrary.Entry source;
	private Component name;
	private Preset preset;
	// the preset as selected, before any edits
	private Preset baseline;
	private boolean edited;
	// the pack selection we asked the screen to load before creating the world, if a reload is in progress
	@Nullable
	private List<String> pendingSelection;
	private int revision;

	public TerrainState(CreateWorldScreen screen) {
		this.screen = screen;
		this.select(PresetLibrary.defaultPreset());
	}

	public static TerrainState of(CreateWorldScreen screen) {
		return ((Holder) screen).ultraterraforged$getTerrainState();
	}

	public WorldCreationUiState uiState() {
		return this.screen.getUiState();
	}

	public void select(PresetLibrary.Entry entry) {
		this.source = entry;
		this.name = entry.name();
		this.baseline = entry.create();
		this.preset = this.baseline.copy();
		this.edited = false;
		this.changed();
	}

	// e.g. the result of the advanced editor
	public void set(Component name, Preset preset) {
		this.name = name;
		this.preset = preset.copy();
		this.edited = true;
		this.changed();
	}

	public void markEdited() {
		this.edited = true;
		this.changed();
	}

	private void changed() {
		this.revision++;
	}

	public PresetLibrary.Entry source() {
		return this.source;
	}

	public Component name() {
		return this.name;
	}

	@Override
	public Preset preset() {
		return this.preset;
	}

	public Preset baseline() {
		return this.baseline;
	}

	public boolean isEdited() {
		return this.edited;
	}

	// increases whenever the preset changes, so views know when to refresh
	@Override
	public int revision() {
		return this.revision;
	}

	@Nullable
	public List<String> pendingSelection() {
		return this.pendingSelection;
	}

	public void setPendingSelection(@Nullable List<String> selection) {
		this.pendingSelection = selection;
	}

	public boolean isUltraTerraForgedSelected() {
		WorldCreationUiState.WorldTypeEntry type = this.uiState().getWorldType();
		return type != null && type.preset() != null && type.preset().is(RTFWorldPresets.ULTRATERRAFORGED);
	}

	/**
	 * Switches the world type to UltraTerraForged, if it's available.
	 */
	public boolean selectUltraTerraForged() {
		if (this.isUltraTerraForgedSelected()) {
			return true;
		}
		for (WorldCreationUiState.WorldTypeEntry entry : this.uiState().getNormalPresetList()) {
			if (entry.preset() != null && entry.preset().is(RTFWorldPresets.ULTRATERRAFORGED)) {
				this.uiState().setWorldType(entry);
				return true;
			}
		}
		return false;
	}

	/**
	 * The seed the world will be created with. An empty seed field means a random one; pick it now so the preview
	 * shows the world that will actually be created.
	 */
	@Override
	public long seed() {
		OptionalLong seed = WorldOptions.parseSeed(this.uiState().getSeed());
		if (seed.isPresent()) {
			return seed.getAsLong();
		}
		long random = ThreadLocalRandom.current().nextLong();
		this.uiState().setSeed(Long.toString(random));
		return random;
	}

	public interface Holder {
		TerrainState ultraterraforged$getTerrainState();
	}
}
