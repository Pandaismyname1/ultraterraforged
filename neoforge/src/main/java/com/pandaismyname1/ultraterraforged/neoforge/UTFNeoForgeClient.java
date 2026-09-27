package com.pandaismyname1.ultraterraforged.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import com.pandaismyname1.ultraterraforged.data.preset.UTFWorldPresets;

// client only, so dedicated servers never load the screens
@Mod(value = UTFCommon.MOD_ID, dist = Dist.CLIENT)
public class UTFNeoForgeClient {

	public UTFNeoForgeClient(IEventBus modBus) {
		modBus.addListener(UTFNeoForgeClient::registerPresetEditors);
	}

	private static void registerPresetEditors(RegisterPresetEditorsEvent event) {
		event.register(UTFWorldPresets.ULTRATERRAFORGED, (screen, ctx) -> new PresetConfigScreen(screen));
	}
}
