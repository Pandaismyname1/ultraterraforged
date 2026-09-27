package com.pandaismyname1.ultraterraforged.forge;

import com.pandaismyname1.ultraterraforged.data.preset.RTFWorldPresets;
import net.minecraftforge.client.event.RegisterPresetEditorsEvent;
import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.PresetConfigScreen;

class RTFForgeClient {

	public static void registerPresetEditors(RegisterPresetEditorsEvent event) {
		event.register(RTFWorldPresets.ULTRATERRAFORGED, (screen, ctx) -> new PresetConfigScreen(screen));
	}
}