package com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.template;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;

public class FeatureTemplateManager {
	private final MinecraftServer server;
	private volatile ResourceManager resourceManager;
	private final Map<Identifier, FeatureTemplate> cache;
	
	public FeatureTemplateManager(MinecraftServer server) {
		this.server = server;
		this.resourceManager = server.getResourceManager();
		this.cache = new ConcurrentHashMap<>();
	}
	
	public FeatureTemplate load(Identifier location) {
		// /reload gives the server a new resource manager: the templates are read again from it
		ResourceManager current = this.server.getResourceManager();
		if (current != this.resourceManager) {
			synchronized (this) {
				if (current != this.resourceManager) {
					this.cache.clear();
					this.resourceManager = current;
				}
			}
		}
		return this.cache.computeIfAbsent(location, this::read);
	}
	
	private FeatureTemplate read(Identifier location) {
		return this.resourceManager.getResource(location).flatMap((resource) -> {
			try(InputStream stream = resource.open()) {
				return FeatureTemplate.load(this.server.registryAccess().lookupOrThrow(Registries.BLOCK).filterFeatures(this.server.getWorldData().enabledFeatures()), stream);
			} catch (IOException e) {
				e.printStackTrace();
				return Optional.empty();
			}
		}).orElse(new FeatureTemplate(ImmutableList.of()));
	}
}
