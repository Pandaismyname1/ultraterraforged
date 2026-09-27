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
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.config.ModpackConfig;
import raccoonman.reterraforged.data.preset.settings.BuiltinPresets;
import raccoonman.reterraforged.data.preset.settings.Preset;

/**
 * The presets a player can pick from: the modpack's (config/reterraforged/modpack-presets), the built-in ones and
 * those saved in config/reterraforged/presets.
 */
public final class PresetLibrary {

	public enum Source {
		BUILTIN,
		// shipped by a modpack; players can start from them but never change the files
		MODPACK,
		// saved by the player
		USER,
		// e.g. pasted from a share code
		OTHER
	}

	/**
	 * @param id stable identifier, "builtin/<name>", "modpack/<file name>" or "file/<file name>"
	 * @param factory creates a fresh, independently editable copy
	 */
	public record Entry(String id, Component name, @Nullable Component description, Supplier<Preset> factory, @Nullable Path file, Source source) {

		public Preset create() {
			return this.factory.get();
		}
	}

	public static final String DEFAULT_ID = "builtin/default";
	// optional fields a modpack preset can carry next to the settings
	private static final String NAME_KEY = "name";
	private static final String DESCRIPTION_KEY = "description";
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

	/**
	 * The presets that come with the game, as set up by the modpack in the config folder.
	 */
	public static List<Entry> shipped() {
		return shipped(ModpackConfig.load(), ModpackConfig.presetFolder());
	}

	/**
	 * The preset new worlds start from, as set up by the modpack in the config folder.
	 */
	public static Entry defaultPreset() {
		ModpackConfig config = ModpackConfig.load();
		return defaultPreset(config, shipped(config, ModpackConfig.presetFolder()));
	}

	/**
	 * The modpack's presets followed by the built-in ones. The built-in presets are left out only if the modpack
	 * asks for it and actually has presets of its own.
	 */
	public static List<Entry> shipped(ModpackConfig config, Path modpackFolder) {
		List<Entry> modpack = modpack(modpackFolder);
		List<Entry> entries = new ArrayList<>(modpack);
		if (config.showBuiltinPresets() || modpack.isEmpty()) {
			entries.addAll(BUILTINS);
		}
		return entries;
	}

	/**
	 * The entry the modpack chose as the default, or Default (the first entry if that's hidden) when it chose none
	 * or one that doesn't exist.
	 */
	public static Entry defaultPreset(ModpackConfig config, List<Entry> entries) {
		String reference = config.defaultPreset();
		if (reference != null && !reference.isBlank()) {
			Optional<Entry> chosen = find(reference.trim(), entries);
			if (chosen.isPresent()) {
				return chosen.get();
			}
			RTFCommon.LOGGER.warn("The default preset \"{}\" in {} isn't one of the available presets {}", reference, ModpackConfig.FILE, entries.stream().map(Entry::id).toList());
		}
		for (Entry entry : entries) {
			if (entry.id().equals(DEFAULT_ID)) {
				return entry;
			}
		}
		return entries.isEmpty() ? BUILTINS.get(0) : entries.get(0);
	}

	/**
	 * Finds an entry by its full id, or else by its name within the id ("highlands", "My Preset"), ignoring case.
	 * Earlier entries win, so a modpack preset shadows a built-in one of the same name.
	 */
	public static Optional<Entry> find(String reference, List<Entry> entries) {
		for (Entry entry : entries) {
			if (entry.id().equals(reference)) {
				return Optional.of(entry);
			}
		}
		for (Entry entry : entries) {
			String name = entry.id().substring(entry.id().indexOf('/') + 1);
			if (name.endsWith(".json")) {
				name = name.substring(0, name.length() - ".json".length());
			}
			if (name.equalsIgnoreCase(reference)) {
				return Optional.of(entry);
			}
		}
		return Optional.empty();
	}

	/**
	 * Presets saved in the given folders; files that can't be read are logged and skipped.
	 */
	public static List<Entry> files(Path... folders) {
		List<Entry> entries = new ArrayList<>();
		for (Path folder : folders) {
			for (Path file : list(folder)) {
				readJson(file).flatMap((json) -> parse(file, json)).ifPresent((preset) -> {
					entries.add(new Entry("file/" + file.getFileName(), Component.literal(baseName(file)), null, factory(file), file, Source.USER));
				});
			}
		}
		return entries;
	}

	/**
	 * Presets a modpack ships. Besides the settings, each file may have a "name" and a "description", which can be
	 * translation keys; the name defaults to the file name.
	 */
	public static List<Entry> modpack(Path folder) {
		List<Entry> entries = new ArrayList<>();
		for (Path file : list(folder)) {
			readJson(file).ifPresent((json) -> parse(file, json).ifPresent((preset) -> {
				JsonObject object = json.getAsJsonObject();
				String name = string(object, NAME_KEY);
				String description = string(object, DESCRIPTION_KEY);
				entries.add(new Entry(
					"modpack/" + file.getFileName(),
					name != null ? Component.translatableWithFallback(name, name) : Component.literal(baseName(file)),
					description != null ? Component.translatableWithFallback(description, description) : null,
					factory(file),
					file,
					Source.MODPACK
				));
			}));
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
		return new Entry("file/" + file.getFileName(), Component.literal(name.trim()), null, factory(file), file, Source.USER);
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

	private static List<Path> list(Path folder) {
		if (!Files.isDirectory(folder)) {
			return List.of();
		}
		try (Stream<Path> stream = Files.list(folder)) {
			return stream.filter((file) -> Files.isRegularFile(file) && file.getFileName().toString().endsWith(".json")).sorted(Comparator.comparing(Path::getFileName)).toList();
		} catch (IOException e) {
			RTFCommon.LOGGER.error("Couldn't list presets in {}", folder, e);
			return List.of();
		}
	}

	// reads the file again each time, so every entry created from it is a fresh copy
	private static Supplier<Preset> factory(Path file) {
		return () -> readJson(file).flatMap((json) -> parse(file, json)).orElseGet(BuiltinPresets::makeDefault);
	}

	private static Optional<JsonElement> readJson(Path file) {
		try (Reader reader = Files.newBufferedReader(file)) {
			return Optional.of(JsonParser.parseReader(reader));
		} catch (IOException | RuntimeException e) {
			RTFCommon.LOGGER.error("Couldn't read preset {}", file, e);
			return Optional.empty();
		}
	}

	private static Optional<Preset> parse(Path file, JsonElement json) {
		return Preset.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial((error) -> {
			RTFCommon.LOGGER.error("Couldn't read preset {}: {}", file, error);
		});
	}

	private static String baseName(Path file) {
		String name = file.getFileName().toString();
		return name.substring(0, name.length() - ".json".length());
	}

	@Nullable
	private static String string(JsonObject json, String key) {
		JsonElement element = json.get(key);
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? element.getAsString() : null;
	}

	private static Entry builtin(String id, String translationId, Supplier<Preset> factory) {
		Component name = Component.translatable(RTFTranslationKeys.presetName(translationId)).withStyle(ChatFormatting.GRAY);
		return new Entry("builtin/" + id, name, Component.translatable(RTFTranslationKeys.presetDescription(translationId)), factory, null, Source.BUILTIN);
	}

	private PresetLibrary() {
	}
}
