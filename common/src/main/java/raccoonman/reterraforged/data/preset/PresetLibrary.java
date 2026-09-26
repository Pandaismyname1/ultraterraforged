package raccoonman.reterraforged.data.preset;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.data.preset.settings.BuiltinPresets;
import raccoonman.reterraforged.data.preset.settings.Preset;

/**
 * The presets a player can pick from: the built-in ones and those saved in config/reterraforged/presets.
 */
public final class PresetLibrary {

	/**
	 * @param id stable identifier, "builtin/<name>" or "file/<file name>"
	 * @param factory creates a fresh, independently editable copy
	 */
	public record Entry(String id, Component name, @Nullable Component description, Supplier<Preset> factory, @Nullable Path file) {

		public boolean isBuiltin() {
			return this.file == null;
		}

		public Preset create() {
			return this.factory.get();
		}
	}

	public static final String DEFAULT_ID = "builtin/default";
	private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_ -]+$");

	private static final List<Entry> BUILTINS = List.of(
		builtin("default", "default", BuiltinPresets::makeDefault),
		builtin("archipelago", "archipelago", BuiltinPresets::makeArchipelago),
		builtin("supercontinent", "supercontinent", BuiltinPresets::makeSupercontinent),
		builtin("highlands", "highlands", BuiltinPresets::makeHighlands),
		builtin("prairie", "prairie", BuiltinPresets::makePrairie),
		builtin("badlands", "badlands", BuiltinPresets::makeBadlands),
		builtin("frozen_north", "frozenNorth", BuiltinPresets::makeFrozenNorth),
		builtin("tropics", "tropics", BuiltinPresets::makeTropics),
		builtin("volcanic_isles", "volcanicIsles", BuiltinPresets::makeVolcanicIsles),
		builtin("waterlands", "waterlands", BuiltinPresets::makeWaterlands),
		builtin("patchwork", "patchwork", BuiltinPresets::makePatchwork),
		// the original TerraForged presets
		builtin("legacy_default", "legacyDefault", BuiltinPresets::makeLegacyDefault),
		builtin("beautiful", "beautiful", BuiltinPresets::makeLegacyBeautiful),
		builtin("huge_biomes", "hugeBiomes", BuiltinPresets::makeLegacyHugeBiomes),
		builtin("lite", "lite", BuiltinPresets::makeLegacyLite),
		builtin("vanillaish", "vanillaish", BuiltinPresets::makeLegacyVanillaish)
	);

	public static List<Entry> builtins() {
		return BUILTINS;
	}

	public static Entry defaultPreset() {
		return BUILTINS.get(0);
	}

	/**
	 * Presets saved in the given folders; files that can't be read are logged and skipped.
	 */
	public static List<Entry> files(Path... folders) {
		List<Entry> entries = new ArrayList<>();
		for (Path folder : folders) {
			if (!Files.isDirectory(folder)) {
				continue;
			}
			List<Path> files;
			try (Stream<Path> stream = Files.list(folder)) {
				files = stream.filter((file) -> Files.isRegularFile(file) && file.getFileName().toString().endsWith(".json")).sorted(Comparator.comparing(Path::getFileName)).toList();
			} catch (IOException e) {
				RTFCommon.LOGGER.error("Couldn't list presets in {}", folder, e);
				continue;
			}
			for (Path file : files) {
				read(file).ifPresent((preset) -> {
					String name = file.getFileName().toString();
					name = name.substring(0, name.length() - ".json".length());
					entries.add(new Entry("file/" + file.getFileName(), Component.literal(name), null, () -> read(file).orElseGet(BuiltinPresets::makeDefault), file));
				});
			}
		}
		return entries;
	}

	// preset names become file names, so keep them to characters every file system accepts
	public static boolean isValidName(String name) {
		return VALID_NAME.matcher(name).matches() && !name.isBlank() && name.length() <= 64;
	}

	public static Path file(Path folder, String name) {
		return folder.resolve(name.trim() + ".json");
	}

	/**
	 * Saves the preset as {@code <name>.json} in the folder, replacing a preset with the same name.
	 *
	 * @return the entry for the saved file
	 */
	public static Entry save(Path folder, String name, Preset preset) throws IOException {
		if (!isValidName(name)) {
			throw new IllegalArgumentException("Invalid preset name: " + name);
		}
		Files.createDirectories(folder);
		Path file = file(folder, name);
		write(file, preset);
		return new Entry("file/" + file.getFileName(), Component.literal(name.trim()), null, () -> read(file).orElseGet(BuiltinPresets::makeDefault), file);
	}

	public static void write(Path file, Preset preset) throws IOException {
		JsonElement json = Preset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow(false, (error) -> {});
		// write next to the target first, so a failed write never leaves half a preset behind
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		try (Writer writer = Files.newBufferedWriter(temp); JsonWriter jsonWriter = new JsonWriter(writer)) {
			jsonWriter.setSerializeNulls(false);
			jsonWriter.setIndent("  ");
			GsonHelper.writeValue(jsonWriter, json, null);
		}
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
	}

	private static java.util.Optional<Preset> read(Path file) {
		try (Reader reader = Files.newBufferedReader(file)) {
			return Preset.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).resultOrPartial((error) -> {
				RTFCommon.LOGGER.error("Couldn't read preset {}: {}", file, error);
			});
		} catch (IOException | RuntimeException e) {
			RTFCommon.LOGGER.error("Couldn't read preset {}", file, e);
			return java.util.Optional.empty();
		}
	}

	private static Entry builtin(String id, String translationId, Supplier<Preset> factory) {
		Component name = Component.translatable(RTFTranslationKeys.presetName(translationId)).withStyle(ChatFormatting.GRAY);
		return new Entry("builtin/" + id, name, Component.translatable(RTFTranslationKeys.presetDescription(translationId)), factory, null);
	}

	private PresetLibrary() {
	}
}
