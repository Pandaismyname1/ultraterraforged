package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;

/**
 * A stand-in world for testing caves: stone up to the generated ground, the sea over it where it's low, and whatever
 * the caves carve, chunk by chunk, as they would in the game. Only what's carved is stored.
 */
final class CaveWorld {
	static final int BOTTOM = -64;
	static final Path OUT = Path.of("build", "terrain-views");

	final GeneratorContext context;
	private final Long2IntOpenHashMap ground = new Long2IntOpenHashMap();
	private final Long2ObjectOpenHashMap<BlockState> carved = new Long2ObjectOpenHashMap<>();
	final List<BlockPos> postprocessed = new ArrayList<>();

	CaveWorld(GeneratorContext context) {
		this.context = context;
	}

	int ground(int x, int z) {
		long key = BlockPos.asLong(x, 0, z);
		if (this.ground.containsKey(key)) {
			return this.ground.get(key);
		}
		Cell cell = new Cell();
		this.context.lookup.apply(cell, x, z, true);
		int ground = this.context.levels.scale(cell.height);
		this.ground.put(key, ground);
		return ground;
	}

	BlockState get(int x, int y, int z) {
		BlockState state = this.carved.get(BlockPos.asLong(x, y, z));
		if (state != null) {
			return state;
		}
		return this.original(x, y, z);
	}

	BlockState original(int x, int y, int z) {
		if (y <= BOTTOM) {
			return Blocks.BEDROCK.defaultBlockState();
		}
		int ground = this.ground(x, z);
		if (y <= ground) {
			return Blocks.STONE.defaultBlockState();
		}
		if (y <= this.context.levels.waterY) {
			return Blocks.WATER.defaultBlockState();
		}
		return Blocks.AIR.defaultBlockState();
	}

	// carves the chunks that overlap a box with the given features
	void carve(float minX, float minZ, float maxX, float maxZ, CaveFeatures.Feature... features) {
		int cx0 = (int) Math.floor(minX) >> 4;
		int cz0 = (int) Math.floor(minZ) >> 4;
		int cx1 = (int) Math.floor(maxX) >> 4;
		int cz1 = (int) Math.floor(maxZ) >> 4;
		for (int cx = cx0; cx <= cx1; cx++) {
			for (int cz = cz0; cz <= cz1; cz++) {
				CaveCarving carving = new CaveCarving(this.chunk(cx, cz), this.context);
				for (CaveFeatures.Feature feature : features) {
					feature.carve(carving);
				}
			}
		}
	}

	// a chunk of this world, carving into it
	ChunkAccess chunk(int chunkX, int chunkZ) {
		// (stub only: recording every call would fill the memory)
		ChunkAccess chunk = mock(ChunkAccess.class, org.mockito.Mockito.withSettings().stubOnly());
		ChunkPos pos = new ChunkPos(chunkX, chunkZ);
		when(chunk.getPos()).thenReturn(pos);
		when(chunk.getMinBuildHeight()).thenReturn(BOTTOM);
		when(chunk.getMaxBuildHeight()).thenReturn(320);
		when(chunk.getHeight(any(Heightmap.Types.class), anyInt(), anyInt())).thenAnswer((invocation) -> this.ground(pos.getMinBlockX() + (int) invocation.getArgument(1), pos.getMinBlockZ() + (int) invocation.getArgument(2)));
		when(chunk.getBlockState(any(BlockPos.class))).thenAnswer((invocation) -> {
			BlockPos at = invocation.getArgument(0);
			return this.get(at.getX(), at.getY(), at.getZ());
		});
		when(chunk.setBlockState(any(BlockPos.class), any(BlockState.class), anyBoolean())).thenAnswer((invocation) -> {
			BlockPos at = invocation.getArgument(0);
			BlockState old = this.get(at.getX(), at.getY(), at.getZ());
			this.carved.put(at.asLong(), invocation.getArgument(1));
			return old;
		});
		org.mockito.Mockito.doAnswer((invocation) -> {
			this.postprocessed.add(((BlockPos) invocation.getArgument(0)).immutable());
			return null;
		}).when(chunk).markPosForPostprocessing(any(BlockPos.class));
		return chunk;
	}

	// a probe for finding where caves go, without carving
	CaveCarving probe() {
		return new CaveCarving(this.chunk(0, 0), this.context);
	}

	// how many carved blocks match
	int count(Predicate<BlockState> test) {
		int count = 0;
		for (BlockState state : this.carved.values()) {
			if (test.test(state)) {
				count++;
			}
		}
		return count;
	}

	Iterable<Long> carvedPositions() {
		return this.carved.keySet();
	}

	// a cut straight down through the world along x or z, around a place
	void writeSlice(String name, int x, int z, boolean alongX, int width, int minY, int maxY) throws IOException {
		int height = maxY - minY;
		int scale = 3;
		BufferedImage image = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < width; i++) {
			int bx = alongX ? x - width / 2 + i : x;
			int bz = alongX ? z : z - width / 2 + i;
			for (int y = minY; y < maxY; y++) {
				int color = color(this.get(bx, y, bz));
				for (int px = 0; px < scale; px++) {
					for (int py = 0; py < scale; py++) {
						image.setRGB(i * scale + px, (maxY - 1 - y) * scale + py, color);
					}
				}
			}
		}
		Files.createDirectories(OUT);
		ImageIO.write(image, "png", OUT.resolve(name + ".png").toFile());
	}

	// looking down on what's carved: the highest carved block under each column, coloured by what it is
	void writeMap(String name, int centerX, int centerZ, int size) throws IOException {
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		int[] top = new int[size * size];
		int[] colors = new int[size * size];
		java.util.Arrays.fill(top, Integer.MIN_VALUE);
		for (Long key : this.carved.keySet()) {
			BlockPos pos = BlockPos.of(key);
			int ix = pos.getX() - centerX + size / 2;
			int iz = pos.getZ() - centerZ + size / 2;
			if (ix < 0 || iz < 0 || ix >= size || iz >= size) {
				continue;
			}
			int index = iz * size + ix;
			if (pos.getY() > top[index]) {
				top[index] = pos.getY();
				colors[index] = color(this.carved.get(key.longValue()));
			}
		}
		for (int i = 0; i < size * size; i++) {
			image.setRGB(i % size, i / size, top[i] == Integer.MIN_VALUE ? 0x707070 : colors[i]);
		}
		Files.createDirectories(OUT);
		ImageIO.write(image, "png", OUT.resolve(name + ".png").toFile());
	}

	private static int color(BlockState state) {
		if (state.isAir()) {
			return 0x101018;
		}
		if (state.is(Blocks.WATER)) {
			return 0x3060D0;
		}
		if (state.is(Blocks.BASALT) || state.is(Blocks.BLACKSTONE) || state.is(Blocks.SMOOTH_BASALT)) {
			return 0x303038;
		}
		if (state.is(Blocks.BLUE_ICE)) {
			return 0x70A0FF;
		}
		if (state.is(Blocks.PACKED_ICE)) {
			return 0xA0C0FF;
		}
		if (state.is(Blocks.BEDROCK)) {
			return 0x000000;
		}
		return 0x808080;
	}
}
