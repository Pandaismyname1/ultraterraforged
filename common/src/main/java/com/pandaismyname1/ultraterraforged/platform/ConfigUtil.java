package com.pandaismyname1.ultraterraforged.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import dev.architectury.injectables.annotations.ExpectPlatform;
import com.pandaismyname1.ultraterraforged.RTFCommon;

public class ConfigUtil {
	public static final Path RTF_CONFIG_PATH = getConfigPath().resolve(RTFCommon.MOD_ID);
	public static final Path LEGACY_CONFIG_PATH = getConfigPath().resolve(RTFCommon.LEGACY_MOD_ID);
	
	public static Path rtf(String path) {
		return RTF_CONFIG_PATH.resolve(path);
	}
	
	public static Path legacy(String path) {
		return LEGACY_CONFIG_PATH.resolve(path);
	}
	
	@ExpectPlatform
	public static Path getConfigPath() {
		throw new IllegalStateException();
	}
	
	// where ReTerraForged, which UltraTerraForged continues, kept the same files
	private static final String RETERRAFORGED_ID = "reterraforged";

	static {
		if(!Files.exists(RTF_CONFIG_PATH)) {
			try {
				Path previous = getConfigPath().resolve(RETERRAFORGED_ID);
				if(Files.isDirectory(previous)) {
					// players coming from ReTerraForged keep their saved presets, modpack setup and server preset
					copy(previous, RTF_CONFIG_PATH);
					RTFCommon.LOGGER.info("Copied the ReTerraForged config folder {} to {}", previous, RTF_CONFIG_PATH);
				} else {
					Files.createDirectory(RTF_CONFIG_PATH);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void copy(Path from, Path to) throws IOException {
		try (Stream<Path> files = Files.walk(from)) {
			for (Path file : (Iterable<Path>) files::iterator) {
				Path target = to.resolve(from.relativize(file).toString());
				if (Files.isDirectory(file)) {
					Files.createDirectories(target);
				} else {
					Files.copy(file, target);
				}
			}
		}
	}
}
