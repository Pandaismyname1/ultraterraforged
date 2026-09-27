package com.pandaismyname1.ultraterraforged.mixin;

import java.io.IOException;
import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import com.pandaismyname1.ultraterraforged.RTFCommon;
import com.pandaismyname1.ultraterraforged.client.ClientConfig;
import com.pandaismyname1.ultraterraforged.client.data.RTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.client.gui.createworld.PresetApplier;
import com.pandaismyname1.ultraterraforged.client.gui.createworld.TerrainState;
import com.pandaismyname1.ultraterraforged.client.gui.createworld.TerrainTab;

@Mixin(CreateWorldScreen.class)
abstract class MixinCreateWorldScreen extends Screen implements TerrainState.Holder {
	@Shadow
	@Final
	WorldCreationUiState uiState;
	@Shadow
	private boolean recreated;

	@Unique
	private TerrainState ultraterraforged$terrainState;

	protected MixinCreateWorldScreen(Component title) {
		super(title);
	}

	@Shadow
	protected abstract void onCreate();

	@Override
	public TerrainState ultraterraforged$getTerrainState() {
		if (this.ultraterraforged$terrainState == null) {
			this.ultraterraforged$terrainState = new TerrainState((CreateWorldScreen) (Object) this);
		}
		return this.ultraterraforged$terrainState;
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void ultraterraforged$selectDefaultWorldType(CallbackInfo callback) {
		// only for a new world, and only the first time the screen is set up
		if (this.ultraterraforged$terrainState == null) {
			TerrainState state = this.ultraterraforged$getTerrainState();
			if (!this.recreated && ClientConfig.load().defaultWorldType()) {
				state.selectUltraTerraForged();
			}
			// set only by the dev launch configurations, so test worlds can be flown around
			if (!this.recreated && Boolean.getBoolean("ultraterraforged.dev.creative")) {
				state.uiState().setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
				state.uiState().setAllowCheats(true);
			}
		}
	}

	@ModifyArg(
		method = "init",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;addTabs([Lnet/minecraft/client/gui/components/tabs/Tab;)Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;"
		)
	)
	private Tab[] ultraterraforged$addTerrainTab(Tab[] tabs) {
		Tab[] withTerrain = Arrays.copyOf(tabs, tabs.length + 1);
		withTerrain[tabs.length] = new TerrainTab((CreateWorldScreen) (Object) this);
		return withTerrain;
	}

	// make the world's datapacks match the chosen world type before creating it
	@Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
	private void ultraterraforged$applyPreset(CallbackInfo callback) {
		TerrainState state = this.ultraterraforged$getTerrainState();
		try {
			if (!PresetApplier.prepareForCreate((CreateWorldScreen) (Object) this, state)) {
				// the datapacks are reloading; tick continues once they're loaded
				callback.cancel();
			}
		} catch (IOException | RuntimeException e) {
			RTFCommon.LOGGER.error("Couldn't prepare the UltraTerraForged preset", e);
			state.setPendingSelection(null);
			SystemToast.addOrUpdate(this.minecraft.getToasts(), SystemToast.SystemToastIds.PACK_LOAD_FAILURE, Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_APPLY_FAILED), Component.literal(String.valueOf(e.getMessage())));
			callback.cancel();
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void ultraterraforged$createOncePresetLoaded(CallbackInfo callback) {
		TerrainState state = this.ultraterraforged$getTerrainState();
		if (PresetApplier.isReadyToCreate(state)) {
			state.setPendingSelection(null);
			// creating the world replaces this screen, which mustn't happen while it's ticking
			this.minecraft.tell(() -> {
				if (this.minecraft.screen == (Object) this) {
					this.onCreate();
				}
			});
		}
	}
}
