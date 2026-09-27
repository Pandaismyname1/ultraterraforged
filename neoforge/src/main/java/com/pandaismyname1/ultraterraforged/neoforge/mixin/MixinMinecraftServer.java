package com.pandaismyname1.ultraterraforged.neoforge.mixin;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import com.pandaismyname1.ultraterraforged.server.UTFMinecraftServer;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.template.FeatureTemplateManager;

// Made on first use rather than in the constructor, and reloaded by a reload listener (see UTFNeoForge), so nothing here
// depends on NeoForge's patched constructor or on javac's numbering of MinecraftServer's lambdas.
@Implements(@Interface(iface = UTFMinecraftServer.class, prefix = "ultraterraforged$UTFMinecraftServer$"))
@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
	@Unique
	private volatile FeatureTemplateManager templateManager;

	public FeatureTemplateManager ultraterraforged$UTFMinecraftServer$getFeatureTemplateManager() {
		FeatureTemplateManager templateManager = this.templateManager;
		if (templateManager == null) {
			synchronized (this) {
				templateManager = this.templateManager;
				if (templateManager == null) {
					this.templateManager = templateManager = new FeatureTemplateManager((MinecraftServer) (Object) this, this.getResourceManager());
				}
			}
		}
		return templateManager;
	}

	@Shadow
	public ResourceManager getResourceManager() {
		throw new UnsupportedOperationException();
	}
}
