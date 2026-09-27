package com.pandaismyname1.ultraterraforged.forge;

import com.pandaismyname1.ultraterraforged.data.preset.UTFWorldPresets;
import net.minecraftforge.client.event.RegisterPresetEditorsEvent;
import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.PresetConfigScreen;

class UTFForgeClient {

	public static void registerPresetEditors(RegisterPresetEditorsEvent event) {
		event.register(UTFWorldPresets.ULTRATERRAFORGED, (screen, ctx) -> new PresetConfigScreen(screen));
	}
}