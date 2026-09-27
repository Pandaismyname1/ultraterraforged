package com.pandaismyname1.ultraterraforged.world.worldgen.terrain;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TerrainType {
    private static final Object LOCK = new Object();
    public static final List<Terrain> REGISTRY = new CopyOnWriteArrayList<>();
    public static final Terrain NONE = register("none", TerrainCategory.NONE);
    public static final Terrain DEEP_OCEAN = register("deep_ocean", TerrainCategory.DEEP_OCEAN);
    public static final Terrain SHALLOW_OCEAN = register("ocean", TerrainCategory.SHALLOW_OCEAN);
    public static final Terrain COAST = register("coast", TerrainCategory.COAST);
    public static final Terrain BEACH = register("beach", TerrainCategory.BEACH);
    public static final Terrain RIVER = register("river", TerrainCategory.RIVER);
    public static final Terrain RIVER_BANKS = register("river_banks", TerrainCategory.FLATLAND);
    public static final Terrain LAKE = register("lake", TerrainCategory.LAKE);
    public static final Terrain WETLAND = registerWetlands("wetland", TerrainCategory.WETLAND);
    public static final Terrain PLAINS = register("plains", TerrainCategory.FLATLAND);
    public static final Terrain STEPPE = register("steppe", TerrainCategory.FLATLAND);
    public static final Terrain BORDER = register("border", TerrainCategory.FLATLAND);
    public static final Terrain BADLANDS = registerBadlands("badlands", TerrainCategory.FLATLAND);
    public static final Terrain PLATEAU = register("plateau", TerrainCategory.LOWLAND);
    public static final Terrain HILLS_1 = register("hills_1", TerrainCategory.LOWLAND);
    public static final Terrain HILLS_2 = register("hills_2", TerrainCategory.LOWLAND);
    public static final Terrain DALES = register("dales", TerrainCategory.LOWLAND);
    public static final Terrain TORRIDONIAN = register("torridonian", TerrainCategory.LOWLAND);
    public static final Terrain MOUNTAINS_1 = registerMountain("mountains_1", TerrainCategory.HIGHLAND);
    public static final Terrain MOUNTAINS_2 = registerMountain("mountains_2", TerrainCategory.HIGHLAND);
    public static final Terrain MOUNTAINS_3 = registerMountain("mountains_3", TerrainCategory.HIGHLAND);
    public static final Terrain DOLOMITES = registerMountain("dolomites", TerrainCategory.HIGHLAND);
    public static final Terrain MOUNTAIN_CLIFFS = registerMountain("mountains_cliffs", TerrainCategory.HIGHLAND);
    public static final Terrain MOUNTAIN_CHAIN = registerMountain("mountain_chain", TerrainCategory.HIGHLAND);
    public static final Terrain VOLCANO = registerVolcano("volcano", TerrainCategory.HIGHLAND);
    public static final Terrain VOLCANO_PIPE = registerVolcano("volcano_pipe", TerrainCategory.HIGHLAND);

    public static final Terrain REMOTE_ISLANDS = register("remote_islands", TerrainCategory.ISLAND);
    public static final Terrain LAGOON = register("lagoon", TerrainCategory.ISLAND);
    public static final Terrain DEEP_LAGOON = register("deep_lagoon", TerrainCategory.ISLAND);
    public static final Terrain ARCHIPELAGO = register("archipelago", TerrainCategory.ISLAND);
    public static final Terrain MUSHROOM_ARCHIPELAGO = register("mushroom_archipelago", TerrainCategory.ISLAND);
    public static final Terrain MUSHROOM_FIELDS = register("mushroom_fields", TerrainCategory.ISLAND);
    // the rolling land around a volcano's cone; the cone itself is VOLCANO
    public static final Terrain VOLCANIC_LOWLANDS = registerVolcano("volcanic_lowlands", TerrainCategory.HIGHLAND);
    // sand dune fields in deserts; their steep faces stay sand
    public static final Terrain DUNES = registerLandform("dunes", TerrainCategory.FLATLAND);
    // the narrow gravel beach at the foot of a sea cliff
    public static final Terrain SHINGLE_BEACH = registerLandform("shingle_beach", TerrainCategory.BEACH);
    // outcrops of bare rock on hilltops, and the boulders strewn around them
    public static final Terrain TOR = registerLandform("tor", TerrainCategory.LOWLAND);
    // dead flat salt crust in desert basins
    public static final Terrain SALT_FLAT = registerLandform("salt_flat", TerrainCategory.FLATLAND);
    // cones of gravel and sand at the foot of mountains
    public static final Terrain ALLUVIAL_FAN = registerLandform("alluvial_fan", TerrainCategory.LOWLAND);
    // the flat floors of U-shaped valleys, and the bowls of cirques, in cold mountains
    public static final Terrain GLACIAL_VALLEY = registerLandform("glacial_valley", TerrainCategory.HIGHLAND);
    public static final Terrain CIRQUE = registerLandform("cirque", TerrainCategory.HIGHLAND);
    // oval hills, and ridges of rubble, left by ice sheets on cold lowlands
    public static final Terrain DRUMLINS = registerLandform("drumlins", TerrainCategory.LOWLAND);
    public static final Terrain MORAINE = registerLandform("moraine", TerrainCategory.LOWLAND);
    public static final Terrain BARRIER_ISLAND = registerLandform("barrier_island", TerrainCategory.BEACH);
    // limestone tower hills and sinkholes
    public static final Terrain KARST = registerLandform("karst", TerrainCategory.LOWLAND);
    public static final Terrain SINKHOLE = registerLandform("sinkhole", TerrainCategory.LOWLAND);
    // the marshy islands and channels where a river fans out into the sea
    public static final Terrain DELTA = registerLandform("delta", TerrainCategory.WETLAND);
    // the reshaped coast: arms of land, islands off the coast, and bars of sand
    public static final Terrain PENINSULA = registerLandform("peninsula", TerrainCategory.LOWLAND);
    public static final Terrain COASTAL_ISLAND = registerLandform("coastal_island", TerrainCategory.LOWLAND);
    public static final Terrain SAND_BAR = registerLandform("sand_bar", TerrainCategory.BEACH);
    // small bare rocky islets
    public static final Terrain SKERRY = registerLandform("skerry", TerrainCategory.COAST);
    // islands in lakes and wide rivers
    public static final Terrain RIVER_ISLAND = registerLandform("river_island", TerrainCategory.FLATLAND);
    // the cones of volcanic island arcs
    public static final Terrain VOLCANIC_ISLAND = registerVolcano("volcanic_island", TerrainCategory.HIGHLAND);
    // the sea floor's features
    public static final Terrain SUBMARINE_CANYON = registerLandform("submarine_canyon", TerrainCategory.SHALLOW_OCEAN);
    public static final Terrain OCEAN_TRENCH = registerLandform("ocean_trench", TerrainCategory.DEEP_OCEAN);
    public static final Terrain SEAMOUNT = registerLandform("seamount", TerrainCategory.DEEP_OCEAN);
    public static final Terrain GUYOT = registerLandform("guyot", TerrainCategory.DEEP_OCEAN);
    public static final Terrain OCEAN_RIDGE = registerLandform("ocean_ridge", TerrainCategory.DEEP_OCEAN);
    public static final Terrain BLUE_HOLE = registerLandform("blue_hole", TerrainCategory.SHALLOW_OCEAN);
    public static final Terrain CORAL_REEF = registerLandform("coral_reef", TerrainCategory.SHALLOW_OCEAN);
    public static final Terrain SAND_WAVES = registerLandform("sand_waves", TerrainCategory.SHALLOW_OCEAN);
    
    public static void forEach(Consumer<Terrain> action) {
        TerrainType.REGISTRY.forEach(action);
    }
    
    public static Optional<Terrain> find(Predicate<Terrain> filter) {
        return TerrainType.REGISTRY.stream().filter(filter).findFirst();
    }
    
    public static Terrain getOrCreate(String name, Terrain parent) {
        if (parent == null || parent == TerrainType.NONE) {
            return TerrainType.NONE;
        }
        Terrain current = get(name);
        if (current != null) {
            return current;
        }
        return register(new Terrain(0, name, parent));
    }
    
    public static Terrain get(String name) {
        for (Terrain terrain : TerrainType.REGISTRY) {
            if (terrain.getName().equalsIgnoreCase(name)) {
                return terrain;
            }
        }
        return null;
    }
    
    public static Terrain get(int id) {
        synchronized (TerrainType.LOCK) {
            if (id >= 0 && id < TerrainType.REGISTRY.size()) {
                return TerrainType.REGISTRY.get(id);
            }
            return TerrainType.NONE;
        }
    }
    
    public static Terrain register(Terrain instance) {
        synchronized (TerrainType.LOCK) {
            Terrain current = get(instance.getName());
            if (current != null) {
                return current;
            }
            Terrain terrain = instance.withId(TerrainType.REGISTRY.size());
            TerrainType.REGISTRY.add(terrain);
            return terrain;
        }
    }
    
    public static Terrain registerComposite(Terrain a, Terrain b) {
        if (a == b) {
            return a;
        }
        synchronized (TerrainType.LOCK) {
            Terrain min = (a.getId() < b.getId()) ? a : b;
            Terrain max = (a.getId() > b.getId()) ? a : b;
            Terrain current = get(min.getName() + "-" + max.getName());
            if (current != null) {
                return current;
            }
            CompositeTerrain mix = new CompositeTerrain(TerrainType.REGISTRY.size(), min, max);
            TerrainType.REGISTRY.add(mix);
            return mix;
        }
    }
    
    private static Terrain register(String name, TerrainCategory type) {
        synchronized (TerrainType.LOCK) {
            Terrain terrain = new Terrain(TerrainType.REGISTRY.size(), name, type);
            TerrainType.REGISTRY.add(terrain);
            return terrain;
        }
    }
    
    private static Terrain registerWetlands(String name, TerrainCategory type) {
        synchronized (TerrainType.LOCK) {
            Terrain terrain = new ConfiguredTerrain(TerrainType.REGISTRY.size(), name, type, true);
            TerrainType.REGISTRY.add(terrain);
            return terrain;
        }
    }
    
    private static Terrain registerBadlands(String name, TerrainCategory type) {
        synchronized (TerrainType.LOCK) {
            Terrain terrain = new ConfiguredTerrain(TerrainType.REGISTRY.size(), name, type, 0.3f);
            TerrainType.REGISTRY.add(terrain);
            return terrain;
        }
    }
    
    private static Terrain registerMountain(String name, TerrainCategory type) {
        synchronized (TerrainType.LOCK) {
            Terrain terrain = new ConfiguredTerrain(TerrainType.REGISTRY.size(), name, type, true, true);
            TerrainType.REGISTRY.add(terrain);
            return terrain;
        }
    }
    
    // a landform's own type, kept rather than relabelled coast or beach with the rest of the shore, since the surface
    // rules look for it
    private static Terrain registerLandform(String name, TerrainCategory type) {
        synchronized (TerrainType.LOCK) {
            Terrain terrain = new Terrain(TerrainType.REGISTRY.size(), name, type) {
                @Override
                public boolean isCoast() {
                    return false;
                }
                
                @Override
                public boolean overridesCoast() {
                    return true;
                }
            };
            TerrainType.REGISTRY.add(terrain);
            return terrain;
        }
    }
    
    private static Terrain registerVolcano(String name, TerrainCategory type) {
        synchronized (TerrainType.LOCK) {
            Terrain terrain = new ConfiguredTerrain(TerrainType.REGISTRY.size(), name, type, true, true) {
                @Override
                public boolean isVolcano() {
                    return true;
                }
                
                @Override
                public boolean overridesCoast() {
                    return true;
                }
            };
            TerrainType.REGISTRY.add(terrain);
            return terrain;
        }
    }
}
