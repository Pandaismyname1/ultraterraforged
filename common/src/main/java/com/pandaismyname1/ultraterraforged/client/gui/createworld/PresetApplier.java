package com.pandaismyname1.ultraterraforged.client.gui.createworld;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.server.packs.repository.PackRepository;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.data.PresetPacks;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

/**
 * Makes the datapacks of a world being created match the chosen world type: a UltraTerraForged world gets the
 * datapack of its preset, any other world type gets none.
 *
 * The pack is named after a hash of the preset. Vanilla only reloads datapacks when the selection changes, so a
 * changed preset has to get a new name to take effect.
 */
public final class PresetApplier {
	private static final String PACK_PREFIX = "ultraterraforged-preset";

	/**
	 * @return true if the world can be created right away; false if the datapacks are being reloaded, in which case
	 *         the state has a pending selection and creation should continue once it's loaded
	 */
	public static boolean prepareForCreate(CreateWorldScreen screen, TerrainState state) throws IOException {
		Pair<Path, PackRepository> dataPacks = screen.getDataPackSelectionSettings(state.uiState().getSettings().dataConfiguration());
		Path directory = dataPacks.getFirst();
		PackRepository repository = dataPacks.getSecond();

		List<String> current = state.uiState().getSettings().dataConfiguration().dataPacks().getEnabled();
		String wanted = state.isUltraTerraForgedSelected() ? packName(state.preset()) : null;

		List<String> wantedPresetPacks = wanted != null ? List.of("file/" + wanted) : List.of();
		if (presetPacks(current).equals(wantedPresetPacks)) {
			return true;
		}
		List<String> selection = new ArrayList<>(current.stream().filter((id) -> !isPresetPack(id)).toList());
		selection.addAll(wantedPresetPacks);

		if (wanted != null && !Files.exists(directory.resolve(wanted))) {
			PresetPacks.export(state.preset(), state.uiState().getSettings().worldgenLoadContext(), directory.resolve(wanted));
		}
		deleteOtherPresetPacks(directory, wanted);

		repository.reload();
		repository.setSelected(selection);
		if (wanted != null && !repository.getSelectedIds().contains("file/" + wanted)) {
			throw new IOException("The preset datapack " + wanted + " wasn't found in " + directory);
		}
		state.setPendingSelection(List.copyOf(selection));
		screen.tryApplyNewDataPacks(repository, false, (config) -> {
			// only called when loading failed and the player backed out
			state.setPendingSelection(null);
		});
		return false;
	}

	/**
	 * Whether a reload started by {@link #prepareForCreate} has finished, meaning the world can now be created.
	 */
	public static boolean isReadyToCreate(TerrainState state) {
		List<String> pending = state.pendingSelection();
		// vanilla may reorder packs or add newly found ones while loading, so only compare the preset packs
		return pending != null && presetPacks(state.uiState().getSettings().dataConfiguration().dataPacks().getEnabled()).equals(presetPacks(pending));
	}

	static String packName(Preset preset) {
		String json = Preset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow(false, UTFCommon.LOGGER::error).toString();
		try {
			byte[] hash = MessageDigest.getInstance("SHA-1").digest(json.getBytes(StandardCharsets.UTF_8));
			return PACK_PREFIX + "-" + HexFormat.of().formatHex(hash).substring(0, 12) + ".zip";
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static List<String> presetPacks(List<String> packs) {
		return packs.stream().filter(PresetApplier::isPresetPack).toList();
	}

	private static boolean isPresetPack(String id) {
		return PresetPacks.isPresetPack(id);
	}

	private static void deleteOtherPresetPacks(Path directory, String keep) throws IOException {
		if (!Files.isDirectory(directory)) {
			return;
		}
		List<Path> stale;
		try (Stream<Path> files = Files.list(directory)) {
			stale = files.filter((file) -> {
				String name = file.getFileName().toString();
				return name.startsWith(PACK_PREFIX) && name.endsWith(".zip") && !name.equals(keep);
			}).toList();
		}
		for (Path file : stale) {
			Files.deleteIfExists(file);
		}
	}

	private PresetApplier() {
	}
}
