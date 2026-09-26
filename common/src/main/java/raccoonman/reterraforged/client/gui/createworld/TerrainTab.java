package raccoonman.reterraforged.client.gui.createworld;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.presetconfig.OptionWidgets;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.RenderMode;
import raccoonman.reterraforged.data.preset.PresetLibrary;
import raccoonman.reterraforged.platform.ConfigUtil;
import raccoonman.reterraforged.preset.option.PresetOptions;

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
	private final List<PresetLibrary.Entry> presets;

	private final StringWidget status;
	private final Button presetButton;
	private final Button advancedButton;
	private final ScrollingPanel sliders;
	private final TerrainPreview preview;
	private final CycleButton<RenderMode> viewButton;
	private int builtRevision = -1;

	public TerrainTab(CreateWorldScreen screen) {
		this.screen = screen;
		this.state = TerrainState.of(screen);
		this.presets = new ArrayList<>(PresetLibrary.builtins());
		this.presets.addAll(PresetLibrary.files(ConfigUtil.rtf("presets")));

		Minecraft minecraft = Minecraft.getInstance();
		this.status = new StringWidget(Component.empty(), minecraft.font).alignLeft();
		this.presetButton = Button.builder(Component.empty(), (button) -> this.cyclePreset(Screen.hasShiftDown() ? -1 : 1)).build();
		this.presetButton.setTooltip(Tooltip.create(Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_PRESET_TOOLTIP)));
		this.advancedButton = Button.builder(Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_ADVANCED), (button) -> {
			minecraft.setScreen(PresetConfigScreen.editing(this.screen, this.state.name(), this.state.preset()));
		}).build();
		this.advancedButton.setTooltip(Tooltip.create(Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_ADVANCED_TOOLTIP)));
		this.sliders = new ScrollingPanel();
		this.preview = new TerrainPreview(this.state);
		this.viewButton = CycleButton.<RenderMode>builder((mode) -> Component.literal(mode.name().toLowerCase().replace('_', ' ')))
			.withValues(RenderMode.values())
			.withInitialValue(this.preview.mode())
			.create(0, 0, 0, 0, Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_VIEW), (button, mode) -> this.preview.setMode(mode));
		this.rebuildSliders();
		this.updateLabels();
	}

	@Override
	public Component getTabTitle() {
		return Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_TITLE);
	}

	@Override
	public void visitChildren(Consumer<AbstractWidget> consumer) {
		consumer.accept(this.status);
		consumer.accept(this.presetButton);
		consumer.accept(this.advancedButton);
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
		this.status.setWidth(columnWidth);
		this.status.height = 12;
		int y = top + 12 + GAP;
		this.presetButton.setX(left);
		this.presetButton.setY(y);
		this.presetButton.setWidth(columnWidth);
		y += ROW + GAP;
		this.advancedButton.setX(left);
		this.advancedButton.setY(y);
		this.advancedButton.setWidth(columnWidth);
		y += ROW + GAP * 2;
		this.sliders.setBounds(left, y, columnWidth, Math.max(ROW, area.bottom() - PADDING - y));

		int previewX = left + columnWidth + PADDING;
		this.preview.setBounds(previewX, top, previewSize);
		this.viewButton.setX(previewX);
		this.viewButton.setY(top + previewSize + GAP);
		this.viewButton.setWidth(previewSize);
		this.viewButton.height = ROW;
	}

	@Override
	public void tick() {
		// the preset can change from outside the tab, e.g. the advanced editor
		if (this.builtRevision != this.state.revision()) {
			this.rebuildSliders();
		}
		this.updateLabels();
	}

	private void cyclePreset(int direction) {
		int index = this.presets.indexOf(this.state.source());
		PresetLibrary.Entry next = this.presets.get(Math.floorMod(index + direction, this.presets.size()));
		this.state.select(next);
		this.state.selectReTerraForged();
		this.rebuildSliders();
		this.updateLabels();
	}

	private void rebuildSliders() {
		List<AbstractWidget> widgets = new ArrayList<>();
		for (PresetOptions.SimpleSetting setting : PresetOptions.SIMPLE) {
			widgets.add(OptionWidgets.create(setting.option(), this.state.preset(), this::onSettingChanged, Component.translatable(setting.labelKey())));
		}
		this.sliders.setChildren(widgets);
		this.builtRevision = this.state.revision();
	}

	private void onSettingChanged() {
		this.state.markEdited();
		this.state.selectReTerraForged();
		// the widgets already show the new value; don't rebuild them mid-drag
		this.builtRevision = this.state.revision();
		this.updateLabels();
	}

	private void updateLabels() {
		MutableComponent name = this.state.name().copy();
		if (this.state.isEdited()) {
			name.append(Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_MODIFIED).withStyle(ChatFormatting.YELLOW));
		}
		this.presetButton.setMessage(CommonComponents.optionNameValue(Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_PRESET), name));

		if (this.state.isReTerraForgedSelected()) {
			this.status.setMessage(Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_ACTIVE).withStyle(ChatFormatting.GREEN));
		} else {
			this.status.setMessage(Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_INACTIVE).withStyle(ChatFormatting.GRAY));
		}
	}
}
