package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import java.util.zip.Deflater;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import com.pandaismyname1.ultraterraforged.data.preset.PresetLibrary;
import com.pandaismyname1.ultraterraforged.data.preset.PresetShareCode;
import com.pandaismyname1.ultraterraforged.data.preset.settings.BuiltinPresets;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

public class PresetShareCodeTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	private static JsonElement json(Preset preset) {
		return Preset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow(false, (error) -> {});
	}

	@Test
	void everyBuiltinPresetSurvivesACode() {
		for (Supplier<Preset> preset : BuiltinPresetRenderTest.presets().values()) {
			String code = PresetShareCode.encode(preset.get());
			assertTrue(code.startsWith(PresetShareCode.PREFIX));
			// short enough to paste into chat or a forum post
			assertTrue(code.length() < 4000, "code is " + code.length() + " characters long");
			Preset decoded = PresetShareCode.decode(code).getOrThrow(false, (error) -> {});
			assertEquals(json(preset.get()), json(decoded));
		}
	}

	@Test
	void editsSurviveACode() {
		Preset preset = BuiltinPresets.makeDefault();
		preset.world().continent.continentScale = 1234;
		preset.rivers().riverCount = 17;
		Preset decoded = PresetShareCode.decode(PresetShareCode.encode(preset)).getOrThrow(false, (error) -> {});
		assertEquals(1234, decoded.world().continent.continentScale);
		assertEquals(17, decoded.rivers().riverCount);
	}

	@Test
	void whitespaceFromCopyingIsIgnored() {
		String code = PresetShareCode.encode(BuiltinPresets.makeDefault());
		String wrapped = "  " + code.substring(0, 20) + "\n" + code.substring(20, 40) + " \r\n" + code.substring(40) + "\n";
		assertTrue(PresetShareCode.decode(wrapped).result().isPresent());
	}

	@Test
	void invalidCodesAreRejected() {
		String code = PresetShareCode.encode(BuiltinPresets.makeDefault());
		for (String invalid : List.of("", "hello", "UTF1:", "UTF1:!!!!", "UTF2:" + code.substring(5), code.substring(0, code.length() / 2), "UTF1:" + Base64.getUrlEncoder().encodeToString("not deflated".getBytes(StandardCharsets.UTF_8)))) {
			assertTrue(PresetShareCode.decode(invalid).result().isEmpty(), "accepted " + invalid);
		}
	}

	@Test
	void hugeCodesAreRejected() {
		// a small code that inflates to many megabytes must not be read into memory
		byte[] zeros = new byte[8 << 20];
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
		deflater.setInput(zeros);
		deflater.finish();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		while (!deflater.finished()) {
			out.write(buffer, 0, deflater.deflate(buffer));
		}
		deflater.end();
		String code = PresetShareCode.PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
		assertTrue(PresetShareCode.decode(code).error().isPresent());
	}

	@Test
	void savedPresetsAreListed(@TempDir Path folder) throws Exception {
		Preset preset = BuiltinPresets.makeDefault();
		preset.rivers().riverCount = 21;
		PresetLibrary.Entry saved = PresetLibrary.save(folder, "My World", preset);
		assertTrue(Files.exists(folder.resolve("My World.json")));
		assertEquals(21, saved.create().rivers().riverCount);

		List<PresetLibrary.Entry> files = PresetLibrary.files(folder);
		assertEquals(1, files.size());
		assertEquals(saved.id(), files.get(0).id());
		assertEquals("My World", files.get(0).name().getString());

		// saving again under the same name replaces it
		preset.rivers().riverCount = 3;
		PresetLibrary.save(folder, "My World", preset);
		assertEquals(3, PresetLibrary.files(folder).get(0).create().rivers().riverCount);
		try (var stream = Files.list(folder)) {
			assertEquals(1, stream.count(), "no temporary files are left behind");
		}
	}

	@Test
	void presetNamesMustBeUsableAsFileNames() {
		for (String valid : List.of("Mine", "my-world_2", "Big Rivers")) {
			assertTrue(PresetLibrary.isValidName(valid), valid);
		}
		for (String invalid : List.of("", "   ", "../escape", "a/b", "con:", "what?", "x".repeat(65))) {
			assertTrue(!PresetLibrary.isValidName(invalid), invalid);
		}
	}
}
