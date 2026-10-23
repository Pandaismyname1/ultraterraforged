package com.pandaismyname1.ultraterraforged.compat.byepregen;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.mixin.byepregen.AccessorNoiseChunk;
import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;

/**
 * Bye Pregen's "arena" fills a chunk with terrain on its own, for the whole height of the world, from a noise chunk
 * UltraTerraForged made only as tall as the terrain there: it read past the end of the noise chunk's columns and
 * crashed creating the world (issue #13). Here its fill is asked for no more than the noise chunk has; above that the
 * chunk stays air, as it does without Bye Pregen. Bye Pregen's classes are reached by name, so UltraTerraForged
 * neither needs nor ships them.
 */
public final class ByePregenCompat {
	public static final String MOD_ID = "byepregen";
	private static final String FILLER = "com.moepus.byepregen.worldgen.arena.ArenaNoiseFiller";

	@Nullable
	private static Handles handles;
	private static boolean failed;

	private record Handles(MethodHandle fill, MethodHandle request, MethodHandle chunk, MethodHandle defaultBlock, MethodHandle minCellY, MethodHandle cellCountY, MethodHandle fluidUpperBound) {
	}

	private ByePregenCompat() {
	}

	public static boolean isEnabled() {
		return ModLoaderUtil.isLoaded(MOD_ID);
	}

	/**
	 * Bye Pregen's fill, asked only for the cells the noise chunk has; null when it asks for no more than that.
	 */
	@Nullable
	public static ChunkAccess fillCulled(NoiseChunk noiseChunk, Object request, Object targets) {
		Handles handles = handles();
		if (handles == null) {
			return null;
		}
		try {
			int cells = ((AccessorNoiseChunk) noiseChunk).getCellCountY();
			int requested = (int) handles.cellCountY().invoke(request);
			if (cells >= requested) {
				return null;
			}
			Object culled = handles.request().invoke((ChunkAccess) handles.chunk().invoke(request), (BlockState) handles.defaultBlock().invoke(request), (int) handles.minCellY().invoke(request), cells, (int) handles.fluidUpperBound().invoke(request));
			return (ChunkAccess) handles.fill().invoke(noiseChunk, culled, targets);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		}
	}

	@Nullable
	private static synchronized Handles handles() {
		if (handles == null && !failed) {
			try {
				MethodHandles.Lookup lookup = MethodHandles.publicLookup();
				Class<?> filler = Class.forName(FILLER);
				Class<?> request = Class.forName(FILLER + "$Request");
				Class<?> targets = Class.forName(FILLER + "$TargetSections");
				handles = new Handles(
					lookup.findStatic(filler, "fill", MethodType.methodType(ChunkAccess.class, NoiseChunk.class, request, targets)).asType(MethodType.methodType(ChunkAccess.class, NoiseChunk.class, Object.class, Object.class)),
					lookup.findConstructor(request, MethodType.methodType(void.class, ChunkAccess.class, BlockState.class, int.class, int.class, int.class)).asType(MethodType.methodType(Object.class, ChunkAccess.class, BlockState.class, int.class, int.class, int.class)),
					getter(lookup, request, "chunk", ChunkAccess.class),
					getter(lookup, request, "defaultBlock", BlockState.class),
					getter(lookup, request, "minCellY", int.class),
					getter(lookup, request, "cellCountY", int.class),
					getter(lookup, request, "globalFluidUpperBound", int.class)
				);
			} catch (ReflectiveOperationException | RuntimeException e) {
				failed = true;
				UTFCommon.LOGGER.warn("This version of Bye Pregen isn't supported; turn its arena off (worldgen.arena.enabled = false in config/byepregen.toml) if creating a world crashes", e);
			}
		}
		return handles;
	}

	private static MethodHandle getter(MethodHandles.Lookup lookup, Class<?> type, String name, Class<?> returns) throws ReflectiveOperationException {
		return lookup.findVirtual(type, name, MethodType.methodType(returns)).asType(MethodType.methodType(returns, Object.class));
	}
}
