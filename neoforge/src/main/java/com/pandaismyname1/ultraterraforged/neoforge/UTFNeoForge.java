package com.pandaismyname1.ultraterraforged.neoforge;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.client.data.UTFLanguageProvider;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.platform.neoforge.RegistryUtilImpl;
import com.pandaismyname1.ultraterraforged.server.UTFMinecraftServer;

@Mod(UTFCommon.MOD_ID)
public class UTFNeoForge {

    public UTFNeoForge(IEventBus modBus) {
    	UTFCommon.bootstrap();

    	modBus.addListener(UTFNeoForge::gatherData);
    	NeoForge.EVENT_BUS.addListener(UTFNeoForge::addReloadListeners);

    	RegistryUtilImpl.register(modBus);
    }

    private static void gatherData(GatherDataEvent event) {
    	boolean includeClient = event.includeClient();
    	DataGenerator generator = event.getGenerator();
    	PackOutput output = generator.getPackOutput();

    	generator.addProvider(includeClient, new UTFLanguageProvider.EnglishUS(output));
    	generator.addProvider(includeClient, PackMetadataGenerator.forFeaturePack(output, Component.translatable(UTFTranslationKeys.METADATA_DESCRIPTION)));
    }

    // /reload: templates are read again from the new resources. The first load needs nothing: the server reads them
    // when it first asks for its template manager.
    private static void addReloadListeners(AddReloadListenerEvent event) {
    	event.addListener((ResourceManagerReloadListener) (resourceManager) -> {
    		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    		if (server instanceof UTFMinecraftServer utfServer) {
    			utfServer.getFeatureTemplateManager().onReload(resourceManager);
    		}
    	});
    }
}
