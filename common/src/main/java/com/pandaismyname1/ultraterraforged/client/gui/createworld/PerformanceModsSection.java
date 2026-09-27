package com.pandaismyname1.ultraterraforged.client.gui.createworld;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.compat.performance.PerformanceMods;
import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;

/**
 * The performance mods part of the Terrain tab: a light for how well set up the game is, then a row for each mod,
 * saying whether it's installed or what to get. Clicking a mod opens its page.
 */
final class PerformanceModsSection {
	// the mods can't change while the game runs
	private static PerformanceMods.Report report;

	private static PerformanceMods.Report report() {
		if (report == null) {
			report = PerformanceMods.report();
		}
		return report;
	}

	/**
	 * The light on its own, small enough to sit beside the tab's status line; clicking it shows the list.
	 */
	static AbstractWidget light(Runnable show) {
		return new Light(report(), show);
	}

	static List<AbstractWidget> widgets(Screen screen) {
		PerformanceMods.Report report = report();
		String loader = ModLoaderUtil.loaderName();
		List<AbstractWidget> widgets = new ArrayList<>();
		widgets.add(new Header(report));
		for (PerformanceMods.Status status : report.statuses()) {
			widgets.add(new Row(screen, status, loader));
		}
		return widgets;
	}

	static int color(PerformanceMods.Light light) {
		return switch (light) {
			case RED -> 0xFFE04040;
			case ORANGE -> 0xFFF08A24;
			case YELLOW -> 0xFFF0D030;
			case GREEN -> 0xFF40C040;
		};
	}

	private static String loaderName(String loader) {
		return loader.equals(PerformanceMods.FABRIC) ? "Fabric" : "Forge";
	}

	private static Tooltip tooltip(PerformanceMods.Report report) {
		String light = switch (report.light()) {
			case RED -> UTFTranslationKeys.GUI_PERFORMANCE_MODS_LIGHT_RED;
			case ORANGE -> UTFTranslationKeys.GUI_PERFORMANCE_MODS_LIGHT_ORANGE;
			case YELLOW -> UTFTranslationKeys.GUI_PERFORMANCE_MODS_LIGHT_YELLOW;
			case GREEN -> UTFTranslationKeys.GUI_PERFORMANCE_MODS_LIGHT_GREEN;
		};
		return Tooltip.create(Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_TITLE).withStyle(ChatFormatting.BOLD)
			.append(CommonComponents.NEW_LINE).append(Component.translatable(light).withStyle(ChatFormatting.RESET))
			.append(CommonComponents.NEW_LINE).append(CommonComponents.NEW_LINE)
			.append(Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_HELP).withStyle(ChatFormatting.GRAY)));
	}

	// a small round lamp with a darker rim, 8 pixels across, centred on the given height
	private static void drawLamp(GuiGraphics graphics, int x, int middle, PerformanceMods.Light light) {
		int color = color(light);
		graphics.fill(x + 1, middle - 4, x + 7, middle + 4, 0xFF202020);
		graphics.fill(x, middle - 3, x + 8, middle + 3, 0xFF202020);
		graphics.fill(x + 2, middle - 3, x + 6, middle + 3, color);
		graphics.fill(x + 1, middle - 2, x + 7, middle + 2, color);
		graphics.fill(x + 2, middle - 2, x + 4, middle, 0x80FFFFFF);
	}

	private static class Light extends AbstractWidget {
		private final PerformanceMods.Report report;
		private final Runnable show;

		Light(PerformanceMods.Report report, Runnable show) {
			super(0, 0, 0, 0, Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_TITLE));
			this.report = report;
			this.show = show;
			this.setTooltip(tooltip(report));
		}

		static int width(Font font, PerformanceMods.Report report) {
			return 12 + font.width(count(report));
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			Font font = Minecraft.getInstance().font;
			int middle = this.getY() + this.height / 2;
			drawLamp(graphics, this.getX(), middle, this.report.light());
			graphics.drawString(font, count(this.report), this.getX() + 11, middle - 4, this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
		}

		@Override
		public void onClick(double mouseX, double mouseY) {
			this.show.run();
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, this.getMessage());
		}
	}

	static int lightWidth() {
		return Light.width(Minecraft.getInstance().font, report());
	}

	private static Component count(PerformanceMods.Report report) {
		return Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_COUNT, report.installed(), report.available());
	}

	private static class Header extends AbstractWidget {
		private final PerformanceMods.Report report;

		Header(PerformanceMods.Report report) {
			super(0, 0, 0, 0, Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_TITLE));
			this.report = report;
			this.active = false;
			this.setTooltip(tooltip(report));
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			Font font = Minecraft.getInstance().font;
			int middle = this.getY() + this.height / 2;
			int x = this.getX() + 1;
			int color = color(this.report.light());
			drawLamp(graphics, x, middle, this.report.light());
			graphics.drawString(font, this.getMessage().copy().withStyle(ChatFormatting.BOLD), x + 13, middle - 4, 0xFFFFFFFF);
			Component count = count(this.report);
			graphics.drawString(font, count, this.getX() + this.width - font.width(count) - 2, middle - 4, color);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, this.getMessage());
		}
	}

	private static class Row extends AbstractWidget {
		private final Screen screen;
		private final PerformanceMods.Status status;
		private final Component state;
		private final int stateColor;
		private final String url;

		Row(Screen screen, PerformanceMods.Status status, String loader) {
			super(0, 0, 0, 0, Component.literal(status.mod().name()));
			this.screen = screen;
			this.status = status;
			PerformanceMods.Mod mod = status.mod();
			PerformanceMods.Build shown = status.installed() != null ? status.installed() : status.recommended();

			if (status.installed() != null) {
				this.state = Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_INSTALLED);
				this.stateColor = 0xFF55FF55;
			} else if (status.recommended() != null) {
				this.state = Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_MISSING);
				this.stateColor = 0xFFFFD040;
			} else if (mod.availableFrom() != null) {
				this.state = Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_NEEDS_VERSION, mod.availableFrom());
				this.stateColor = 0xFF808080;
			} else if (mod.builds(loader).stream().anyMatch(PerformanceMods.Build::viaConnector)) {
				this.state = Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_NEEDS_CONNECTOR);
				this.stateColor = 0xFFB0B0B0;
			} else {
				this.state = Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_NOT_ON_LOADER, loaderName(loader));
				this.stateColor = 0xFF808080;
			}
			boolean connector = mod.builds(loader).stream().anyMatch(PerformanceMods.Build::viaConnector);
			// with nothing to get until Connector is there, the link goes to Connector
			this.url = shown != null ? shown.url() : connector ? PerformanceMods.CONNECTOR_URL : mod.url();

			MutableComponent tooltip = Component.translatable(UTFTranslationKeys.performanceModCategory(mod.category().name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.AQUA)
				.append(CommonComponents.NEW_LINE)
				.append(Component.translatable(UTFTranslationKeys.performanceModDescription(mod.key())).withStyle(ChatFormatting.WHITE));
			if (shown != null && shown.alternative() && !shown.name().equals(mod.name()) && !mod.builds(loader).stream().anyMatch((build) -> !build.alternative())) {
				tooltip.append(CommonComponents.NEW_LINE).append(CommonComponents.NEW_LINE)
					.append(Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_STANDS_IN, mod.name(), loaderName(loader), shown.name()).withStyle(ChatFormatting.GRAY));
			}
			if (connector) {
				tooltip.append(CommonComponents.NEW_LINE).append(CommonComponents.NEW_LINE)
					.append(Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_VIA_CONNECTOR, mod.name(), loaderName(loader)).withStyle(ChatFormatting.GRAY));
			}
			if (status.installed() != null) {
				tooltip.append(CommonComponents.NEW_LINE).append(Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_RUNNING, status.installed().name()).withStyle(ChatFormatting.GREEN));
			} else if (status.recommended() != null) {
				tooltip.append(CommonComponents.NEW_LINE).append(Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_GET, status.recommended().name()).withStyle(ChatFormatting.YELLOW));
			}
			tooltip.append(CommonComponents.NEW_LINE).append(Component.translatable(UTFTranslationKeys.GUI_PERFORMANCE_MODS_OPEN).withStyle(ChatFormatting.DARK_GRAY));
			this.setTooltip(Tooltip.create(tooltip));
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			Font font = Minecraft.getInstance().font;
			if (this.isHoveredOrFocused()) {
				graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x30FFFFFF);
			}
			int middle = this.getY() + this.height / 2 - 4;
			boolean installed = this.status.installed() != null;
			boolean available = this.status.isAvailable();
			graphics.drawString(font, installed ? "✔" : available ? "○" : "-", this.getX() + 2, middle, installed ? 0xFF55FF55 : available ? 0xFFFFD040 : 0xFF606060);
			// an installed stand-in shows under its own name
			PerformanceMods.Build shown = installed ? this.status.installed() : this.status.recommended();
			Component name = shown != null && !shown.name().equals(this.status.mod().name())
				? Component.literal(shown.name()).append(Component.literal(" (" + this.status.mod().name() + ")").withStyle(ChatFormatting.GRAY))
				: this.getMessage();
			graphics.drawString(font, name, this.getX() + 13, middle, available ? 0xFFFFFFFF : 0xFF909090);
			graphics.drawString(font, this.state, this.getX() + this.width - font.width(this.state) - 2, middle, this.stateColor);
		}

		@Override
		public void onClick(double mouseX, double mouseY) {
			ConfirmLinkScreen.confirmLinkNow(this.url, this.screen, true);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, Component.empty().append(this.getMessage()).append(", ").append(this.state));
		}
	}

	private PerformanceModsSection() {
	}
}
