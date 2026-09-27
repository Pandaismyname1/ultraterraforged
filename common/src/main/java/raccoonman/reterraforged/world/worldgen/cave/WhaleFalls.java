package raccoonman.reterraforged.world.worldgen.cave;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Whale falls: the skeleton of a whale lying on the deep sea floor, a spine of bone with ribs arching over it and a
 * skull at one end. Each fits inside its own chunk.
 */
final class WhaleFalls implements CaveFeatures.Feature {
	// the floor lies at least this deep
	private static final int MIN_DEPTH = 18;
	private static final int LENGTH = 11;

	private final int seed;
	private final float frequency;

	WhaleFalls(int seed, float frequency) {
		this.seed = seed;
		this.frequency = frequency;
	}

	@Override
	public void carve(CaveCarving carving) {
		int cx = carving.chunkX;
		int cz = carving.chunkZ;
		if (CaveFeatures.random(this.seed, cx, cz, 0) >= this.frequency * 0.04F) {
			return;
		}
		boolean alongX = CaveFeatures.random(this.seed, cx, cz, 1) < 0.5F;
		int across = 5 + (int) (CaveFeatures.random(this.seed, cx, cz, 2) * 6.0F);
		int startX = alongX ? carving.minX + 2 : carving.minX + across;
		int startZ = alongX ? carving.minZ + across : carving.minZ + 2;
		int sea = carving.levels.waterY;
		// on a fairly level floor, deep down
		int floor = carving.ground(startX, startZ);
		int end = carving.ground(startX + (alongX ? LENGTH : 0), startZ + (alongX ? 0 : LENGTH));
		if (floor > sea - MIN_DEPTH || Math.abs(end - floor) > 3) {
			return;
		}
		BlockState spine = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z);
		BlockState rib = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		BlockState crossbar = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.Z : Direction.Axis.X);
		for (int i = 0; i < LENGTH; i++) {
			int x = startX + (alongX ? i : 0);
			int z = startZ + (alongX ? 0 : i);
			int y = carving.ground(x, z) + 1;
			carving.set(x, y, z, spine);
			// ribs arching over the middle of the body, every other block
			if (i >= 3 && i <= 8 && (i & 1) == 1) {
				for (int side : new int[] { -2, 2 }) {
					int rx = x + (alongX ? 0 : side);
					int rz = z + (alongX ? side : 0);
					carving.set(rx, y, rz, rib);
					carving.set(rx, y + 1, rz, rib);
					carving.set(rx, y + 2, rz, rib);
				}
				for (int bar = -1; bar <= 1; bar++) {
					carving.set(x + (alongX ? 0 : bar), y + 3, z + (alongX ? bar : 0), crossbar);
				}
			}
		}
		// the skull
		int headX = startX + (alongX ? LENGTH : 0);
		int headZ = startZ + (alongX ? 0 : LENGTH);
		int headY = carving.ground(headX, headZ) + 1;
		for (int a = 0; a < 2; a++) {
			for (int b = -1; b <= 1; b++) {
				carving.set(headX + (alongX ? a : b), headY, headZ + (alongX ? b : a), Blocks.BONE_BLOCK.defaultBlockState());
			}
		}
	}
}
