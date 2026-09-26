package raccoonman.reterraforged.forge;

import raccoonman.reterraforged.data.preset.RTFWorldPresets;
import net.minecraftforge.client.event.RegisterPresetEditorsEvent;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;

class RTFForgeClient {

	public static void registerPresetEditors(RegisterPresetEditorsEvent event) {
		event.register(RTFWorldPresets.RETERRAFORGED, (screen, ctx) -> new PresetConfigScreen(screen));
	}
}