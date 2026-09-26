package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.List;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.widget.Slider;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.preset.option.BoolOption;
import raccoonman.reterraforged.preset.option.EnumOption;
import raccoonman.reterraforged.preset.option.FloatOption;
import raccoonman.reterraforged.preset.option.IntOption;
import raccoonman.reterraforged.preset.option.Option;
import raccoonman.reterraforged.preset.option.OptionTag;

// Builds an editor widget for any preset option; every widget supports ctrl+click to reset to the default
public final class OptionWidgets {

	public static AbstractWidget create(Option<?> option, Preset preset, Runnable onChange) {
		return create(option, preset, onChange, option.displayName());
	}

	// with a label other than the option's own, e.g. where the surrounding category isn't shown
	public static AbstractWidget create(Option<?> option, Preset preset, Runnable onChange, Component name) {
		if (option instanceof IntOption intOption) {
			if (intOption.isSeed()) {
				return PresetWidgets.createRandomButton(option.translationKey(), intOption.get(preset), (value) -> {
					intOption.set(preset, value);
					onChange.run();
				});
			}
			return new OptionSlider<>(intOption, preset, Slider.Format.INT, intOption.min(), intOption.max(), (raw) -> raw.intValue(), onChange, name);
		}
		if (option instanceof FloatOption floatOption) {
			return new OptionSlider<>(floatOption, preset, Slider.Format.FLOAT, floatOption.min(), floatOption.max(), (raw) -> raw.floatValue(), onChange, name);
		}
		if (option instanceof BoolOption boolOption) {
			return new OptionCycleButton<>(boolOption, preset, List.of(true, false), OptionWidgets::booleanName, onChange, name);
		}
		if (option instanceof EnumOption<?> enumOption) {
			return createEnum(enumOption, preset, onChange, name);
		}
		throw new IllegalArgumentException("No widget for option " + option);
	}

	private static <E extends Enum<E>> AbstractWidget createEnum(EnumOption<E> option, Preset preset, Runnable onChange, Component name) {
		return new OptionCycleButton<>(option, preset, option.values(), (value) -> Component.literal(option.name(value)), onChange, name);
	}

	private static Component booleanName(boolean value) {
		return Component.translatable(value ? RTFTranslationKeys.GUI_BUTTON_TRUE : RTFTranslationKeys.GUI_BUTTON_FALSE);
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
		private final Format format;

		public OptionSlider(Option<T> option, Preset preset, Format format, T min, T max, Function<Double, T> fromSlider, Runnable onChange, Component name) {
			super(-1, -1, -1, -1, option.get(preset).floatValue(), min.floatValue(), max.floatValue(), name, format, (slider, value) -> {
				T stored = option.set(preset, fromSlider.apply(slider.scaleValue(value)));
				onChange.run();
				return slider.getSliderValue(stored.floatValue());
			});
			this.option = option;
			this.preset = preset;
			this.format = format;
			this.setTooltip(createTooltip(option, Component.literal(format.getMessage(option.defaultValue().doubleValue()))));
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (this.active && this.visible && Screen.hasControlDown() && this.clicked(mouseX, mouseY)) {
				this.option.reset(this.preset);
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
		private final List<T> values;
		private final Function<T, Component> nameGetter;
		private final Component name;

		public OptionCycleButton(Option<T> option, Preset preset, List<T> values, Function<T, Component> nameGetter, Runnable onChange, Component name) {
			super(-1, -1, -1, -1, CommonComponents.EMPTY, (button) -> {
				if (button instanceof OptionCycleButton<?> self) {
					self.cycle(Screen.hasShiftDown() ? -1 : 1);
					onChange.run();
				}
			}, DEFAULT_NARRATION);
			this.option = option;
			this.preset = preset;
			this.values = values;
			this.nameGetter = nameGetter;
			this.name = name;
			this.setTooltip(createTooltip(option, nameGetter.apply(option.defaultValue())));
			this.updateMessage();
		}

		private void cycle(int direction) {
			if (Screen.hasControlDown()) {
				this.option.reset(this.preset);
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
