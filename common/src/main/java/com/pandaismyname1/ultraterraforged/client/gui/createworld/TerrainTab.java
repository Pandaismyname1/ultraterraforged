package com.pandaismyname1.ultraterraforged.client.gui.createworld;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.client.gui.PresetSharing;
import com.pandaismyname1.ultraterraforged.client.gui.screen.SavePresetScreen;
import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.OptionWidgets;
import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.RenderMode;
import com.pandaismyname1.ultraterraforged.data.preset.PresetLibrary;
import com.pandaismyname1.ultraterraforged.preset.option.PresetOptions;

/**
 * The "Terrain" tab of the Create World screen: pick a preset, adjust the most important settings, and see the
 * result live. Anything more detailed is one click away in the full editor.
 */
public class TerrainTab implements Tab {
	private static final int PADDING = 8;
	private static final int ROW = 20;
	private static final int GAP = 4;

	private final CreateWorldScreen screen;
	private final TerrainState state;
	private final List<PresetLibrary.Entry> presets = new ArrayList<>();

	private final StringWidget status;
	private final AbstractWidget performanceLight;
	private final Button presetButton;
	private final Button advancedButton;
	private final Button saveButton;
	private final Button copyButton;
	private final Button pasteButton;
	private final ScrollingPanel sliders;
	private final TerrainPreview preview;
	private final CycleButton<RenderMode> viewButton;
	private int builtRevision = -1;
	@Nullable
	private PresetLibrary.Entry tooltipSource;

	public TerrainTab(CreateWorldScreen screen) {
		this.screen = screen;
		this.state = TerrainState.of(screen);
		this.reloadPresets();

		Minecraft minecraft = Minecraft.getInstance();
		this.status = new StringWidget(Component.empty(), minecraft.font).alignLeft();
		this.presetButton = Button.builder(Component.empty(), (button) -> this.cyclePreset(Screen.hasShiftDown() ? -1 : 1)).build();
		this.advancedButton = Button.builder(Component.translatable(UTFTranslationKeys.GUI_TERRAIN_TAB_ADVANCED), (button) -> {
			minecraft.setScreen(PresetConfigScreen.editing(this.screen, this.state.name(), this.state.preset(), this.state.baseline()));
		}).build();
		this.advancedButton.setTooltip(Tooltip.create(Component.translatable(UTFTranslationKeys.GUI_TERRAIN_TAB_ADVANCED_TOOLTIP)));
		this.saveButton = Button.builder(Component.translatable(UTFTranslationKeys.GUI_SAVE_PRESET), (button) -> {
			minecraft.setScreen(new SavePresetScreen(this.screen, this.state.name().getString(), this.state.preset().copy(), (saved) -> {
				this.reloadPresets();
				this.state.select(saved);
				this.state.selectUltraTerraForged();
			}));
		}).tooltip(Tooltip.create(Component.translatable(UTFTranslationKeys.GUI_SAVE_PRESET_TOOLTIP))).build();
		this.copyButton = Button.builder(Component.translatable(UTFTranslationKeys.GUI_SHARE_COPY), (button) -> PresetSharing.copy(this.state.preset()))
			.tooltip(Tooltip.create(Component.translatable(UTFTranslationKeys.GUI_SHARE_COPY_TOOLTIP))).build();
		this.pasteButton = Button.builder(Component.translatable(UTFTranslationKeys.GUI_SHARE_PASTE), (button) -> {
			PresetSharing.paste().ifPresent((preset) -> {
				this.state.select(new PresetLibrary.Entry("shared", Component.translatable(UTFTranslationKeys.GUI_SHARE_PASTED_NAME), null, preset::copy, null, PresetLibrary.Source.OTHER));
				this.state.selectUltraTerraForged();
				this.rebuildSliders();
				this.updateLabels();
			});
		}).tooltip(Tooltip.create(Component.translatable(UTFTranslationKeys.GUI_SHARE_PASTE_TOOLTIP))).build();
		this.sliders = new ScrollingPanel();
		this.performanceLight = PerformanceModsSection.light(this.sliders::scrollToBottom);
		this.preview = new TerrainPreview(this.state);
		this.viewButton = CycleButton.<RenderMode>builder(OptionWidgets::enumName)
			.withValues(RenderMode.values())
			.withInitialValue(this.preview.mode())
			.create(0, 0, 0, 0, Component.translatable(UTFTranslationKeys.GUI_TERRAIN_TAB_VIEW), (button, mode) -> this.preview.setMode(mode));
		this.rebuildSliders();
		this.updateLabels();
	}

	@Override
	public Component getTabTitle() {
		return Component.translatable(UTFTranslationKeys.GUI_TERRAIN_TAB_TITLE);
	}

	@Override
	public void visitChildren(Consumer<AbstractWidget> consumer) {
		consumer.accept(this.status);
		consumer.accept(this.performanceLight);
		consumer.accept(this.presetButton);
		consumer.accept(this.advancedButton);
		consumer.accept(this.saveButton);
		consumer.accept(this.copyButton);
		consumer.accept(this.pasteButton);
		consumer.accept(this.sliders);
		consumer.accept(this.preview);
		consumer.accept(this.viewButton);
	}

	@Override
	public void doLayout(ScreenRectangle area) {
		int columnWidth = Math.min(210, (area.width() - PADDING * 3) / 2);
		int previewSize = Math.max(64, Math.min(area.height() - PADDING * 2 - ROW - GAP, area.width() - columnWidth - PADDING * 3));
		int left = area.left() + (area.width() - (columnWidth + PADDING + previewSize)) / 2;
		int top = area.top() + PADDING;

		this.status.setX(left);
		this.status.setY(top);
		int lightWidth = PerformanceModsSection.lightWidth();
		this.status.setWidth(columnWidth - lightWidth - GAP);
		this.status.height = 12;
		this.performanceLight.setX(left + columnWidth - lightWidth);
		this.performanceLight.setY(top);
		this.performanceLight.setWidth(lightWidth);
		this.performanceLight.height = 12;
		int y = top + 12 + GAP;
		this.presetButton.setX(left);
		this.presetButton.setY(y);
		this.presetButton.setWidth(columnWidth);
		y += ROW + GAP;
		this.advancedButton.setX(left);
		this.advancedButton.setY(y);
		this.advancedButton.setWidth(columnWidth);
		y += ROW + GAP;
		int third = (columnWidth - GAP * 2) / 3;
		this.saveButton.setX(left);
		this.copyButton.setX(left + third + GAP);
		this.pasteButton.setX(left + (third + GAP) * 2);
		for (Button button : List.of(this.saveButton, this.copyButton, this.pasteButton)) {
			button.setY(y);
			button.setWidth(button == this.pasteButton ? columnWidth - (third + GAP) * 2 : third);
		}
		y += ROW + GAP * 2;
		this.sliders.setBounds(left, y, columnWidth, Math.max(ROW, area.bottom() - PADDING - y));

		int previewX = left + columnWidth + PADDING;
		this.preview.setBounds(previewX, top, previewSize);
		this.viewButton.setX(previewX);
		this.viewButton.setY(top + previewSize + GAP);
		this.viewButton.setWidth(previewSize);
		this.viewButton.height = ROW;
	}

	// called by the Create World screen while this tab is shown
	public void tick() {
		// the preset can change from outside the tab, e.g. the advanced editor
		if (this.builtRevision != this.state.revision()) {
			this.rebuildSliders();
		}
		this.updateLabels();
	}

	private void reloadPresets() {
		this.presets.clear();
		this.presets.addAll(PresetLibrary.shipped());
		this.presets.addAll(PresetLibrary.files(PresetSharing.presetFolder()));
	}

	private void cyclePreset(int direction) {
		// entries are matched by id, file entries are recreated whenever the folder is read
		int index = -1;
		for (int i = 0; i < this.presets.size(); i++) {
			if (this.presets.get(i).id().equals(this.state.source().id())) {
				index = i;
			}
		}
		if (index < 0) {
			// e.g. a pasted preset; start over from the first or last one
			index = direction > 0 ? -1 : this.presets.size();
		}
		PresetLibrary.Entry next = this.presets.get(Math.floorMod(index + direction, this.presets.size()));
		this.state.select(next);
		this.state.selectUltraTerraForged();
		this.rebuildSliders();
		this.updateLabels();
	}

	private void rebuildSliders() {
		List<AbstractWidget> widgets = new ArrayList<>();
		for (PresetOptions.SimpleSetting setting : PresetOptions.SIMPLE) {
			widgets.add(OptionWidgets.create(setting.option(), this.state.preset(), this.state.baseline(), this::onSettingChanged, Component.translatable(setting.labelKey())));
		}
		widgets.addAll(PerformanceModsSection.widgets(this.screen));
		this.sliders.setChildren(widgets);
		this.builtRevision = this.state.revision();
	}

	private void onSettingChanged() {
		this.state.markEdited();
		this.state.selectUltraTerraForged();
		// the widgets already show the new value; don't rebuild them mid-drag
		this.builtRevision = this.state.revision();
		this.updateLabels();
	}

	private void updateLabels() {
		MutableComponent name = this.state.name().copy();
		if (this.state.isEdited()) {
			name.append(Component.translatable(UTFTranslationKeys.GUI_TERRAIN_TAB_MODIFIED).withStyle(ChatFormatting.YELLOW));
		}
		this.presetButton.setMessage(CommonComponents.optionNameValue(Component.translatable(UTFTranslationKeys.GUI_TERRAIN_TAB_PRESET), name));
		if (this.tooltipSource != this.state.source()) {
			this.tooltipSource = this.state.source();
			MutableComponent tooltip = Component.empty();
			if (this.tooltipSource.description() != null) {
				tooltip.append(this.tooltipSource.description()).append(CommonComponents.NEW_LINE).append(CommonComponents.NEW_LINE);
			}
			tooltip.append(Component.translatable(UTFTranslationKeys.GUI_TERRAIN_TAB_PRESET_TOOLTIP).withStyle(ChatFormatting.GRAY));
			this.presetButton.setTooltip(Tooltip.create(tooltip));
		}

		if (this.state.isUltraTerraForgedSelected()) {
			this.status.setMessage(Component.translatable(UTFTranslationKeys.GUI_TERRAIN_TAB_ACTIVE).withStyle(ChatFormatting.GREEN));
		} else {
			this.status.setMessage(Component.translatable(UTFTranslationKeys.GUI_TERRAIN_TAB_INACTIVE).withStyle(ChatFormatting.GRAY));
		}
	}
}
