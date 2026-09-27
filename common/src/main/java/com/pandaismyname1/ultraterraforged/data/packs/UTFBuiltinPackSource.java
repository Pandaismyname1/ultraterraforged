package com.pandaismyname1.ultraterraforged.data.packs;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;
import net.minecraft.server.packs.repository.PackSource;
import com.pandaismyname1.ultraterraforged.UTFCommon;

public class UTFBuiltinPackSource extends BuiltInPackSource {
	private static final ResourceLocation PACKS_DIR = UTFCommon.location("datapacks");
	
	public UTFBuiltinPackSource() {
		super(PackType.SERVER_DATA, createUTFPackSource(), PACKS_DIR);
	}

	@Nullable
	@Override
	protected Pack createVanillaPack(PackResources packResources) {
		return null;
	}

	@Override
	protected Component getPackTitle(String title) {
		return Component.literal(title);
	}

	@Override
	protected Pack createBuiltinPack(String title, ResourcesSupplier resourceSupplier, Component description) {
        return Pack.readMetaAndCreate(title, description, false, resourceSupplier, PackType.SERVER_DATA, Pack.Position.TOP, PackSource.FEATURE);
	}

	private static VanillaPackResources createUTFPackSource() {
		VanillaPackResourcesBuilder builder = new VanillaPackResourcesBuilder().exposeNamespace(UTFCommon.MOD_ID);
		PackType packType = PackType.SERVER_DATA;
		String root = "/" + packType.getDirectory() + "/";
		URL uRL = UTFCommon.class.getResource(root);
		if (uRL == null) {
			UTFCommon.LOGGER.error("File {} does not exist in classpath", root);
		} else {
			try {
				URI uRI = uRL.toURI();
				String uriSchema = uRI.getScheme();
				if (!"jar".equals(uriSchema) && !"file".equals(uriSchema)) {
					UTFCommon.LOGGER.warn("Assets URL '{}' uses unexpected schema", uRI);
				}
				Path path = safeGetPath(uRI);
				builder.pushAssetPath(packType, path);
			} catch (Exception exception) {
				UTFCommon.LOGGER.error("Couldn't resolve path to assets", exception);
			}	
		}
        return builder.applyDevelopmentConfig().build();
	}

    private static Path safeGetPath(URI uRI) throws IOException {
        try {
            return Paths.get(uRI);
        } catch (FileSystemNotFoundException fileSystemNotFoundException) {
        } catch (Throwable throwable) {
        	UTFCommon.LOGGER.warn("Unable to get path for: {}", uRI, throwable);
        }
        try {
            FileSystems.newFileSystem(uRI, Collections.emptyMap());
        } catch (FileSystemAlreadyExistsException fileSystemAlreadyExistsException) {
            // empty catch block
        }
        return Paths.get(uRI);
    }
}
