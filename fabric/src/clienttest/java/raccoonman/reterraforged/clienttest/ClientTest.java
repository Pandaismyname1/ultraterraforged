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
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import raccoonman.reterraforged.client.gui.PresetSharing;
import raccoonman.reterraforged.client.gui.createworld.TerrainState;
import raccoonman.reterraforged.client.gui.screen.SavePresetScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import raccoonman.reterraforged.preset.option.PresetOptions;

/**
 * Walks through creating a ReTerraForged world in a real client, taking screenshots along the way. Everything it
 * sees is written to result.txt in the output folder; the game quits when done.
 */
public class ClientTest implements ClientModInitializer {
	private static final int TIMEOUT_TICKS = 20 * 60 * 5;
	private static final String SAVED_PRESET = "ClientTest Preset";

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

		try {
			Files.deleteIfExists(PresetSharing.presetFolder().resolve(SAVED_PRESET + ".json"));
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
			this.button(mc.screen, "Copy Code").onPress();
			String code = mc.keyboardHandler.getClipboard();
			this.log("share code: " + code.substring(0, Math.min(12, code.length())) + "... (" + code.length() + " characters)");
			this.button(mc.screen, "Paste Code").onPress();
			this.log("after pasting: preset " + state.name().getString() + ", edited " + state.isEdited());
			PresetOptions.CONTINENT_SCALE.set(state.preset(), 900);
			state.markEdited();
		});
		this.step("edited", () -> true, 80, () -> {
			this.screenshot("04_small_continents");
			this.button(mc.screen, "Advanced").onPress();
		});
		this.step("advanced editor", () -> mc.screen instanceof PresetConfigScreen, 40, () -> {
			this.screenshot("05_advanced_world");
			this.log("editor widgets: " + this.describeWidgets(mc.screen));
			this.searchBox(mc.screen).setValue("river");
		});
		this.step("search", () -> true, 40, () -> {
			this.screenshot("06_search_river");
			this.searchBox(mc.screen).setValue("");
			this.button(mc.screen, "Page").onPress();
			this.button(mc.screen, "Page").onPress();
		});
		this.step("caves page", () -> true, 40, () -> {
			this.screenshot("07_advanced_caves");
			for (int i = 0; i < 5; i++) {
				this.button(mc.screen, "Page").onPress();
			}
		});
		this.step("structures page", () -> true, 40, () -> {
			this.screenshot("08_advanced_structures");
			this.button(mc.screen, "Save As").onPress();
		});
		this.step("save preset screen", () -> mc.screen instanceof SavePresetScreen, 20, () -> {
			this.searchBox(mc.screen).setValue(SAVED_PRESET);
			this.screenshot("09_save_preset");
			this.button(mc.screen, "Save").onPress();
		});
		this.step("saved", () -> mc.screen instanceof PresetConfigScreen, 20, () -> {
			this.log("saved preset file exists: " + Files.exists(PresetSharing.presetFolder().resolve(SAVED_PRESET + ".json")));
			((PresetConfigScreen) mc.screen).doneButton.onPress();
		});
		this.step("back from editor", () -> mc.screen instanceof CreateWorldScreen, 20, () -> {
			CreateWorldScreen screen = (CreateWorldScreen) mc.screen;
			TerrainState state = TerrainState.of(screen);
			this.log("after editor: preset " + state.name().getString() + ", continent scale " + PresetOptions.CONTINENT_SCALE.get(state.preset()) + ", edited " + state.isEdited());
			this.screenshot("10_back_in_terrain_tab");
			this.button(screen, "Create New World").onPress();
		});
		this.step("world loaded", () -> mc.level != null && mc.player != null, 200, () -> {
			this.screenshot("11_in_world");
			IntegratedServer server = mc.getSingleplayerServer();
			this.log("world datapacks: " + this.list(server.getWorldPath(LevelResource.DATAPACK_DIR)));
			this.log("enabled packs: " + server.getPackRepository().getSelectedIds());
			server.execute(() -> {
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new LoggingSource(this)), "rtf locate plains");
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new LoggingSource(this)), "rtf locate mountain_chain");
			});
		});
		this.step("rock layers", () -> true, 100, () -> {
			IntegratedServer server = mc.getSingleplayerServer();
			BlockPos center = mc.player.blockPosition();
			server.execute(() -> this.slice(server.overworld(), center));
		});
		this.step("commands", () -> true, 300, () -> {
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
			this.screenshot("12_reopened");
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

	private EditBox searchBox(Screen screen) {
		for (var child : screen.children()) {
			if (child instanceof EditBox box) {
				return box;
			}
		}
		throw new IllegalStateException("No text box in " + this.describeWidgets(screen));
	}

	private AbstractButton button(Screen screen, String text) {
		for (var child : screen.children()) {
			if (child instanceof AbstractButton button && button.visible && button.getMessage().getString().startsWith(text)) {
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

	// a vertical cut through the terrain, coloured like a map, and the rocks found in it
	private void slice(ServerLevel level, BlockPos center) {
		int width = 256;
		int minY = level.getMinBuildHeight();
		int height = level.getMaxBuildHeight() - minY;
		java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
		try (NativeImage image = new NativeImage(width, height, true)) {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int dx = 0; dx < width; dx++) {
				int x = center.getX() - width / 2 + dx;
				for (int y = minY; y < minY + height; y++) {
					pos.set(x, y, center.getZ());
					net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
					int color = state.isAir() ? 0xFF000000 : state.getMapColor(level, pos).col;
					// NativeImage is ABGR
					int abgr = 0xFF000000 | (color & 0xFF) << 16 | (color & 0xFF00) | (color >> 16 & 0xFF);
					image.setPixelRGBA(dx, height - 1 - (y - minY), state.isAir() ? 0xFF201010 : abgr);
					if (!state.isAir() && y >= 8) {
						counts.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath(), 1, Integer::sum);
					}
				}
			}
			image.writeToFile(this.out.resolve("slice.png"));
		} catch (IOException e) {
			this.log("slice failed: " + e);
		}
		this.log("blocks above y=8 in the slice: " + counts);
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
