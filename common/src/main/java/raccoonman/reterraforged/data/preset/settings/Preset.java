package raccoonman.reterraforged.data.preset.settings;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import raccoonman.reterraforged.compat.terrablender.TBNoiseRouterData;
import raccoonman.reterraforged.data.preset.PresetBiomeData;
import raccoonman.reterraforged.data.preset.PresetBiomeModifierData;
import raccoonman.reterraforged.data.preset.PresetConfiguredCarvers;
import raccoonman.reterraforged.data.preset.PresetConfiguredFeatures;
import raccoonman.reterraforged.data.preset.PresetData;
import raccoonman.reterraforged.data.preset.PresetDimensionTypes;
import raccoonman.reterraforged.data.preset.PresetNoiseData;
import raccoonman.reterraforged.data.preset.PresetNoiseGeneratorSettings;
import raccoonman.reterraforged.data.preset.PresetNoiseParameters;
import raccoonman.reterraforged.data.preset.PresetNoiseRouterData;
import raccoonman.reterraforged.data.preset.PresetPlacedFeatures;
import raccoonman.reterraforged.data.preset.PresetStructureRuleData;
import raccoonman.reterraforged.data.preset.PresetStructureSets;
import raccoonman.reterraforged.data.preset.PresetSurfaceLayerData;
import raccoonman.reterraforged.registries.RTFRegistries;

//TODO make this actually immutable when we rework the gui
public record Preset(WorldSettings world, SurfaceSettings surface, CaveSettings caves, ClimateSettings climate, TerrainSettings terrain, RiverSettings rivers, FilterSettings filters, StructureSettings structures, MiscellaneousSettings miscellaneous, LandformSettings landforms, CoastSettings coasts, CaveFeatureSettings caveFeatures, OceanSettings oceans) {
	private static final Codec<Preset> UNVERSIONED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		WorldSettings.CODEC.fieldOf("world").forGetter(Preset::world),
		// presets are mutable, so a missing section must get its own default instance rather than a shared one
		SurfaceSettings.CODEC.optionalFieldOf("surface").xmap((surface) -> surface.orElseGet(Preset::defaultSurface), Optional::of).forGetter(Preset::surface),
		CaveSettings.CODEC.optionalFieldOf("caves").xmap((caves) -> caves.orElseGet(CaveSettings::makeVanilla), Optional::of).forGetter(Preset::caves),
		ClimateSettings.CODEC.fieldOf("climate").forGetter(Preset::climate),
		TerrainSettings.CODEC.fieldOf("terrain").forGetter(Preset::terrain),
		RiverSettings.CODEC.fieldOf("rivers").forGetter(Preset::rivers),
		FilterSettings.CODEC.fieldOf("filters").forGetter(Preset::filters),
		StructureSettings.CODEC.optionalFieldOf("structures").xmap((structures) -> structures.orElseGet(StructureSettings::new), Optional::of).forGetter(Preset::structures),
		MiscellaneousSettings.CODEC.fieldOf("miscellaneous").forGetter(Preset::miscellaneous),
		// added after the first releases; older presets get today's landforms
		LandformSettings.CODEC.optionalFieldOf("landforms").xmap((landforms) -> landforms.orElseGet(LandformSettings::makeDefault), Optional::of).forGetter(Preset::landforms),
		CoastSettings.CODEC.optionalFieldOf("coasts").xmap((coasts) -> coasts.orElseGet(CoastSettings::makeDefault), Optional::of).forGetter(Preset::coasts),
		CaveFeatureSettings.CODEC.optionalFieldOf("caveFeatures").xmap((caveFeatures) -> caveFeatures.orElseGet(CaveFeatureSettings::makeDefault), Optional::of).forGetter(Preset::caveFeatures),
		OceanSettings.CODEC.optionalFieldOf("oceans").xmap((oceans) -> oceans.orElseGet(OceanSettings::makeDefault), Optional::of).forGetter(Preset::oceans)
	).apply(instance, Preset::new));

	// what preset files and datapacks use; handles upgrading presets saved by older versions
	public static final Codec<Preset> CODEC = PresetFormat.versioned(UNVERSIONED_CODEC);

	private static SurfaceSettings defaultSurface() {
		return new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 95, 0.65F, 0.475F, 0.4F, 0.45F, 6.0F, 3.0F, SurfaceSettings.Erosion.DEFAULT_SNOW_ASPECT));
	}
	
	public Preset copy() {
		return new Preset(this.world.copy(), this.surface.copy(), this.caves.copy(), this.climate.copy(), this.terrain.copy(), this.rivers.copy(), this.filters.copy(), this.structures.copy(), this.miscellaneous.copy(), this.landforms.copy(), this.coasts.copy(), this.caveFeatures.copy(), this.oceans.copy());
	}

	public HolderLookup.Provider buildPatch(HolderLookup.Provider registries) {
		RegistrySetBuilder builder = new RegistrySetBuilder();
		this.addPatch(builder, RTFRegistries.PRESET, PresetData::bootstrap);
		this.addPatch(builder, RTFRegistries.NOISE, PresetNoiseData::bootstrap);
		this.addPatch(builder, RTFRegistries.BIOME_MODIFIER, PresetBiomeModifierData::bootstrap);
		this.addPatch(builder, RTFRegistries.STRUCTURE_RULE, PresetStructureRuleData::bootstrap);
		this.addPatch(builder, RTFRegistries.SURFACE_LAYERS, PresetSurfaceLayerData::bootstrap);
		this.addPatch(builder, Registries.CONFIGURED_FEATURE, (preset, ctx) -> {
			PresetConfiguredFeatures.bootstrap(preset, ctx);
		});
		this.addPatch(builder, Registries.CONFIGURED_CARVER, (preset, ctx) -> {
			PresetConfiguredCarvers.bootstrap(preset, ctx);	
		});
		this.addPatch(builder, Registries.STRUCTURE_SET, (preset, ctx) -> PresetStructureSets.bootstrap(preset, ctx, registries.lookupOrThrow(Registries.STRUCTURE_SET)));
		this.addPatch(builder, Registries.PLACED_FEATURE, PresetPlacedFeatures::bootstrap);
		this.addPatch(builder, Registries.BIOME, PresetBiomeData::bootstrap);
		this.addPatch(builder, Registries.DIMENSION_TYPE, PresetDimensionTypes::bootstrap);
		this.addPatch(builder, Registries.NOISE, PresetNoiseParameters::bootstrap);
		this.addPatch(builder, Registries.DENSITY_FUNCTION, (preset, ctx) -> {
			PresetNoiseRouterData.bootstrap(preset, ctx);
			TBNoiseRouterData.bootstrap(ctx);
		});
		this.addPatch(builder, Registries.NOISE_SETTINGS, PresetNoiseGeneratorSettings::bootstrap);
		return builder.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), registries);
	}
	
	private <T> void addPatch(RegistrySetBuilder builder, ResourceKey<? extends Registry<T>> key, Patch<T> patch) {
    	builder.add(key, (ctx) -> {
    		patch.apply(this, ctx);
    	});
    }
    
	private interface Patch<T> {
        void apply(Preset preset, BootstapContext<T> ctx);
	}
}
