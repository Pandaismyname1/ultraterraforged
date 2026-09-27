package com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig;

import java.util.List;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.pandaismyname1.ultraterraforged.client.data.RTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.client.gui.widget.Slider;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.preset.option.BoolOption;
import com.pandaismyname1.ultraterraforged.preset.option.EnumOption;
import com.pandaismyname1.ultraterraforged.preset.option.FloatOption;
import com.pandaismyname1.ultraterraforged.preset.option.IntOption;
import com.pandaismyname1.ultraterraforged.preset.option.Option;
import com.pandaismyname1.ultraterraforged.preset.option.OptionTag;

// Builds an editor widget for any preset option. Values that differ from the baseline (the preset the player started
// from) are marked, and every widget supports ctrl+click to go back to the baseline value.
public final class OptionWidgets {
	private static final int MODIFIED_COLOR = 0xFFFFC23D;

	public static AbstractWidget create(Option<?> option, Preset preset, Preset baseline, Runnable onChange) {
		return create(option, preset, baseline, onChange, option.displayName());
	}

	// with a label other than the option's own, e.g. where the surrounding category isn't shown
	public static AbstractWidget create(Option<?> option, Preset preset, Preset baseline, Runnable onChange, Component name) {
		if (option instanceof IntOption intOption) {
			if (intOption.isSeed()) {
				return PresetWidgets.createRandomButton(option.translationKey(), intOption.get(preset), (value) -> {
					intOption.set(preset, value);
					onChange.run();
				});
			}
			return new OptionSlider<>(intOption, preset, baseline, Slider.Format.INT, intOption.min(), intOption.max(), (raw) -> raw.intValue(), onChange, name);
		}
		if (option instanceof FloatOption floatOption) {
			return new OptionSlider<>(floatOption, preset, baseline, Slider.Format.FLOAT, floatOption.min(), floatOption.max(), (raw) -> raw.floatValue(), onChange, name);
		}
		if (option instanceof BoolOption boolOption) {
			return new OptionCycleButton<>(boolOption, preset, baseline, List.of(true, false), OptionWidgets::booleanName, onChange, name);
		}
		if (option instanceof EnumOption<?> enumOption) {
			return createEnum(enumOption, preset, baseline, onChange, name);
		}
		throw new IllegalArgumentException("No widget for option " + option);
	}

	private static <E extends Enum<E>> AbstractWidget createEnum(EnumOption<E> option, Preset preset, Preset baseline, Runnable onChange, Component name) {
		return new OptionCycleButton<>(option, preset, baseline, option.values(), OptionWidgets::enumName, onChange, name);
	}

	public static Component enumName(Enum<?> value) {
		return Component.translatable(RTFTranslationKeys.enumValue(value));
	}

	private static Component booleanName(boolean value) {
		return Component.translatable(value ? RTFTranslationKeys.GUI_BUTTON_TRUE : RTFTranslationKeys.GUI_BUTTON_FALSE);
	}

	// a bar along the left edge of options the player changed
	private static void renderModified(GuiGraphics graphics, AbstractWidget widget, boolean modified) {
		if (modified) {
			graphics.fill(widget.getX() - 4, widget.getY() + 1, widget.getX() - 2, widget.getY() + widget.getHeight() - 1, MODIFIED_COLOR);
		}
	}

	private static <T> Tooltip createTooltip(Option<T> option, Component defaultValue) {
		MutableComponent text = Component.translatable(option.tooltipKey());
		for (OptionTag tag : option.tags()) {
			text.append(CommonComponents.NEW_LINE).append(Component.translatable(tag.translationKey()).withStyle(ChatFormatting.GOLD));
		}
		text.append(CommonComponents.NEW_LINE).append(Component.translatable(RTFTranslationKeys.GUI_RESET_TO_DEFAULT, defaultValue).withStyle(ChatFormatting.GRAY));
		return Tooltip.create(text);
	}

	private static class OptionSlider<T extends Number & Comparable<T>> extends Slider {
		private final Option<T> option;
		private final Preset preset;
		private final Preset baseline;
		private final Format format;

		public OptionSlider(Option<T> option, Preset preset, Preset baseline, Format format, T min, T max, Function<Double, T> fromSlider, Runnable onChange, Component name) {
			super(-1, -1, -1, -1, option.get(preset).floatValue(), min.floatValue(), max.floatValue(), name, format, (slider, value) -> {
				T stored = option.set(preset, fromSlider.apply(slider.scaleValue(value)));
				onChange.run();
				return slider.getSliderValue(stored.floatValue());
			});
			this.option = option;
			this.preset = preset;
			this.baseline = baseline;
			this.format = format;
			this.setTooltip(createTooltip(option, Component.literal(format.getMessage(option.get(baseline).doubleValue()))));
		}

		@Override
		public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			super.renderWidget(graphics, mouseX, mouseY, partialTick);
			renderModified(graphics, this, this.option.isModified(this.preset, this.baseline));
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (this.active && this.visible && Screen.hasControlDown() && this.clicked(mouseX, mouseY)) {
				this.option.resetTo(this.preset, this.baseline);
				this.setValue(this.getSliderValue(this.option.get(this.preset).floatValue()));
				this.applyValue();
				this.updateMessage();
				this.playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
				return true;
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}
	}

	// a cycling button that, unlike the vanilla one, can be reset; shift+click cycles backwards
	private static class OptionCycleButton<T> extends Button {
		private final Option<T> option;
		private final Preset preset;
		private final Preset baseline;
		private final List<T> values;
		private final Function<T, Component> nameGetter;
		private final Component name;

		public OptionCycleButton(Option<T> option, Preset preset, Preset baseline, List<T> values, Function<T, Component> nameGetter, Runnable onChange, Component name) {
			super(-1, -1, -1, -1, CommonComponents.EMPTY, (button) -> {
				if (button instanceof OptionCycleButton<?> self) {
					self.cycle(Screen.hasShiftDown() ? -1 : 1);
					onChange.run();
				}
			}, DEFAULT_NARRATION);
			this.option = option;
			this.preset = preset;
			this.baseline = baseline;
			this.values = values;
			this.nameGetter = nameGetter;
			this.name = name;
			this.setTooltip(createTooltip(option, nameGetter.apply(option.get(baseline))));
			this.updateMessage();
		}

		@Override
		public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			super.renderWidget(graphics, mouseX, mouseY, partialTick);
			renderModified(graphics, this, this.option.isModified(this.preset, this.baseline));
		}

		private void cycle(int direction) {
			if (Screen.hasControlDown()) {
				this.option.resetTo(this.preset, this.baseline);
			} else {
				int index = this.values.indexOf(this.option.get(this.preset));
				int next = Math.floorMod(index + direction, this.values.size());
				this.option.set(this.preset, this.values.get(next));
			}
			this.updateMessage();
		}

		private void updateMessage() {
			this.setMessage(CommonComponents.optionNameValue(this.name, this.nameGetter.apply(this.option.get(this.preset))));
		}
	}

	private OptionWidgets() {
	}
}
