package raccoonman.reterraforged.world.worldgen.terrain;

public class CompositeTerrain extends Terrain {
    private boolean flat;
    private float erosion;
    private Terrain a;
    private Terrain b;
    
    CompositeTerrain(int id, Terrain a, Terrain b) {
        super(id, a.getName() + "-" + b.getName(), getDominant(a, b));
        this.flat = (a.isFlat() && b.isFlat());
        this.erosion = Math.min(a.erosionModifier(), b.erosionModifier());
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean includes(Terrain terrain) {
        return this.a.includes(terrain) || this.b.includes(terrain);
    }
    
    @Override
    public float erosionModifier() {
        return this.erosion;
    }
    
    @Override
    public boolean isFlat() {
        return this.flat;
    }
    
    private static Terrain getDominant(Terrain a, Terrain b) {
        TerrainCategory typeA = a.getCategory();
        TerrainCategory typeB = a.getCategory();
        TerrainCategory dom = typeA.getDominant(typeB);
        if (dom == typeA) {
            return a;
        }
        return b;
    }
}
