package com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.compress.utils.FileNameUtils;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import io.netty.util.internal.StringUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.client.gui.Toasts;
import com.pandaismyname1.ultraterraforged.client.gui.screen.page.BisectedPage;
import com.pandaismyname1.ultraterraforged.client.gui.screen.page.LinkedPageScreen.Page;
import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import com.pandaismyname1.ultraterraforged.client.gui.widget.Label;
import com.pandaismyname1.ultraterraforged.client.gui.widget.WidgetList;
import com.pandaismyname1.ultraterraforged.client.gui.widget.WidgetList.Entry;
import com.pandaismyname1.ultraterraforged.data.preset.settings.BuiltinPresets;
import com.pandaismyname1.ultraterraforged.data.preset.PresetLibrary;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.platform.ConfigUtil;

class PresetListPage extends BisectedPage<PresetConfigScreen, PresetEntry, AbstractWidget> {
	private static final Path PRESET_PATH = ConfigUtil.utf("presets");
	private static final Path EXPORT_PATH = ConfigUtil.utf("exports");
	private static final Path LEGACY_PRESET_PATH = ConfigUtil.legacy("presets");
	
	private static final Predicate<String> IS_VALID = Pattern.compile("^[A-Za-z0-9\\-_ ]+$").asPredicate();

	private EditBox input;
	private Button createPreset;
	private Button deletePreset;
	private Button exportAsDatapack;
	private Button copyPreset;
	private Button openPresetFolder;
	private Button openExportFolder;
	
	public PresetListPage(PresetConfigScreen screen) {
		super(screen);
		
		try {
			if(!Files.exists(PRESET_PATH)) Files.createDirectory(PRESET_PATH);
			if(!Files.exists(EXPORT_PATH)) Files.createDirectory(EXPORT_PATH);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Component title() {
		return Component.translatable(UTFTranslationKeys.GUI_SELECT_PRESET_TITLE);
	}

	@Override
	public void init() {
		super.init();
		
		this.input = PresetWidgets.createEditBox(this.screen.font, (text) -> {
			boolean isValid = this.isValidPresetName(text);
			final int white = 0xFFE0E0E0;
			final int red = 0xFFFF3F30;
			this.createPreset.active = isValid;
			this.input.setTextColor(isValid ? white : red);
		}, Component.translatable(UTFTranslationKeys.GUI_INPUT_PROMPT).withStyle(ChatFormatting.DARK_GRAY));
		this.createPreset = PresetWidgets.createThrowingButton(UTFTranslationKeys.GUI_BUTTON_CREATE, () -> {
			new PresetEntry(Component.literal(this.input.getValue()), BuiltinPresets.makeDefault(), false, this).save();
			this.rebuildPresets();
			this.input.setValue(StringUtil.EMPTY_STRING);
		});
		this.createPreset.active = this.isValidPresetName(this.input.getValue());
		this.copyPreset = PresetWidgets.createThrowingButton(UTFTranslationKeys.GUI_BUTTON_COPY, () -> {
			PresetEntry preset = this.left.getSelected().getWidget();
			String name = preset.getName().getString();
			int counter = 1;
			String uniqueName;
			while(Files.exists(PRESET_PATH.resolve((uniqueName = name + " (" + counter + ")") + ".json"))) { 
				counter++;
			}
			new PresetEntry(Component.literal(uniqueName), preset.getPreset().copy(), false, this).save();
			this.rebuildPresets();
		});
		this.deletePreset = PresetWidgets.createThrowingButton(UTFTranslationKeys.GUI_BUTTON_DELETE, () -> {
			PresetEntry preset = this.left.getSelected().getWidget();
			Files.delete(preset.getPath());
			this.rebuildPresets();
		});
		this.openPresetFolder = PresetWidgets.createThrowingButton(UTFTranslationKeys.GUI_BUTTON_OPEN_PRESET_FOLDER, () -> {
			Blaze3D.openPath(PRESET_PATH);
			this.rebuildPresets();
		});
		this.openExportFolder = PresetWidgets.createThrowingButton(UTFTranslationKeys.GUI_BUTTON_OPEN_EXPORT_FOLDER, () -> {
			Blaze3D.openPath(EXPORT_PATH);
			this.rebuildPresets();
		});
		this.exportAsDatapack = PresetWidgets.createThrowingButton(UTFTranslationKeys.GUI_BUTTON_EXPORT_AS_DATAPACK, () -> {
			PresetEntry preset = this.left.getSelected().getWidget();
			Path path = EXPORT_PATH.resolve(preset.getName().getString() + ".zip");
			this.screen.exportAsDatapack(path, preset);
			this.rebuildPresets();
			
			Toasts.notify(UTFTranslationKeys.GUI_BUTTON_EXPORT_SUCCESS, Component.literal(path.toString()), SystemToastId.WORLD_BACKUP);
		});

		this.right.addWidget(this.input);
		this.right.addWidget(this.createPreset);
		this.right.addWidget(this.copyPreset);
		this.right.addWidget(this.deletePreset);
		this.right.addWidget(this.openPresetFolder);
		this.right.addWidget(this.openExportFolder);
		this.right.addWidget(this.exportAsDatapack);
		
		this.left.setRenderSelected(true);
		
		try {
			this.rebuildPresets();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Optional<Page> previous() {
		return Optional.empty();
	}

	@Override
	public Optional<Page> next() {
		return Optional.ofNullable(this.left).map(WidgetList::getSelected).map((e) -> {
			PresetEntry entry = e.getWidget();
			if(entry.isBuiltin()) {
				entry = new PresetEntry(entry.name, entry.preset.copy(), true, (b) -> {});
			}
			return new OptionPage(new EditorSession(this.screen, entry, entry.getPreset().copy()), 0);
		});
	}
	
	@Override
	public void onDone() {
		super.onDone();
		
		Entry<PresetEntry> selected = this.left.getSelected();
		if(selected != null) {
			try {
				this.screen.applyPreset(selected.getWidget());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void selectPreset(@Nullable PresetEntry entry) {
		boolean active = entry != null;
		
		this.screen.doneButton.active = active;
		this.copyPreset.active = active;
		this.deletePreset.active = active && !entry.isBuiltin();
		this.exportAsDatapack.active = active;
		this.screen.nextButton.active = active;
	}
	
	private void rebuildPresets() throws IOException {
		this.selectPreset(null);
		
		List<PresetEntry> entries = new ArrayList<>();
		for (PresetLibrary.Entry entry : PresetLibrary.files(PRESET_PATH, LEGACY_PRESET_PATH)) {
			entries.add(new PresetEntry(entry.name(), entry.create(), false, this));
		}
		// the modpack's presets are read-only like the built-in ones
		for (PresetLibrary.Entry entry : PresetLibrary.shipped()) {
			entries.add(new PresetEntry(entry.name(), entry.create(), true, this));
		}
		this.left.replaceEntries(entries.stream().map(WidgetList.Entry::new).toList());
	}
	
	private boolean isValidPresetName(String text) {
		return IS_VALID.test(text) && !this.hasPresetWithName(text);
	}
	
	private boolean hasPresetWithName(String name) {
		return this.left.children().stream().filter((entry) -> {
			return entry.getWidget().getName().getString().equals(name);
		}).findAny().isPresent();
	}
	
	
	public static class PresetEntry extends Label {
		private Component name;
		private Preset preset;
		private boolean builtin;
		
		public PresetEntry(Component name, Preset preset, boolean builtin, OnPress onPress) {
			super(-1, -1, -1, -1, onPress, name);
			
			this.name = name;
			this.preset = preset;
			this.builtin = builtin;
		}
		
		public PresetEntry(Component name, Preset preset, boolean builtin, PresetListPage page) {
			this(name, preset, builtin, (b) -> {
				if(b instanceof PresetEntry entry) {
					page.selectPreset(entry);
				}
			});
		}

		public Component getName() {
			return this.name;
		}
		
		public Preset getPreset() {
			return this.preset;
		}
		
		public boolean isBuiltin() {
			return this.builtin;
		}
		
		public Path getPath() {
			return PRESET_PATH.resolve(this.name.getString() + ".json");
		}
		
		//FIXME delete old pack before save
		public void save() throws IOException {
			if(!this.builtin) {
				try(
					Writer writer = Files.newBufferedWriter(this.getPath());
					JsonWriter jsonWriter = new JsonWriter(writer);
				) {
					JsonElement element = Preset.CODEC.encodeStart(JsonOps.INSTANCE, this.preset).result().orElseThrow();
					jsonWriter.setSerializeNulls(false);
					jsonWriter.setIndent("  ");
					GsonHelper.writeValue(jsonWriter, element, null);
				}
			}
		}
	}
}
