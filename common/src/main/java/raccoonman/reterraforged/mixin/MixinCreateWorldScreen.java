package raccoonman.reterraforged.mixin;

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
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.ClientConfig;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.createworld.PresetApplier;
import raccoonman.reterraforged.client.gui.createworld.TerrainState;
import raccoonman.reterraforged.client.gui.createworld.TerrainTab;

@Mixin(CreateWorldScreen.class)
abstract class MixinCreateWorldScreen extends Screen implements TerrainState.Holder {
	@Shadow
	@Final
	WorldCreationUiState uiState;
	@Shadow
	private boolean recreated;

	@Unique
	private TerrainState reterraforged$terrainState;

	protected MixinCreateWorldScreen(Component title) {
		super(title);
	}

	@Shadow
	protected abstract void onCreate();

	@Override
	public TerrainState reterraforged$getTerrainState() {
		if (this.reterraforged$terrainState == null) {
			this.reterraforged$terrainState = new TerrainState((CreateWorldScreen) (Object) this);
		}
		return this.reterraforged$terrainState;
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void reterraforged$selectDefaultWorldType(CallbackInfo callback) {
		// only for a new world, and only the first time the screen is set up
		if (this.reterraforged$terrainState == null) {
			TerrainState state = this.reterraforged$getTerrainState();
			if (!this.recreated && ClientConfig.load().defaultWorldType()) {
				state.selectReTerraForged();
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
	private Tab[] reterraforged$addTerrainTab(Tab[] tabs) {
		Tab[] withTerrain = Arrays.copyOf(tabs, tabs.length + 1);
		withTerrain[tabs.length] = new TerrainTab((CreateWorldScreen) (Object) this);
		return withTerrain;
	}

	// make the world's datapacks match the chosen world type before creating it
	@Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
	private void reterraforged$applyPreset(CallbackInfo callback) {
		TerrainState state = this.reterraforged$getTerrainState();
		try {
			if (!PresetApplier.prepareForCreate((CreateWorldScreen) (Object) this, state)) {
				// the datapacks are reloading; tick continues once they're loaded
				callback.cancel();
			}
		} catch (IOException | RuntimeException e) {
			RTFCommon.LOGGER.error("Couldn't prepare the ReTerraForged preset", e);
			state.setPendingSelection(null);
			SystemToast.addOrUpdate(this.minecraft.getToasts(), SystemToast.SystemToastIds.PACK_LOAD_FAILURE, Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_APPLY_FAILED), Component.literal(String.valueOf(e.getMessage())));
			callback.cancel();
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void reterraforged$createOncePresetLoaded(CallbackInfo callback) {
		TerrainState state = this.reterraforged$getTerrainState();
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
