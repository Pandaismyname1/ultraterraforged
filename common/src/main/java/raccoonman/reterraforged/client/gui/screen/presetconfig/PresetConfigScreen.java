package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.io.IOException;
import java.nio.file.Path;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.client.gui.createworld.TerrainState;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.data.PresetPacks;
import raccoonman.reterraforged.data.preset.settings.Preset;

public class PresetConfigScreen extends LinkedPageScreen {
	private CreateWorldScreen parent;
	
	public PresetConfigScreen(CreateWorldScreen parent) {
		this.parent = parent;
		this.currentPage = new PresetListPage(this);
	}

	// opens straight into the editor for a preset that isn't saved as a file, such as the one chosen in the Terrain tab
	// changes are marked against, and reset to, the baseline
	public static PresetConfigScreen editing(CreateWorldScreen parent, Component name, Preset preset, Preset baseline) {
		PresetConfigScreen screen = new PresetConfigScreen(parent);
		screen.currentPage = new OptionPage(new EditorSession(screen, new PresetEntry(name, preset.copy(), true, (button) -> {}), baseline.copy()), 0);
		return screen;
	}
	
	@Override
	public void onClose() {
		super.onClose();

		this.minecraft.setScreen(this.parent);
	}
	
	// the world is created from the seed text in the World tab, so that is what has to change
	public void setSeed(long seed) {
		this.parent.getUiState().setSeed(Long.toString(seed));
	}

	public long seed() {
		return TerrainState.of(this.parent).seed();
	}
	
	public WorldCreationContext getSettings() {
		return this.parent.getUiState().getSettings();
	}

	// hands the edited preset to the Create World screen; its datapack is made when the world is created
	public void applyPreset(PresetEntry preset) throws IOException {
		TerrainState state = TerrainState.of(this.parent);
		state.set(preset.getName(), preset.getPreset());
		state.selectReTerraForged();
	}
	
	public void exportAsDatapack(Path outputPath, PresetEntry presetEntry) throws IOException {
		PresetPacks.export(presetEntry.getPreset(), this.getSettings().worldgenLoadContext(), outputPath);
	}
}
