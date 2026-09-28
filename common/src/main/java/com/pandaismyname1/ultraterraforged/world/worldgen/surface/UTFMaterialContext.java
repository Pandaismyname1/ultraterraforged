package com.pandaismyname1.ultraterraforged.world.worldgen.surface;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import com.pandaismyname1.ultraterraforged.mixin.MaterialRuleContextAccessor;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;

/**
 * What UltraTerraForged's material rules and conditions find through vanilla's material rule context. Since 26.3 the
 * context doesn't know the chunk, so the material system's mixin keeps the chunk it's building on this thread.
 */
public final class UTFMaterialContext {
	private static final ThreadLocal<ChunkAccess> CHUNK = new ThreadLocal<>();

	private UTFMaterialContext() {
	}

	public static RandomState randomState(MaterialRuleContext context) {
		return ((MaterialRuleContextAccessor) (Object) context).ultraterraforged$randomState();
	}

	@Nullable
	public static UTFRandomState utfRandomState(MaterialRuleContext context) {
		return (Object) randomState(context) instanceof UTFRandomState utfRandomState ? utfRandomState : null;
	}

	@Nullable
	public static GeneratorContext generatorContext(MaterialRuleContext context) {
		UTFRandomState randomState = utfRandomState(context);
		return randomState != null ? randomState.generatorContext() : null;
	}

	public static UTFSurfaceSystem surfaceSystem(MaterialRuleContext context) {
		return (UTFSurfaceSystem) ((MaterialRuleContextAccessor) (Object) context).ultraterraforged$system();
	}

	// the chunk whose surface this thread is building, or null outside of it
	@Nullable
	public static ChunkAccess chunk() {
		return CHUNK.get();
	}

	public static void setChunk(@Nullable ChunkAccess chunk) {
		if (chunk != null) {
			CHUNK.set(chunk);
		} else {
			CHUNK.remove();
		}
	}
}
