package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.template.FeatureTemplate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/**
 * The tree templates are old structure files, saved without a data version. Read without being brought up to this
 * version's format, 26.3 took every block in them for air and no tree grew.
 */
public class FeatureTemplateTest {

	@TestFactory
	Stream<DynamicTest> treesAreMadeOfLogsAndLeaves() throws IOException, URISyntaxException {
		TestBootstrap.init();
		Path root = Paths.get(FeatureTemplateTest.class.getResource("/data/ultraterraforged/structures/trees").toURI());
		List<Path> templates;
		try (Stream<Path> files = Files.walk(root)) {
			templates = files.filter(file -> file.toString().endsWith(".nbt")).sorted().toList();
		}
		assertTrue(!templates.isEmpty(), "no tree templates found");
		return templates.stream().map(file -> DynamicTest.dynamicTest(root.relativize(file).toString(), () -> {
			try (InputStream in = Files.newInputStream(file)) {
				FeatureTemplate template = FeatureTemplate.load(BuiltInRegistries.BLOCK, in).orElseThrow();
				assertTrue(Arrays.stream(template.getBlocks(Mirror.NONE, Rotation.NONE)).anyMatch(block -> BuiltInRegistries.BLOCK.getKey(block.state().getBlock()).getPath().endsWith("_log")), "no logs in " + file.getFileName());
			}
		}));
	}
}
