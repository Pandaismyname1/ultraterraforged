package com.pandaismyname1.ultraterraforged.server.commands;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import com.pandaismyname1.ultraterraforged.client.data.RTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

// A plain word argument with server-side suggestions rather than a custom ArgumentType,
// so the command tree stays vanilla-compatible and clients don't need the mod installed
public class TerrainArgument {
    public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(input -> Component.translatable(RTFTranslationKeys.TERRAIN_ARGUMENT_INVALID, input));
    // volcano_pipe, the lava lake in a crater, can be located too
    private static final List<Terrain> BLACKLIST = ImmutableList.of(TerrainType.NONE);

    public static RequiredArgumentBuilder<CommandSourceStack, String> terrain(String name) {
    	return Commands.argument(name, StringArgumentType.word()).suggests((ctx, builder) -> {
    		return SharedSuggestionProvider.suggest(getTerrainTypeNames(), builder);
    	});
    }

    public static Terrain getTerrain(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
    	String terrainName = StringArgumentType.getString(ctx, name);
    	Terrain terrain = TerrainType.get(terrainName);
    	if (terrain == null || BLACKLIST.contains(terrain)) {
    		throw ERROR_INVALID_VALUE.create(terrainName);
    	}
    	return terrain;
    }

    private static Stream<String> getTerrainTypeNames() {
    	return TerrainType.REGISTRY.stream().filter((type) -> !BLACKLIST.contains(type)).map(Terrain::getName);
    }
}
