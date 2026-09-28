package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.UTFMaterialContext;

// a condition on the cell under a column, worked out once per column
abstract class CellCondition extends MaterialRuleContext.LazyXZCondition {
	@Nullable
	private final ChunkCellReader cells;

	@Nullable
	protected final GeneratorContext generatorContext;

	public CellCondition(MaterialRuleContext context) {
		super(context);
		this.generatorContext = UTFMaterialContext.generatorContext(context);
		this.cells = this.generatorContext != null ? new ChunkCellReader(this.generatorContext) : null;
	}

	public abstract boolean test(Cell cell, int x, int z);

	@Override
	protected boolean compute() {
		if(this.cells == null) {
			return false;
		}
        int x = this.context.blockX();
        int z = this.context.blockZ();
        return this.test(this.cells.getCell(x, z), x, z);
	}
}
