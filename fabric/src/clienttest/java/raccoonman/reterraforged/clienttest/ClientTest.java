package raccoonman.reterraforged.clienttest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import raccoonman.reterraforged.client.gui.createworld.TerrainState;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import raccoonman.reterraforged.preset.option.PresetOptions;

/**
 * Walks through creating a ReTerraForged world in a real client, taking screenshots along the way. Everything it
 * sees is written to result.txt in the output folder; the game quits when done.
 */
public class ClientTest implements ClientModInitializer {
	private static final int TIMEOUT_TICKS = 20 * 60 * 5;

	private final Path out = Path.of(System.getProperty("reterraforged.clienttest.out", "clienttest"));
	private final List<Step> steps = new ArrayList<>();
	private int stepIndex;
	private int waited;
	private int totalTicks;
	private String worldFolder;

	private record Step(String name, BooleanSupplier ready, int delay, Runnable action) {
	}

	@Override
	public void onInitializeClient() {
		try {
			Files.createDirectories(this.out);
			try (Stream<Path> old = Files.list(this.out)) {
				for (Path file : old.toList()) {
					Files.delete(file);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Minecraft mc = Minecraft.getInstance();
		this.step("title screen", () -> mc.screen instanceof TitleScreen, 40, () -> CreateWorldScreen.openFresh(mc, mc.screen));
		this.step("create world screen", () -> mc.screen instanceof CreateWorldScreen, 20, () -> {
			CreateWorldScreen screen = (CreateWorldScreen) mc.screen;
			this.log("default world type: " + screen.getUiState().getWorldType().describePreset().getString());
			this.log("reterraforged selected: " + TerrainState.of(screen).isReTerraForgedSelected());
			this.screenshot("01_game_tab");
			screen.tabNavigationBar.selectTab(3, false);
		});
		this.step("terrain tab", () -> true, 80, () -> {
			this.screenshot("02_terrain_tab");
			this.log("widgets: " + this.describeWidgets(mc.screen));
			this.button(mc.screen, "Preset").onPress();
		});
		this.step("next preset", () -> true, 80, () -> {
			TerrainState state = TerrainState.of((CreateWorldScreen) mc.screen);
			this.log("preset after cycling: " + state.name().getString());
			this.screenshot("03_next_preset");
			PresetOptions.CONTINENT_SCALE.set(state.preset(), 900);
			state.markEdited();
		});
		this.step("edited", () -> true, 80, () -> {
			this.screenshot("04_small_continents");
			this.button(mc.screen, "Advanced").onPress();
		});
		this.step("advanced editor", () -> mc.screen instanceof PresetConfigScreen, 40, () -> {
			this.screenshot("05_advanced_world");
			((PresetConfigScreen) mc.screen).nextButton.onPress();
			((PresetConfigScreen) mc.screen).nextButton.onPress();
		});
		this.step("caves page", () -> true, 40, () -> {
			this.screenshot("06_advanced_caves");
			for (int i = 0; i < 5; i++) {
				((PresetConfigScreen) mc.screen).nextButton.onPress();
			}
		});
		this.step("structures page", () -> true, 40, () -> {
			this.screenshot("07_advanced_structures");
			((PresetConfigScreen) mc.screen).doneButton.onPress();
		});
		this.step("back from editor", () -> mc.screen instanceof CreateWorldScreen, 20, () -> {
			CreateWorldScreen screen = (CreateWorldScreen) mc.screen;
			TerrainState state = TerrainState.of(screen);
			this.log("after editor: preset " + state.name().getString() + ", continent scale " + PresetOptions.CONTINENT_SCALE.get(state.preset()) + ", edited " + state.isEdited());
			this.screenshot("08_back_in_terrain_tab");
			this.button(screen, "Create New World").onPress();
		});
		this.step("world loaded", () -> mc.level != null && mc.player != null, 200, () -> {
			this.screenshot("09_in_world");
			IntegratedServer server = mc.getSingleplayerServer();
			this.log("world datapacks: " + this.list(server.getWorldPath(LevelResource.DATAPACK_DIR)));
			this.log("enabled packs: " + server.getPackRepository().getSelectedIds());
			server.execute(() -> {
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new LoggingSource(this)), "rtf locate plains");
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new LoggingSource(this)), "rtf locate mountain_chain");
			});
		});
		this.step("commands", () -> true, 100, () -> {
			IntegratedServer server = mc.getSingleplayerServer();
			this.log("worldgen settings lifecycle: " + server.getWorldData().worldGenSettingsLifecycle());
			this.worldFolder = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString();
			mc.level.disconnect();
			mc.clearLevel(new GenericDirtMessageScreen(Component.literal("leaving")));
			mc.setScreen(new TitleScreen());
		});
		this.step("back at title", () -> mc.level == null && mc.screen instanceof TitleScreen, 40, () -> {
			this.log("reopening world " + this.worldFolder);
			mc.createWorldOpenFlows().loadLevel(mc.screen, this.worldFolder);
		});
		this.step("after reopening", () -> true, 100, () -> {
			this.log("screen after reopening: " + (mc.screen == null ? "none (in world)" : mc.screen.getClass().getSimpleName() + " '" + mc.screen.getTitle().getString() + "'"));
			this.log("in world: " + (mc.level != null));
			this.screenshot("10_reopened");
			this.log("done");
			mc.stop();
		});

		ClientTickEvents.END_CLIENT_TICK.register((client) -> this.tick());
	}

	private void step(String name, BooleanSupplier ready, int delay, Runnable action) {
		this.steps.add(new Step(name, ready, delay, action));
	}

	private void tick() {
		if (this.stepIndex >= this.steps.size()) {
			return;
		}
		if (++this.totalTicks > TIMEOUT_TICKS) {
			this.log("TIMEOUT waiting for: " + this.steps.get(this.stepIndex).name() + " (screen " + Minecraft.getInstance().screen + ")");
			this.screenshot("timeout");
			this.stepIndex = this.steps.size();
			Minecraft.getInstance().stop();
			return;
		}
		Step step = this.steps.get(this.stepIndex);
		if (!step.ready().getAsBoolean()) {
			this.waited = 0;
			return;
		}
		if (++this.waited < step.delay()) {
			return;
		}
		this.waited = 0;
		this.stepIndex++;
		try {
			this.log("step: " + step.name());
			step.action().run();
		} catch (Throwable t) {
			this.log("FAILED at " + step.name() + ": " + t);
			for (StackTraceElement element : t.getStackTrace()) {
				this.log("    at " + element);
			}
			this.screenshot("failure");
			this.stepIndex = this.steps.size();
			Minecraft.getInstance().stop();
		}
	}

	private Button button(Screen screen, String text) {
		for (var child : screen.children()) {
			if (child instanceof Button button && button.visible && button.getMessage().getString().startsWith(text)) {
				return button;
			}
		}
		throw new IllegalStateException("No button starting with '" + text + "' in " + this.describeWidgets(screen));
	}

	private String describeWidgets(Screen screen) {
		List<String> widgets = new ArrayList<>();
		for (var child : screen.children()) {
			if (child instanceof AbstractWidget widget) {
				widgets.add(widget.getClass().getSimpleName() + "('" + widget.getMessage().getString() + "' " + widget.getX() + "," + widget.getY() + " " + widget.getWidth() + "x" + widget.getHeight() + (widget.active ? "" : " inactive") + ")");
			}
		}
		return String.join(", ", widgets);
	}

	private String list(Path directory) {
		try (Stream<Path> files = Files.list(directory)) {
			return files.map((file) -> file.getFileName().toString()).toList().toString();
		} catch (IOException e) {
			return "<" + e + ">";
		}
	}

	private void screenshot(String name) {
		try (NativeImage image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
			image.writeToFile(this.out.resolve(name + ".png"));
		} catch (IOException e) {
			this.log("screenshot " + name + " failed: " + e);
		}
	}

	void log(String line) {
		System.out.println("[ClientTest] " + line);
		try {
			Files.writeString(this.out.resolve("result.txt"), line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private record LoggingSource(ClientTest test) implements net.minecraft.commands.CommandSource {
		@Override
		public void sendSystemMessage(Component message) {
			this.test.log("command output: " + message.getString());
		}

		@Override
		public boolean acceptsSuccess() {
			return true;
		}

		@Override
		public boolean acceptsFailure() {
			return true;
		}

		@Override
		public boolean shouldInformAdmins() {
			return false;
		}
	}
}
