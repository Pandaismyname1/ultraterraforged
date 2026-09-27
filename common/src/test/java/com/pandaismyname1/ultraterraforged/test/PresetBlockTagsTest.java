package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.SharedConstants;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.VanillaRegistries;
import com.pandaismyname1.ultraterraforged.data.preset.tags.PresetBlockTagsProvider;

/**
 * The preset's block tags are generated in game, in a pack of their own. Minecraft's tag generator refuses required
 * references to tags that pack doesn't define, such as vanilla's, which broke creating worlds.
 */
public class PresetBlockTagsTest {

	@Test
	void tagsGenerateOnTheirOwn(@TempDir Path dir) throws Exception {
		TestBootstrap.init();
		DataGenerator generator = new DataGenerator(dir.resolve("cache"), SharedConstants.getCurrentVersion(), true);
		DataGenerator.PackGenerator pack = generator.new PackGenerator(true, "preset", new PackOutput(dir.resolve("pack")));
		pack.addProvider((output) -> new PresetBlockTagsProvider(output, CompletableFuture.completedFuture(VanillaRegistries.createLookup())));
		generator.run();

		Path rock = dir.resolve("pack/data/ultraterraforged/tags/blocks/rock.json");
		assertTrue(Files.exists(rock), "rock tag wasn't written");
		String json = Files.readString(rock);
		// mods' stones come in through the vanilla stone tags
		assertTrue(json.contains("#minecraft:stone_ore_replaceables") && json.contains("#minecraft:base_stone_overworld"), json);
		assertTrue(Files.exists(dir.resolve("pack/data/ultraterraforged/tags/blocks/strata_excluded.json")));
	}
}
