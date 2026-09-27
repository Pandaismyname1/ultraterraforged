package com.pandaismyname1.ultraterraforged.data;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataGenerator.PackGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.pandaismyname1.ultraterraforged.client.data.UTFLanguageProvider;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.data.preset.tags.PresetBiomeTagsProvider;
import com.pandaismyname1.ultraterraforged.data.preset.tags.PresetBlockTagsProvider;
import com.pandaismyname1.ultraterraforged.data.preset.tags.PresetSurfaceLayerProvider;
import com.pandaismyname1.ultraterraforged.platform.DataGenUtil;

public class UTFDataGen {
	public static final String DATAPACK_PATH = "data/ultraterraforged/datapacks";
	
	public static void generateResourcePacks(ResourcePackFactory resourcePackFactory) {
		DataGenerator.PackGenerator pack = resourcePackFactory.createPack();

		pack.addProvider(UTFLanguageProvider.EnglishUS::new);
		pack.addProvider((PackOutput output) -> PackMetadataGenerator.forFeaturePack(output, Component.translatable(UTFTranslationKeys.METADATA_DESCRIPTION)));
	}
	
	@Deprecated
	public static DataGenerator makePreset(Preset preset, HolderLookup.Provider registryAccess, Path dataGenPath, Path dataGenOutputPath) {
		DataGenerator dataGenerator = new DataGenerator(dataGenPath, SharedConstants.getCurrentVersion(), true);
		PackGenerator packGenerator = dataGenerator.new PackGenerator(true, "preset", new PackOutput(dataGenOutputPath));
		// built up front rather than on the common pool: on Forge, classes first loaded from common pool threads can't see mod classes
		CompletableFuture<HolderLookup.Provider> lookup = CompletableFuture.completedFuture(preset.buildPatch(registryAccess));
		
		packGenerator.addProvider((output) -> {
			return DataGenUtil.createRegistryProvider(output, lookup);
		});
		packGenerator.addProvider((output) -> {
			return new PresetBlockTagsProvider(output, lookup);
		});
		packGenerator.addProvider((output) -> {
			return new PresetSurfaceLayerProvider(preset, output, lookup);
		});
		packGenerator.addProvider((output) -> {
			return new PresetBiomeTagsProvider(preset, output, CompletableFuture.completedFuture(registryAccess));
		});
		packGenerator.addProvider((output) -> {
			return PackMetadataGenerator.forFeaturePack(output, Component.translatable(UTFTranslationKeys.PRESET_METADATA_DESCRIPTION));
		});
		return dataGenerator;
	}
	
	public interface ResourcePackFactory {
		DataGenerator.PackGenerator createPack();
	}
	
	public interface DataPackFactory {
		DataGenerator.PackGenerator createPack(ResourceLocation id);
	}
}
