package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen.Page;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.preset.option.Category;
import raccoonman.reterraforged.preset.option.Option;
import raccoonman.reterraforged.preset.option.PresetOptions;

// An editor page generated from one of the PresetOptions pages
public class OptionPage extends PresetEditorPage {
	private final int index;
	private final raccoonman.reterraforged.preset.option.Page page;
	private final List<Pair<Option<?>, AbstractWidget>> widgets = new ArrayList<>();

	public OptionPage(PresetConfigScreen screen, PresetEntry preset, int index) {
		super(screen, preset);
		this.index = index;
		this.page = PresetOptions.PAGES.get(index);
	}

	@Override
	public Component title() {
		return Component.translatable(this.page.titleKey());
	}

	@Override
	public void init() {
		super.init();

		Preset preset = this.preset.getPreset();
		this.widgets.clear();
		for (Category category : this.page.categories()) {
			List<Option<?>> visible = category.options().stream().filter((option) -> !option.isHidden()).toList();
			if (visible.isEmpty()) {
				continue;
			}
			category.label().ifPresent((label) -> this.left.addWidget(PresetWidgets.createLabel(label)));
			for (Option<?> option : visible) {
				AbstractWidget widget = OptionWidgets.create(option, preset, this::onChanged);
				this.widgets.add(Pair.of(option, widget));
				this.left.addWidget(widget);
			}
		}
		this.updateActive();
	}

	private void onChanged() {
		this.updateActive();
		this.regenerate();
	}

	private void updateActive() {
		Preset preset = this.preset.getPreset();
		for (Pair<Option<?>, AbstractWidget> entry : this.widgets) {
			entry.getSecond().active = entry.getFirst().isActive(preset);
		}
	}

	@Override
	public Optional<Page> previous() {
		if (this.index == 0) {
			return Optional.of(new PresetListPage(this.screen));
		}
		return Optional.of(new OptionPage(this.screen, this.preset, this.index - 1));
	}

	@Override
	public Optional<Page> next() {
		if (this.index + 1 >= PresetOptions.PAGES.size()) {
			return Optional.empty();
		}
		return Optional.of(new OptionPage(this.screen, this.preset, this.index + 1));
	}
}
