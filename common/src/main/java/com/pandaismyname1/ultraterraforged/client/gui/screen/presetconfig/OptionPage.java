package com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import com.pandaismyname1.ultraterraforged.RTFCommon;
import com.pandaismyname1.ultraterraforged.client.data.RTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.client.gui.PresetSharing;
import com.pandaismyname1.ultraterraforged.client.gui.screen.SavePresetScreen;
import com.pandaismyname1.ultraterraforged.client.gui.screen.page.LinkedPageScreen;
import com.pandaismyname1.ultraterraforged.client.gui.widget.WidgetList;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.mixin.ScreenInvoker;
import com.pandaismyname1.ultraterraforged.preset.option.Category;
import com.pandaismyname1.ultraterraforged.preset.option.Option;
import com.pandaismyname1.ultraterraforged.preset.option.Page;
import com.pandaismyname1.ultraterraforged.preset.option.PresetOptions;
import com.pandaismyname1.ultraterraforged.preset.option.StructureOptions;

/**
 * An editor page generated from option declarations: the options on the left, and on the right a page picker,
 * editing tools and the live preview. Typing in the search box lists matching options from every page instead.
 */
public class OptionPage implements LinkedPageScreen.Page {
	private static final int MARGIN = 10;
	private static final int TOP = 30;
	private static final int ROW = 20;
	private static final int GAP = 4;

	private final EditorSession session;
	private final int index;
	private final List<Pair<Option<?>, AbstractWidget>> widgets = new ArrayList<>();
	private String query = "";
	private WidgetList<AbstractWidget> options;

	OptionPage(EditorSession session, int index) {
		this.session = session;
		this.index = index;
	}

	// the declared pages plus one for the structure sets of the world being created, before miscellaneous
	public static List<Page> pages(WorldCreationContext settings) {
		List<Page> pages = new ArrayList<>(PresetOptions.PAGES);
		Set<Holder<Biome>> overworldBiomes = settings.selectedDimensions().overworld().getBiomeSource().possibleBiomes();
		Page structures = StructureOptions.page(settings.worldgenLoadContext().lookupOrThrow(Registries.STRUCTURE_SET), (holder) -> generatesIn(holder.value(), overworldBiomes));
		if (!structures.categories().isEmpty()) {
			pages.add(pages.indexOf(PresetOptions.MISCELLANEOUS), structures);
		}
		return List.copyOf(pages);
	}

	private static boolean generatesIn(StructureSet set, Set<Holder<Biome>> biomes) {
		return set.structures().stream().anyMatch((entry) -> entry.structure().value().biomes().stream().anyMatch(biomes::contains));
	}

	private Page page() {
		return this.session.pages.get(this.index);
	}

	private Preset preset() {
		return this.session.preset();
	}

	@Override
	public Component title() {
		return this.session.entry.getName().copy().append(Component.literal(" - ").withStyle(ChatFormatting.GRAY)).append(Component.translatable(this.page().titleKey()));
	}

	@Override
	public void init() {
		PresetConfigScreen screen = this.session.screen;
		int bottom = screen.height - 30;
		int rightWidth = Mth.clamp((int) ((screen.width - MARGIN * 3) * 0.36F), 100, 240);
		int leftWidth = screen.width - MARGIN * 3 - rightWidth;
		int rightX = screen.width - MARGIN - rightWidth;

		int searchWidth = Math.min(396, leftWidth - 20);
		EditBox search = new EditBox(screen.font, MARGIN + (leftWidth - searchWidth) / 2 + 2, TOP, searchWidth, ROW, Component.translatable(RTFTranslationKeys.GUI_EDITOR_SEARCH));
		search.setHint(Component.translatable(RTFTranslationKeys.GUI_EDITOR_SEARCH).withStyle(ChatFormatting.DARK_GRAY));
		search.setValue(this.query);
		search.setResponder((text) -> {
			if (!text.equals(this.query)) {
				this.query = text;
				this.rebuildOptions();
			}
		});
		this.options = new WidgetList<>(screen.minecraft, leftWidth, screen.height, TOP + ROW + GAP, bottom, 25);
		this.options.setLeftPos(MARGIN);
		this.add(this.options);
		// after the list, which paints over everything above it
		this.add(search);

		int y = TOP;
		List<Integer> indices = IntStream.range(0, this.session.pages.size()).boxed().toList();
		this.add(CycleButton.<Integer>builder((i) -> Component.translatable(this.session.pages.get(i).titleKey()))
			.withValues(indices)
			.withInitialValue(this.index)
			.create(rightX, y, rightWidth, ROW, Component.translatable(RTFTranslationKeys.GUI_EDITOR_PAGE), (button, page) -> screen.setPage(new OptionPage(this.session, page))));
		y += ROW + GAP;

		int half = (rightWidth - GAP) / 2;
		this.add(Button.builder(Component.translatable(RTFTranslationKeys.GUI_EDITOR_RESET_PAGE), (button) -> this.resetShown())
			.bounds(rightX, y, half, ROW)
			.tooltip(Tooltip.create(Component.translatable(RTFTranslationKeys.GUI_EDITOR_RESET_PAGE_TOOLTIP)))
			.build());
		this.add(CycleButton.<RenderMode>builder(OptionWidgets::enumName)
			.withValues(RenderMode.values())
			.withInitialValue(this.session.preview.mode())
			.displayOnlyValue()
			.create(rightX + half + GAP, y, rightWidth - half - GAP, ROW, Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_VIEW), (button, mode) -> this.session.preview.setMode(mode)));
		y += ROW + GAP;

		this.add(Button.builder(Component.translatable(RTFTranslationKeys.GUI_SAVE_PRESET), (button) -> {
			screen.minecraft.setScreen(new SavePresetScreen(screen, this.session.entry.getName().getString(), this.preset().copy(), (saved) -> {}));
		}).bounds(rightX, y, half, ROW).tooltip(Tooltip.create(Component.translatable(RTFTranslationKeys.GUI_SAVE_PRESET_TOOLTIP))).build());
		this.add(Button.builder(Component.translatable(RTFTranslationKeys.GUI_SHARE_COPY), (button) -> PresetSharing.copy(this.preset()))
			.bounds(rightX + half + GAP, y, rightWidth - half - GAP, ROW)
			.tooltip(Tooltip.create(Component.translatable(RTFTranslationKeys.GUI_SHARE_COPY_TOOLTIP)))
			.build());
		y += ROW + GAP;

		AbstractWidget seed = PresetWidgets.createRandomButton(RTFTranslationKeys.GUI_BUTTON_SEED, (int) screen.seed(), (value) -> screen.setSeed(value));
		seed.setX(rightX);
		seed.setY(y);
		seed.setWidth(rightWidth);
		seed.height = ROW;
		this.add(seed);
		y += ROW + GAP;

		int previewSize = Math.min(rightWidth, bottom - y);
		if (previewSize >= 48) {
			this.session.preview.setBounds(rightX + (rightWidth - previewSize) / 2, y, previewSize);
			this.add(this.session.preview);
		}

		this.rebuildOptions();
	}

	private <T extends GuiEventListener & Renderable> void add(T widget) {
		((ScreenInvoker) this.session.screen).invokeAddRenderableWidget(widget);
	}

	private void rebuildOptions() {
		String query = this.query.trim().toLowerCase(Locale.ROOT);
		Preset preset = this.preset();
		List<AbstractWidget> rows = new ArrayList<>();
		this.widgets.clear();
		if (query.isEmpty()) {
			for (Category category : this.page().categories()) {
				this.addCategory(rows, category.label(), visible(category), preset);
			}
		} else {
			for (Page page : this.session.pages) {
				Component pageTitle = Component.translatable(page.titleKey());
				for (Category category : page.categories()) {
					// a matching group, e.g. a structure's name, shows all of its options
					boolean categoryMatches = category.label().map((label) -> matches(label, query)).orElse(false);
					List<Option<?>> found = visible(category).stream().filter((option) -> categoryMatches || matches(option, query)).toList();
					Component label = category.label().map((text) -> pageTitle.copy().append(" > ").append(text)).orElse(pageTitle.copy());
					this.addCategory(rows, Optional.of(label), found, preset);
				}
			}
			if (rows.isEmpty()) {
				rows.add(PresetWidgets.createLabel(Component.translatable(RTFTranslationKeys.GUI_EDITOR_NO_RESULTS).withStyle(ChatFormatting.GRAY)));
			}
		}
		this.options.replaceEntries(rows.stream().map(WidgetList.Entry::new).toList());
		this.options.setScrollAmount(0.0D);
		this.updateActive();
	}

	private void addCategory(List<AbstractWidget> rows, Optional<Component> label, List<Option<?>> options, Preset preset) {
		if (options.isEmpty()) {
			return;
		}
		label.ifPresent((text) -> rows.add(PresetWidgets.createLabel(text)));
		for (Option<?> option : options) {
			AbstractWidget widget = OptionWidgets.create(option, preset, this.session.baseline, this::onChanged);
			this.widgets.add(Pair.of(option, widget));
			rows.add(widget);
		}
	}

	private static List<Option<?>> visible(Category category) {
		return category.options().stream().filter((option) -> !option.isHidden()).toList();
	}

	private static boolean matches(Option<?> option, String query) {
		return matches(option.displayName(), query) || matches(Component.translatable(option.tooltipKey()), query) || option.path().toLowerCase(Locale.ROOT).contains(query);
	}

	private static boolean matches(Component text, String query) {
		return text.getString().toLowerCase(Locale.ROOT).contains(query);
	}

	// resets everything currently listed: the page, or the search results
	private void resetShown() {
		for (Pair<Option<?>, AbstractWidget> entry : this.widgets) {
			entry.getFirst().resetTo(this.preset(), this.session.baseline);
		}
		this.rebuildOptions();
		this.session.changed();
	}

	private void onChanged() {
		this.updateActive();
		this.session.changed();
	}

	private void updateActive() {
		Preset preset = this.preset();
		for (Pair<Option<?>, AbstractWidget> entry : this.widgets) {
			entry.getSecond().active = entry.getFirst().isActive(preset);
		}
	}

	@Override
	public Optional<LinkedPageScreen.Page> previous() {
		if (this.index == 0) {
			return Optional.of(new PresetListPage(this.session.screen));
		}
		return Optional.of(new OptionPage(this.session, this.index - 1));
	}

	@Override
	public Optional<LinkedPageScreen.Page> next() {
		if (this.index + 1 >= this.session.pages.size()) {
			return Optional.empty();
		}
		return Optional.of(new OptionPage(this.session, this.index + 1));
	}

	@Override
	public void onClose() {
		try {
			this.session.entry.save();
		} catch (IOException e) {
			RTFCommon.LOGGER.error("Couldn't save preset {}", this.session.entry.getName().getString(), e);
		}
	}

	@Override
	public void onDone() {
		try {
			this.session.screen.applyPreset(this.session.entry);
		} catch (IOException e) {
			RTFCommon.LOGGER.error("Couldn't apply preset {}", this.session.entry.getName().getString(), e);
		}
	}
}
