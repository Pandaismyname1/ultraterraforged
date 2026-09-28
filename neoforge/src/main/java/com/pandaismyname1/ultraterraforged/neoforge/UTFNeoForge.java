package com.pandaismyname1.ultraterraforged.neoforge;

import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.client.data.UTFLanguageProvider;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.platform.neoforge.RegistryUtilImpl;

@Mod(UTFCommon.MOD_ID)
public class UTFNeoForge {

    public UTFNeoForge(IEventBus modBus) {
    	UTFCommon.bootstrap();

    	modBus.addListener(UTFNeoForge::gatherData);

    	RegistryUtilImpl.register(modBus);
    }

    // translations and pack metadata are client resources
    private static void gatherData(GatherDataEvent.Client event) {
    	event.createProvider(UTFLanguageProvider.EnglishUS::new);
    	event.createProvider((output) -> PackMetadataGenerator.forFeaturePack(output, Component.translatable(UTFTranslationKeys.METADATA_DESCRIPTION)));
    }
}
