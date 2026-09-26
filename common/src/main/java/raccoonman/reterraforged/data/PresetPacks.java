package raccoonman.reterraforged.data;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.apache.commons.io.file.PathUtils;

import net.minecraft.core.HolderLookup;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.preset.settings.Preset;

// Turns a preset into the datapack a world is created with
public final class PresetPacks {
	// the name the preset pack has in a world's datapacks folder
	public static final String WORLD_PACK_NAME = "reterraforged-preset.zip";

	/**
	 * Writes the datapack for a preset as a zip, replacing any existing file.
	 *
	 * @param registries the registries the preset refers to, such as the world's worldgen registries or
	 *                   {@link net.minecraft.data.registries.VanillaRegistries#createLookup()}
	 */
	public static void export(Preset preset, HolderLookup.Provider registries, Path zip) throws IOException {
		Path workDir = Files.createTempDirectory("reterraforged-preset-");
		try {
			Path generated = workDir.resolve("pack");
			RTFDataGen.makePreset(preset, registries, workDir.resolve("cache"), generated).run();

			// build next to the target and swap it in, so a half-written or stale pack is never picked up
			Path parent = zip.toAbsolutePath().getParent();
			Files.createDirectories(parent);
			Path partial = Files.createTempFile(parent, "reterraforged-preset-", ".zip.tmp");
			Files.delete(partial);
			zip(generated, partial);
			Files.move(partial, zip, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			PathUtils.deleteDirectory(workDir);
		}
		RTFCommon.LOGGER.info("Wrote preset datapack to {}", zip);
	}

	private static void zip(Path directory, Path zip) throws IOException {
		URI uri = URI.create("jar:" + zip.toUri());
		try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Map.of("create", "true"))) {
			PathUtils.copyDirectory(directory, fileSystem.getPath("/"));
		}
	}

	private PresetPacks() {
	}
}
