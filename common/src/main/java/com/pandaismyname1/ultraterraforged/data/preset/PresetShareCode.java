package com.pandaismyname1.ultraterraforged.data.preset;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;

/**
 * A preset as one line of text that can be pasted into chat or a forum post: {@code UTF1:} followed by the
 * compressed preset file in URL-safe base64. The preset file carries its own format version, so codes made by
 * older versions of the mod keep working.
 */
public final class PresetShareCode {
	public static final String PREFIX = "UTF1:";
	// a preset is a few kilobytes; anything much larger is not a preset code
	private static final int MAX_SIZE = 1 << 20;

	public static String encode(Preset preset) {
		JsonElement json = Preset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow();
		byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
		try {
			deflater.setInput(bytes);
			deflater.finish();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			while (!deflater.finished()) {
				out.write(buffer, 0, deflater.deflate(buffer));
			}
			return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
		} finally {
			deflater.end();
		}
	}

	public static DataResult<Preset> decode(String code) {
		// codes get wrapped or padded with spaces when they're copied around
		String trimmed = code.replaceAll("\\s", "");
		if (!trimmed.startsWith(PREFIX)) {
			return DataResult.error(() -> "Not a UltraTerraForged preset code");
		}
		byte[] compressed;
		try {
			compressed = Base64.getUrlDecoder().decode(trimmed.substring(PREFIX.length()));
		} catch (IllegalArgumentException e) {
			return DataResult.error(() -> "Damaged preset code: " + e.getMessage());
		}
		Inflater inflater = new Inflater();
		try {
			inflater.setInput(compressed);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			while (!inflater.finished()) {
				int read = inflater.inflate(buffer);
				if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
					return DataResult.error(() -> "Incomplete preset code");
				}
				out.write(buffer, 0, read);
				if (out.size() > MAX_SIZE) {
					return DataResult.error(() -> "Preset code is too large");
				}
			}
			JsonElement json = JsonParser.parseString(out.toString(StandardCharsets.UTF_8));
			return Preset.CODEC.parse(JsonOps.INSTANCE, json);
		} catch (DataFormatException | JsonParseException e) {
			return DataResult.error(() -> "Damaged preset code: " + e.getMessage());
		} finally {
			inflater.end();
		}
	}

	private PresetShareCode() {
	}
}
