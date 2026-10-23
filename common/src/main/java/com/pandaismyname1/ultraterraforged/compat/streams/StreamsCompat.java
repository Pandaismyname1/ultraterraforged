package com.pandaismyname1.ultraterraforged.compat.streams;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;

/**
 * Streams Reflowing moves river water along as currents. Its own idea of where rivers run and which way is downhill
 * comes from vanilla's river biomes and the sea level, so UltraTerraForged's rivers, raised ones above all, got
 * currents running the wrong way or none. Here UltraTerraForged tells it, through Streams' own API, which way the water
 * of its rivers runs. After Mikaaah's UltraTerraForged-Streams-Currents add-on, whose idea this is (discussion #3).
 *
 * This class mentions none of Streams' classes, so it can be loaded without it; {@link RiverCurrents} does.
 */
public class StreamsCompat {
	public static final String MOD_ID = "streamsreflowing";

	public static boolean isEnabled() {
		return ModLoaderUtil.isLoaded(MOD_ID);
	}

	public static void bootstrap() {
		try {
			RiverCurrents.register();
		} catch (LinkageError e) {
			// a Streams Reflowing whose API changed: no currents rather than a crash
			UTFCommon.LOGGER.warn("This version of Streams Reflowing isn't supported, UltraTerraForged's rivers won't have its currents", e);
		}
	}

	/**
	 * The generator of a world UltraTerraForged makes, or null. Streams only gives the overworld currents.
	 */
	@Nullable
	public static GeneratorContext context(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return null;
		}
		return (Object) level.getChunkSource().randomState() instanceof UTFRandomState randomState ? randomState.generatorContext() : null;
	}
}
