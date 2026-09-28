package com.pandaismyname1.ultraterraforged.client.gui.screen;

import java.nio.file.Files;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.client.gui.PresetSharing;
import com.pandaismyname1.ultraterraforged.client.gui.Toasts;
import com.pandaismyname1.ultraterraforged.data.preset.PresetLibrary;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

/**
 * Asks for a name and saves a preset to the player's preset folder, where the Terrain tab and the editor list it.
 */
public class SavePresetScreen extends Screen {
	private final Screen parent;
	private final Preset preset;
	private final String suggestedName;
	private final Consumer<PresetLibrary.Entry> onSaved;
	private EditBox name;
	private Button save;
	private Component hint = Component.empty();

	public SavePresetScreen(Screen parent, String suggestedName, Preset preset, Consumer<PresetLibrary.Entry> onSaved) {
		super(Component.translatable(UTFTranslationKeys.GUI_SAVE_PRESET_TITLE));
		this.parent = parent;
		this.preset = preset;
		// keep only the characters a preset name may have, e.g. from a translated built-in preset name
		this.suggestedName = suggestedName.replaceAll("[^A-Za-z0-9_ -]", "").trim();
		this.onSaved = onSaved;
	}

	@Override
	protected void init() {
		int center = this.width / 2;
		int y = this.height / 2 - 30;
		String value = this.name != null ? this.name.getValue() : this.suggestedName;
		this.name = new EditBox(this.font, center - 100, y, 200, 20, Component.translatable(UTFTranslationKeys.GUI_SAVE_PRESET_NAME));
		this.name.setMaxLength(64);
		this.name.setHint(Component.translatable(UTFTranslationKeys.GUI_SAVE_PRESET_NAME).withStyle(ChatFormatting.DARK_GRAY));
		this.name.setValue(value);
		this.name.setResponder((text) -> this.validate());
		this.addRenderableWidget(this.name);

		this.save = this.addRenderableWidget(Button.builder(Component.translatable(UTFTranslationKeys.GUI_SAVE_PRESET_CONFIRM), (button) -> this.save())
			.bounds(center - 100, y + 40, 98, 20).build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> this.onClose())
			.bounds(center + 2, y + 40, 98, 20).build());
		this.setInitialFocus(this.name);
		this.validate();
	}

	private void validate() {
		String text = this.name.getValue().trim();
		boolean valid = PresetLibrary.isValidName(text);
		this.save.active = valid;
		if (!valid) {
			this.hint = text.isEmpty() ? Component.empty() : Component.translatable(UTFTranslationKeys.GUI_SAVE_PRESET_INVALID_NAME).withStyle(ChatFormatting.RED);
		} else if (Files.exists(PresetLibrary.file(PresetSharing.presetFolder(), text))) {
			this.hint = Component.translatable(UTFTranslationKeys.GUI_SAVE_PRESET_REPLACES).withStyle(ChatFormatting.YELLOW);
		} else {
			this.hint = Component.empty();
		}
	}

	private void save() {
		String text = this.name.getValue().trim();
		try {
			PresetLibrary.Entry entry = PresetLibrary.save(PresetSharing.presetFolder(), text, this.preset);
			Toasts.notify(UTFTranslationKeys.GUI_SAVE_PRESET_SAVED, Component.literal(text), SystemToastId.PERIODIC_NOTIFICATION);
			this.onSaved.accept(entry);
			this.onClose();
		} catch (Exception e) {
			UTFCommon.LOGGER.error("Couldn't save preset {}", text, e);
			Toasts.notify(UTFTranslationKeys.GUI_SAVE_PRESET_FAILED, Component.literal(String.valueOf(e.getMessage())), SystemToastId.PACK_LOAD_FAILURE);
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// enter saves
		if ((event.key() == 257 || event.key() == 335) && this.save.active) {
			this.save();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFFFF);
		graphics.centeredText(this.font, this.hint, this.width / 2, this.height / 2 - 4, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
