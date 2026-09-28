package com.pandaismyname1.ultraterraforged.clienttest;

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
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import com.pandaismyname1.ultraterraforged.client.gui.PresetSharing;
import com.pandaismyname1.ultraterraforged.client.gui.createworld.TerrainState;
import com.pandaismyname1.ultraterraforged.client.gui.screen.SavePresetScreen;
import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import com.pandaismyname1.ultraterraforged.preset.option.PresetOptions;

/**
 * Walks through creating a UltraTerraForged world in a real client, taking screenshots along the way. Everything it
 * sees is written to result.txt in the output folder; the game quits when done.
 */
public class ClientTest implements ClientModInitializer {
	private static final int TIMEOUT_TICKS = 20 * 60 * 5;
	// with -Dultraterraforged.clienttest.tour=true: a default world, visiting each of these, as /utf locate finds them
	private static final boolean TOUR = Boolean.getBoolean("ultraterraforged.clienttest.tour");
	private static final String[] TOUR_STOPS = System.getProperty("ultraterraforged.clienttest.stops", "river,salt_flat,alluvial_fan,glacial_valley,cirque,drumlins,moraine,barrier_island,karst,sinkhole,delta,wetland,lake,peninsula,coastal_island,sand_bar,skerry,volcanic_island,river_island,submarine_canyon,seamount,guyot,blue_hole,coral_reef,ocean_trench,ocean_ridge,sand_waves").split(",");
	private static final String SAVED_PRESET = "ClientTest Preset";

	private final Path out = Path.of(System.getProperty("ultraterraforged.clienttest.out", "clienttest"));
	private final List<Step> steps = new ArrayList<>();
	private int stepIndex;
	private int waited;
	private int totalTicks;
	private String worldFolder;
	@org.jetbrains.annotations.Nullable
	volatile BlockPos volcano;
	// where the last /utf locate led
	@org.jetbrains.annotations.Nullable
	volatile BlockPos located;
	volatile boolean locateDone;

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
		if (TOUR) {
			this.tour(mc);
			ClientTickEvents.END_CLIENT_TICK.register((client) -> this.tick());
			return;
		}
		this.step("title screen", () -> mc.screen instanceof TitleScreen, 40, () -> CreateWorldScreen.openFresh(mc, () -> mc.setScreen(new TitleScreen())));
		this.step("create world screen", () -> mc.screen instanceof CreateWorldScreen, 20, () -> {
			CreateWorldScreen screen = (CreateWorldScreen) mc.screen;
			this.log("default world type: " + screen.getUiState().getWorldType().describePreset().getString());
			this.log("ultraterraforged selected: " + TerrainState.of(screen).isUltraTerraForgedSelected());
			this.screenshot("01_game_tab");
			screen.tabNavigationBar.selectTab(3, false);
		});
		this.step("terrain tab", () -> true, 80, () -> {
			this.screenshot("02_terrain_tab");
			this.log("widgets: " + this.describeWidgets(mc.screen));
			this.button(mc.screen, "Preset").onPress(PRESS);
		});
		this.step("next preset", () -> true, 80, () -> {
			TerrainState state = TerrainState.of((CreateWorldScreen) mc.screen);
			this.log("preset after cycling: " + state.name().getString());
			this.screenshot("03_next_preset");
			this.button(mc.screen, "Copy Code").onPress(PRESS);
			String code = mc.keyboardHandler.getClipboard();
			this.log("share code: " + code.substring(0, Math.min(12, code.length())) + "... (" + code.length() + " characters)");
			this.button(mc.screen, "Paste Code").onPress(PRESS);
			this.log("after pasting: preset " + state.name().getString() + ", edited " + state.isEdited());
			PresetOptions.CONTINENT_SCALE.set(state.preset(), 900);
			state.markEdited();
		});
		this.step("edited", () -> true, 80, () -> {
			this.screenshot("04_small_continents");
			this.button(mc.screen, "Advanced").onPress(PRESS);
		});
		this.step("advanced editor", () -> mc.screen instanceof PresetConfigScreen, 40, () -> {
			this.screenshot("05_advanced_world");
			this.log("editor widgets: " + this.describeWidgets(mc.screen));
			this.searchBox(mc.screen).setValue("river");
		});
		this.step("search", () -> true, 40, () -> {
			this.screenshot("06_search_river");
			this.searchBox(mc.screen).setValue("");
			this.button(mc.screen, "Page").onPress(PRESS);
			this.button(mc.screen, "Page").onPress(PRESS);
		});
		this.step("caves page", () -> true, 40, () -> {
			this.screenshot("07_advanced_caves");
			for (int i = 0; i < 5; i++) {
				this.button(mc.screen, "Page").onPress(PRESS);
			}
		});
		this.step("structures page", () -> true, 40, () -> {
			this.screenshot("08_advanced_structures");
			this.button(mc.screen, "Save As").onPress(PRESS);
		});
		this.step("save preset screen", () -> mc.screen instanceof SavePresetScreen, 20, () -> {
			this.searchBox(mc.screen).setValue(SAVED_PRESET);
			this.screenshot("09_save_preset");
			this.button(mc.screen, "Save").onPress(PRESS);
		});
		this.step("saved", () -> mc.screen instanceof PresetConfigScreen, 20, () -> {
			this.log("saved preset file exists: " + Files.exists(PresetSharing.presetFolder().resolve(SAVED_PRESET + ".json")));
			((PresetConfigScreen) mc.screen).doneButton.onPress(PRESS);
		});
		this.step("back from editor", () -> mc.screen instanceof CreateWorldScreen, 20, () -> {
			CreateWorldScreen screen = (CreateWorldScreen) mc.screen;
			TerrainState state = TerrainState.of(screen);
			this.log("after editor: preset " + state.name().getString() + ", continent scale " + PresetOptions.CONTINENT_SCALE.get(state.preset()) + ", edited " + state.isEdited());
			this.screenshot("10_back_in_terrain_tab");
			this.button(screen, "Create New World").onPress(PRESS);
		});
		this.step("world loaded", () -> mc.level != null && mc.player != null, 200, () -> {
			this.screenshot("11_in_world");
			IntegratedServer server = mc.getSingleplayerServer();
			this.log("world datapacks: " + this.list(server.getWorldPath(LevelResource.DATAPACK_DIR)));
			this.log("enabled packs: " + server.getPackRepository().getSelectedIds());
			server.execute(() -> {
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new LoggingSource(this)), "utf locate plains");
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new LoggingSource(this)), "utf locate mountain_chain");
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new LoggingSource(this)), "utf locate volcano_pipe");
			});
		});
		this.step("rock layers", () -> true, 100, () -> {
			IntegratedServer server = mc.getSingleplayerServer();
			BlockPos center = mc.player.blockPosition();
			server.execute(() -> this.slice(server.overworld(), center, "slice"));
		});
		this.step("volcano", () -> true, 60, () -> {
			if (this.volcano == null) {
				this.log("no volcano found");
				return;
			}
			IntegratedServer server = mc.getSingleplayerServer();
			BlockPos target = this.volcano;
			this.log("flying to the volcano at " + target);
			server.execute(() -> {
				// the locate command reports sea level; stand above the crater's real surface, looking at it
				int surface = server.overworld().getChunk(target.getX() >> 4, target.getZ() >> 4).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, target.getX() & 15, target.getZ() & 15);
				this.volcano = new BlockPos(target.getX(), surface, target.getZ());
				this.log("crater surface at y=" + surface + ", block " + server.overworld().getBlockState(new BlockPos(target.getX(), surface, target.getZ())).getBlock() + " / below " + server.overworld().getBlockState(new BlockPos(target.getX(), surface - 1, target.getZ())).getBlock());
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new LoggingSource(this)), "execute as @a run tp @s " + target.getX() + " " + (surface + 40) + " " + (target.getZ() + 70) + " 180 30");
			});
		});
		this.step("volcano loaded", () -> true, 300, () -> {
			if (this.volcano == null) {
				return;
			}
			this.screenshot("13_volcano");
			IntegratedServer server = mc.getSingleplayerServer();
			BlockPos target = this.volcano;
			server.execute(() -> this.slice(server.overworld(), target, "volcano_slice"));
		});
		this.step("commands", () -> true, 300, () -> {
			IntegratedServer server = mc.getSingleplayerServer();
			this.log("worldgen settings lifecycle: " + server.getWorldData().worldGenSettingsLifecycle());
			this.worldFolder = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString();
			mc.level.disconnect(Component.literal("leaving"));
			mc.disconnectWithSavingScreen();
			mc.setScreen(new TitleScreen());
		});
		this.step("back at title", () -> mc.level == null && mc.screen instanceof TitleScreen, 40, () -> {
			this.log("reopening world " + this.worldFolder);
			mc.createWorldOpenFlows().openWorld(this.worldFolder, () -> mc.setScreen(new TitleScreen()));
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

	private void tour(Minecraft mc) {
		this.step("title screen", () -> mc.screen instanceof TitleScreen, 40, () -> CreateWorldScreen.openFresh(mc, () -> mc.setScreen(new TitleScreen())));
		this.step("create world screen", () -> mc.screen instanceof CreateWorldScreen, 40, () -> {
			this.log("ultraterraforged selected: " + TerrainState.of((CreateWorldScreen) mc.screen).isUltraTerraForgedSelected() + ", preset " + TerrainState.of((CreateWorldScreen) mc.screen).name().getString());
			this.button(mc.screen, "Create New World").onPress(PRESS);
		});
		this.step("world loaded", () -> mc.level != null && mc.player != null, 200, () -> {
			IntegratedServer server = mc.getSingleplayerServer();
			server.execute(() -> {
				this.command(server, "time set noon");
				this.command(server, "gamerule doDaylightCycle false");
				this.command(server, "weather clear 100000");
				this.command(server, "gamerule doWeatherCycle false");
			});
		});
		for (String stop : TOUR_STOPS) {
			this.step("locate " + stop, () -> true, 20, () -> {
				this.located = null;
				this.locateDone = false;
				IntegratedServer server = mc.getSingleplayerServer();
				server.execute(() -> {
					this.command(server, "utf locate " + stop);
					this.locateDone = true;
				});
			});
			this.step("fly to " + stop, () -> this.locateDone, 5, () -> {
				BlockPos target = this.located;
				if (target == null) {
					this.log("no " + stop + " found");
					return;
				}
				IntegratedServer server = mc.getSingleplayerServer();
				boolean sea = java.util.Set.of("barrier_island", "delta", "skerry", "volcanic_island", "submarine_canyon", "seamount", "guyot", "blue_hole", "coral_reef", "ocean_trench", "ocean_ridge", "sand_waves", "coastal_island", "sand_bar").contains(stop);
				int height = sea ? 70 : 40;
				int back = sea ? 90 : 60;
				server.execute(() -> this.command(server, "execute as @a run tp @s " + target.getX() + " " + (target.getY() + height) + " " + (target.getZ() + back) + " 180 " + (sea ? 40 : 30)));
				// hovering there, rather than falling to the ground
				mc.player.getAbilities().flying = true;
				mc.player.onUpdateAbilities();
			});
			this.step("look at " + stop, () -> true, 400, () -> {
				BlockPos target = this.located;
				if (target == null) {
					return;
				}
				this.screenshot("tour_" + stop);
				IntegratedServer server = mc.getSingleplayerServer();
				server.execute(() -> this.surfaceStats(server.overworld(), target, stop));
			});
			if (stop.equals("glacial_valley") || stop.equals("cirque")) {
				this.step("snow at " + stop, () -> true, 20, () -> {
					BlockPos target = this.located;
					if (target == null) {
						return;
					}
					IntegratedServer server = mc.getSingleplayerServer();
					server.execute(() -> this.snowStats(server.overworld(), target, stop));
				});
			}
		}
		this.step("done", () -> true, 20, () -> {
			this.log("done");
			mc.stop();
		});
	}

	private void command(IntegratedServer server, String command) {
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new LoggingSource(this)), command);
	}

	// the blocks on the surface, and on the floor under water, around a place
	private void surfaceStats(ServerLevel level, BlockPos center, String name) {
		java.util.Map<String, Integer> top = new java.util.TreeMap<>();
		java.util.Map<String, Integer> floor = new java.util.TreeMap<>();
		int radius = 32;
		for (int dx = -radius; dx <= radius; dx += 2) {
			for (int dz = -radius; dz <= radius; dz += 2) {
				int x = center.getX() + dx;
				int z = center.getZ() + dz;
				net.minecraft.world.level.chunk.ChunkAccess chunk = level.getChunk(x >> 4, z >> 4);
				int surface = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
				int ground = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15);
				top.merge(this.name(level.getBlockState(new BlockPos(x, surface, z))), 1, Integer::sum);
				if (level.getBlockState(new BlockPos(x, ground + 1, z)).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
					floor.merge(this.name(level.getBlockState(new BlockPos(x, ground, z))), 1, Integer::sum);
				}
			}
		}
		this.log("surface at " + name + " " + center + ": " + top);
		this.log("under water at " + name + ": " + floor);
		// the ground as generated against the ground the heightmap was made with, for a few columns
		if ((Object) level.getChunkSource().randomState() instanceof com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState state && state.generatorContext() != null) {
			StringBuilder line = new StringBuilder("floor against heightmap at " + name + ":");
			for (int i = 0; i < 8; i++) {
				int x = center.getX() + i * 7 - 24;
				int z = center.getZ() + i * 5 - 20;
				com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell cell = new com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell();
				state.generatorContext().lookup.apply(cell, x, z, true);
				int ground = level.getChunk(x >> 4, z >> 4).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15);
				line.append(" ").append(ground).append("/").append(state.generatorContext().levels.scale(cell.height)).append(String.format(" g%.2f ", cell.gradient)).append(cell.terrain.getName()).append(" ").append(this.name(level.getBlockState(new BlockPos(x, ground, z)))).append(";");
			}
			this.log(line.toString());
		}
	}

	// how much of the ground has snow, on slopes facing north and south, by height
	private void snowStats(ServerLevel level, BlockPos center, String name) {
		int radius = 160;
		int[][] snow = new int[2][40];
		int[][] all = new int[2][40];
		for (int dx = -radius; dx <= radius; dx += 2) {
			for (int dz = -radius; dz <= radius; dz += 2) {
				int x = center.getX() + dx;
				int z = center.getZ() + dz;
				int y = this.ground(level, x, z);
				int slopeZ = this.ground(level, x, z + 3) - this.ground(level, x, z - 3);
				int slopeX = this.ground(level, x + 3, z) - this.ground(level, x - 3, z);
				// steep and facing mostly north (the ground rising to the south) or south
				if (Math.abs(slopeZ) < 4 || Math.abs(slopeX) > Math.abs(slopeZ)) {
					continue;
				}
				int facing = slopeZ > 0 ? 0 : 1;
				int band = Math.max(0, Math.min(39, (y - 60) / 8));
				all[facing][band]++;
				net.minecraft.world.level.block.state.BlockState above = level.getBlockState(new BlockPos(x, y + 1, z));
				net.minecraft.world.level.block.state.BlockState at = level.getBlockState(new BlockPos(x, y, z));
				if (above.is(net.minecraft.world.level.block.Blocks.SNOW) || at.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK) || at.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)) {
					snow[facing][band]++;
				}
			}
		}
		StringBuilder line = new StringBuilder("snow by height at " + name + " (north-facing / south-facing):");
		for (int band = 0; band < 40; band++) {
			if (all[0][band] + all[1][band] > 20) {
				line.append(String.format(" y%d: %d%% of %d / %d%% of %d;", 60 + band * 8, all[0][band] == 0 ? 0 : snow[0][band] * 100 / all[0][band], all[0][band], all[1][band] == 0 ? 0 : snow[1][band] * 100 / all[1][band], all[1][band]));
			}
		}
		this.log(line.toString());
	}

	private int ground(ServerLevel level, int x, int z) {
		return level.getChunk(x >> 4, z >> 4).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15);
	}

	private String name(net.minecraft.world.level.block.state.BlockState state) {
		return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
	}

	private void step(String name, BooleanSupplier ready, int delay, Runnable action) {
		this.steps.add(new Step(name, ready, delay, action));
	}

	private void tick() {
		if (this.stepIndex >= this.steps.size()) {
			return;
		}
		if (++this.totalTicks > (TOUR ? TIMEOUT_TICKS * 8 : TIMEOUT_TICKS)) {
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

	private static final KeyEvent PRESS = new KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0);

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

	// distinct colours for rocks that look alike on a map
	private static final java.util.Map<net.minecraft.world.level.block.Block, Integer> ROCK_COLORS = java.util.Map.ofEntries(
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.STONE, 0x7F7F7F),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.ANDESITE, 0x5E7A8C),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.GRANITE, 0xB5654A),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.DIORITE, 0xE8E8E0),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.TUFF, 0x6E7A55),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.CALCITE, 0xF5F0C8),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.DEEPSLATE, 0x333338),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.LAVA, 0xFF6A00),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK, 0x9A3A10),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.BASALT, 0x4A4A50),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.BLACKSTONE, 0x2A2428)
	);
	private static final int SLICE_SCALE = 3;

	// a vertical cut through the terrain from the bottom of the world to just above the surface, and the rocks in it
	private void slice(ServerLevel level, BlockPos center, String name) {
		int width = 256;
		int minY = level.getMinY();
		int top = minY;
		net.minecraft.world.level.block.state.BlockState[][] states = new net.minecraft.world.level.block.state.BlockState[width][];
		java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < width; dx++) {
			int x = center.getX() - width / 2 + dx;
			int surface = level.getChunk(x >> 4, center.getZ() >> 4).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x & 15, center.getZ() & 15);
			top = Math.max(top, surface);
			states[dx] = new net.minecraft.world.level.block.state.BlockState[level.getMaxY() + 1 - minY];
			for (int y = minY; y <= level.getMaxY(); y++) {
				net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos.set(x, y, center.getZ()));
				states[dx][y - minY] = state;
				if (!state.isAir()) {
					counts.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath(), 1, Integer::sum);
				}
			}
		}
		int height = Math.min(level.getMaxY() + 1, top + 10) - minY;
		try (NativeImage image = new NativeImage(width * SLICE_SCALE, height * SLICE_SCALE, true)) {
			for (int dx = 0; dx < width; dx++) {
				for (int dy = 0; dy < height; dy++) {
					net.minecraft.world.level.block.state.BlockState state = states[dx][dy];
					int color = state.isAir() ? 0x201010 : ROCK_COLORS.getOrDefault(state.getBlock(), state.getMapColor(level, pos.set(center.getX() - width / 2 + dx, dy + minY, center.getZ())).col);
					// NativeImage is ARGB since 1.21.2
					int argb = 0xFF000000 | color;
					for (int px = 0; px < SLICE_SCALE; px++) {
						for (int py = 0; py < SLICE_SCALE; py++) {
							image.setPixel(dx * SLICE_SCALE + px, (height - 1 - dy) * SLICE_SCALE + py, argb);
						}
					}
				}
			}
			image.writeToFile(this.out.resolve(name + ".png"));
		} catch (IOException e) {
			this.log("slice failed: " + e);
		}
		this.log("blocks in " + name + ": " + counts);
	}

	private String list(Path directory) {
		try (Stream<Path> files = Files.list(directory)) {
			return files.map((file) -> file.getFileName().toString()).toList().toString();
		} catch (IOException e) {
			return "<" + e + ">";
		}
	}

	// since 1.21.5 the frame is read back from the graphics card and handed over once it's there
	private void screenshot(String name) {
		Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), (image) -> {
			try (image) {
				image.writeToFile(this.out.resolve(name + ".png"));
			} catch (IOException e) {
				this.log("screenshot " + name + " failed: " + e);
			}
		});
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
			String text = message.getString();
			this.test.log("command output: " + text);
			// e.g. "The nearest volcano_pipe is at [768, 81, 768] (1086 blocks away)"
			java.util.regex.Matcher position = java.util.regex.Pattern.compile("volcano_pipe is at \\[(-?\\d+), (-?\\d+), (-?\\d+)\\]").matcher(text);
			if (position.find()) {
				this.test.volcano = new BlockPos(Integer.parseInt(position.group(1)), Integer.parseInt(position.group(2)), Integer.parseInt(position.group(3)));
			}
			java.util.regex.Matcher any = java.util.regex.Pattern.compile("is at \\[(-?\\d+), (-?\\d+), (-?\\d+)\\]").matcher(text);
			if (any.find()) {
				this.test.located = new BlockPos(Integer.parseInt(any.group(1)), Integer.parseInt(any.group(2)), Integer.parseInt(any.group(3)));
			}
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
