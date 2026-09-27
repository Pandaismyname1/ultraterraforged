package com.pandaismyname1.ultraterraforged.server.commands;

import java.util.function.BiConsumer;

import com.mojang.brigadier.CommandDispatcher;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

public class UTFCommands {

	public static void bootstrap() {
		register(LocateTerrainCommand::register);
	}
	
	@ExpectPlatform
	public static void register(BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext> register) {
		throw new UnsupportedOperationException();
	}
}
