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

	// set on a worldgen thread while Streams carves one of its own rivers (MixinRiverCarverFeature)
	private static final ThreadLocal<Boolean> CARVING = ThreadLocal.withInitial(() -> false);

	public static boolean carving() {
		return CARVING.get();
	}

	public static void setCarving(boolean carving) {
		CARVING.set(carving);
	}

	/**
	 * Streams also carves rivers of its own, which in an UltraTerraForged world cut into its rivers and lakes above the
	 * sea: water put in before the features was turned to air, and so was the ground under it, leaving water hanging.
	 * Its rivers still come; a block that is water, or that water rests on, just stays.
	 */
	public static boolean keeps(net.minecraft.server.level.WorldGenRegion region, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		boolean keeps = state.getFluidState().isEmpty()
			&& (!region.getFluidState(pos).isEmpty() || !region.getFluidState(pos.above()).isEmpty())
			&& context(region.getLevel()) != null;
		if (keeps) {
			KEPT.incrementAndGet();
		}
		return keeps;
	}

	// how many blocks keeps has kept this session, for tests
	private static final java.util.concurrent.atomic.AtomicLong KEPT = new java.util.concurrent.atomic.AtomicLong();

	public static long kept() {
		return KEPT.get();
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
