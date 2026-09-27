package raccoonman.reterraforged.world.worldgen.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;

/**
 * Carving caves into one chunk, once its surface is built: hollowing out ellipsoids, lining them with other rock, and
 * reading the height of the ground inside and around the chunk.
 */
public final class CaveCarving {
	// no water in what's hollowed out
	public static final int DRY = Integer.MIN_VALUE;
	// the hollow may break through the surface
	public static final int OPEN = Integer.MIN_VALUE;
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final BlockState WATER = Blocks.WATER.defaultBlockState();

	public final ChunkAccess chunk;
	public final GeneratorContext context;
	public final Levels levels;
	public final int chunkX;
	public final int chunkZ;
	public final int minX;
	public final int minZ;
	public final int bottom;
	private final int[] ground = new int[256];
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final Cell cell = new Cell();

	CaveCarving(ChunkAccess chunk, GeneratorContext context) {
		this.chunk = chunk;
		this.context = context;
		this.levels = context.levels;
		ChunkPos chunkPos = chunk.getPos();
		this.chunkX = chunkPos.x;
		this.chunkZ = chunkPos.z;
		this.minX = chunkPos.getMinBlockX();
		this.minZ = chunkPos.getMinBlockZ();
		this.bottom = chunk.getMinBuildHeight();
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				this.ground[dz << 4 | dx] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
			}
		}
	}

	/**
	 * The top block of the ground: from the chunk inside it, and from the terrain around it. For carving only: what's
	 * laid out across chunks must use terrainHeight, which is the same from every chunk.
	 */
	public int ground(float x, float z) {
		int bx = (int) Math.floor(x);
		int bz = (int) Math.floor(z);
		int dx = bx - this.minX;
		int dz = bz - this.minZ;
		if (dx >= 0 && dz >= 0 && dx < 16 && dz < 16) {
			return this.ground[dz << 4 | dx];
		}
		return this.levels.scale(this.cell(bx, bz).height);
	}

	/**
	 * The terrain at a place, straight from the heightmap, before erosion: the same whichever chunk asks, and without
	 * generating the tiles around it, so caves that reach across chunks are laid out the same in each. The cell is
	 * reused by the next call.
	 */
	public Cell cell(float x, float z) {
		this.context.lookup.compute(this.cell.reset(), (int) Math.floor(x), (int) Math.floor(z));
		return this.cell;
	}

	// the top block of the ground there, as cell gives it
	public int terrainHeight(float x, float z) {
		return this.levels.scale(this.cell(x, z).height);
	}

	// whether a box reaches into this chunk
	public boolean reaches(float minX, float minZ, float maxX, float maxZ) {
		return maxX >= this.minX && minX < this.minX + 16 && maxZ >= this.minZ && minZ < this.minZ + 16;
	}

	/**
	 * Hollows out an ellipsoid: water up to the given level, air above it. Bedrock and fluids are left alone, and so
	 * is the ground within roof blocks of the surface, unless roof is OPEN.
	 */
	public void hollow(float cx, float cy, float cz, float rx, float ry, float rz, int water, int roof) {
		int x0 = Math.max(this.minX, (int) Math.floor(cx - rx));
		int x1 = Math.min(this.minX + 15, (int) Math.ceil(cx + rx));
		int z0 = Math.max(this.minZ, (int) Math.floor(cz - rz));
		int z1 = Math.min(this.minZ + 15, (int) Math.ceil(cz + rz));
		if (x0 > x1 || z0 > z1) {
			return;
		}
		int y0 = Math.max(this.bottom + 1, (int) Math.floor(cy - ry));
		int y1 = (int) Math.ceil(cy + ry);
		for (int x = x0; x <= x1; x++) {
			float nx = (x + 0.5F - cx) / rx;
			for (int z = z0; z <= z1; z++) {
				float nz = (z + 0.5F - cz) / rz;
				float flat = nx * nx + nz * nz;
				if (flat >= 1.0F) {
					continue;
				}
				int top = roof == OPEN ? y1 : Math.min(y1, this.ground[(z - this.minZ) << 4 | (x - this.minX)] - roof);
				for (int y = y0; y <= top; y++) {
					float ny = (y + 0.5F - cy) / ry;
					if (flat + ny * ny >= 1.0F) {
						continue;
					}
					this.clear(x, y, z, water);
				}
			}
		}
	}

	/**
	 * Hollows out a single block, as hollow does.
	 */
	public void clear(int x, int y, int z, int water) {
		this.pos.set(x, y, z);
		BlockState state = this.chunk.getBlockState(this.pos);
		if (state.is(Blocks.BEDROCK) || !state.getFluidState().isEmpty()) {
			return;
		}
		BlockState fill = y <= water ? WATER : AIR;
		if (state != fill) {
			this.chunk.setBlockState(this.pos, fill, false);
		}
	}

	/**
	 * Replaces the solid rock in a shell around an ellipsoid, out to the given thickness in blocks, with a lining,
	 * leaving the surface itself alone. Hollow the ellipsoid after lining it.
	 */
	public void line(float cx, float cy, float cz, float rx, float ry, float rz, float thickness, Lining lining) {
		float outerX = rx + thickness;
		float outerY = ry + thickness;
		float outerZ = rz + thickness;
		int x0 = Math.max(this.minX, (int) Math.floor(cx - outerX));
		int x1 = Math.min(this.minX + 15, (int) Math.ceil(cx + outerX));
		int z0 = Math.max(this.minZ, (int) Math.floor(cz - outerZ));
		int z1 = Math.min(this.minZ + 15, (int) Math.ceil(cz + outerZ));
		if (x0 > x1 || z0 > z1) {
			return;
		}
		int y0 = Math.max(this.bottom + 1, (int) Math.floor(cy - outerY));
		int y1 = (int) Math.ceil(cy + outerY);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				int top = Math.min(y1, this.ground[(z - this.minZ) << 4 | (x - this.minX)] - 1);
				for (int y = y0; y <= top; y++) {
					float ox = (x + 0.5F - cx) / outerX;
					float oy = (y + 0.5F - cy) / outerY;
					float oz = (z + 0.5F - cz) / outerZ;
					if (ox * ox + oy * oy + oz * oz >= 1.0F) {
						continue;
					}
					this.pos.set(x, y, z);
					BlockState state = this.chunk.getBlockState(this.pos);
					if (state.isAir() || state.is(Blocks.BEDROCK) || !state.getFluidState().isEmpty()) {
						continue;
					}
					this.chunk.setBlockState(this.pos, lining.at(x, y, z), false);
				}
			}
		}
	}

	public BlockState get(int x, int y, int z) {
		return this.chunk.getBlockState(this.pos.set(x, y, z));
	}

	public void set(int x, int y, int z, BlockState state) {
		this.chunk.setBlockState(this.pos.set(x, y, z), state, false);
	}

	// whether a block is in this chunk
	public boolean contains(int x, int z) {
		return x >= this.minX && z >= this.minZ && x < this.minX + 16 && z < this.minZ + 16;
	}

	/**
	 * What lines a cave, which may vary from block to block.
	 */
	public interface Lining {
		BlockState at(int x, int y, int z);
	}
}
