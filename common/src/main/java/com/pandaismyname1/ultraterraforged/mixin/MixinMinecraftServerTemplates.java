package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.server.MinecraftServer;
import com.pandaismyname1.ultraterraforged.server.UTFMinecraftServer;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.template.template.FeatureTemplateManager;

// Made on first use, and it follows /reload on its own (see FeatureTemplateManager), so nothing here depends on the
// server's constructor or on the names of its lambdas, which differ between loaders and versions.
@Implements(@Interface(iface = UTFMinecraftServer.class, prefix = "ultraterraforged$UTFMinecraftServer$"))
@Mixin(MinecraftServer.class)
public class MixinMinecraftServerTemplates {
	@Unique
	private volatile FeatureTemplateManager templateManager;

	public FeatureTemplateManager ultraterraforged$UTFMinecraftServer$getFeatureTemplateManager() {
		FeatureTemplateManager templateManager = this.templateManager;
		if (templateManager == null) {
			synchronized (this) {
				templateManager = this.templateManager;
				if (templateManager == null) {
					this.templateManager = templateManager = new FeatureTemplateManager((MinecraftServer) (Object) this);
				}
			}
		}
		return templateManager;
	}
}
