package com.pandaismyname1.ultraterraforged.client.gui;

import java.nio.file.Path;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds;
import net.minecraft.network.chat.Component;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.data.preset.PresetShareCode;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.platform.ConfigUtil;

// Copying and pasting preset codes through the clipboard, and where players' own presets are kept
public final class PresetSharing {

	public static Path presetFolder() {
		return ConfigUtil.utf("presets");
	}

	public static void copy(Preset preset) {
		Minecraft.getInstance().keyboardHandler.setClipboard(PresetShareCode.encode(preset));
		Toasts.notify(UTFTranslationKeys.GUI_SHARE_COPIED, Component.empty(), SystemToastIds.PERIODIC_NOTIFICATION);
	}

	public static Optional<Preset> paste() {
		String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
		return PresetShareCode.decode(clipboard).resultOrPartial((error) -> {
			UTFCommon.LOGGER.warn("Couldn't read the preset code in the clipboard: {}", error);
			Toasts.notify(UTFTranslationKeys.GUI_SHARE_INVALID, Component.literal(error), SystemToastIds.PACK_LOAD_FAILURE);
		}).map((preset) -> {
			Toasts.notify(UTFTranslationKeys.GUI_SHARE_PASTED, Component.empty(), SystemToastIds.PERIODIC_NOTIFICATION);
			return preset;
		});
	}

	private PresetSharing() {
	}
}
