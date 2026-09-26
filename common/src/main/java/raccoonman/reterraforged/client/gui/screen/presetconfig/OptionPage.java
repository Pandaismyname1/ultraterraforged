package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen.Page;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.preset.option.Category;
import raccoonman.reterraforged.preset.option.Option;
import raccoonman.reterraforged.preset.option.PresetOptions;
import raccoonman.reterraforged.preset.option.StructureOptions;

// An editor page generated from option declarations
public class OptionPage extends PresetEditorPage {
	private final List<raccoonman.reterraforged.preset.option.Page> pages;
	private final int index;
	private final List<Pair<Option<?>, AbstractWidget>> widgets = new ArrayList<>();

	public OptionPage(PresetConfigScreen screen, PresetEntry preset, int index) {
		this(screen, preset, pages(screen.getSettings()), index);
	}

	private OptionPage(PresetConfigScreen screen, PresetEntry preset, List<raccoonman.reterraforged.preset.option.Page> pages, int index) {
		super(screen, preset);
		this.pages = pages;
		this.index = index;
	}

	// the declared pages plus one for the structure sets of the world being created, before miscellaneous
	public static List<raccoonman.reterraforged.preset.option.Page> pages(WorldCreationContext settings) {
		List<raccoonman.reterraforged.preset.option.Page> pages = new ArrayList<>(PresetOptions.PAGES);
		Set<Holder<Biome>> overworldBiomes = settings.selectedDimensions().overworld().getBiomeSource().possibleBiomes();
		raccoonman.reterraforged.preset.option.Page structures = StructureOptions.page(settings.worldgenLoadContext().lookupOrThrow(Registries.STRUCTURE_SET), (holder) -> generatesIn(holder.value(), overworldBiomes));
		if (!structures.categories().isEmpty()) {
			pages.add(pages.indexOf(PresetOptions.MISCELLANEOUS), structures);
		}
		return List.copyOf(pages);
	}

	private static boolean generatesIn(StructureSet set, Set<Holder<Biome>> biomes) {
		return set.structures().stream().anyMatch((entry) -> entry.structure().value().biomes().stream().anyMatch(biomes::contains));
	}

	@Override
	public Component title() {
		return Component.translatable(this.pages.get(this.index).titleKey());
	}

	@Override
	public void init() {
		super.init();

		Preset preset = this.preset.getPreset();
		this.widgets.clear();
		for (Category category : this.pages.get(this.index).categories()) {
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
		return Optional.of(new OptionPage(this.screen, this.preset, this.pages, this.index - 1));
	}

	@Override
	public Optional<Page> next() {
		if (this.index + 1 >= this.pages.size()) {
			return Optional.empty();
		}
		return Optional.of(new OptionPage(this.screen, this.preset, this.pages, this.index + 1));
	}
}
