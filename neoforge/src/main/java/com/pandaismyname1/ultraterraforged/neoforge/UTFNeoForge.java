package com.pandaismyname1.ultraterraforged.neoforge;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.client.data.UTFLanguageProvider;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.platform.neoforge.RegistryUtilImpl;

@Mod(UTFCommon.MOD_ID)
public class UTFForge {

    public UTFForge() {
    	UTFCommon.bootstrap();

    	IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

    	if (FMLEnvironment.dist == Dist.CLIENT) {
    		modBus.addListener(UTFForgeClient::registerPresetEditors);
    	}
    	modBus.addListener(UTFForge::gatherData);

    	RegistryUtilImpl.register(modBus);
    }
    
    private static void gatherData(GatherDataEvent event) {
    	boolean includeClient = event.includeClient();
    	DataGenerator generator = event.getGenerator();
    	PackOutput output = generator.getPackOutput();

    	generator.addProvider(includeClient, new UTFLanguageProvider.EnglishUS(output));
    	generator.addProvider(includeClient, PackMetadataGenerator.forFeaturePack(output, Component.translatable(UTFTranslationKeys.METADATA_DESCRIPTION)));
    }
}