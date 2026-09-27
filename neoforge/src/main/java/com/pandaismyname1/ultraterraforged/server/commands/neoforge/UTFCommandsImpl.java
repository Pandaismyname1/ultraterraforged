package com.pandaismyname1.ultraterraforged.server.commands.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class UTFCommandsImpl {
	private static final List<BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext>> COMMANDS = new ArrayList<>();
	
	public static void register(BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext> register) {
		COMMANDS.add(register);
	}
	
	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		COMMANDS.forEach((consumer) -> consumer.accept(event.getDispatcher(), event.getBuildContext()));
	}
}
