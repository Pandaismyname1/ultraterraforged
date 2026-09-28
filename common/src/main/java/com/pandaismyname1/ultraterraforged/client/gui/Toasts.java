package com.pandaismyname1.ultraterraforged.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.Component;
import com.pandaismyname1.ultraterraforged.client.data.UTFTranslationKeys;

public final class Toasts {

	public static void notify(String message, Component description, SystemToastId id) {
		Minecraft mc = Minecraft.getInstance();
		SystemToast.add(mc.getToastManager(), id, Component.translatable(message), description);
	}
	
	public static void tryOrToast(String errorMessage, ThrowingRunnable r) {
		try {
			r.run();
		} catch(Exception e) {
			e.printStackTrace();
			String message = e.getMessage();
			Component messageComponent;
			if(message != null) {
				messageComponent = Component.literal(message);
			} else {
				messageComponent = Component.translatable(UTFTranslationKeys.NO_ERROR_MESSAGE);
			}
			
			notify(errorMessage, messageComponent, SystemToastId.PACK_LOAD_FAILURE);
		}
	}
	
	public interface ThrowingRunnable {
		void run() throws Exception;
	}
}
