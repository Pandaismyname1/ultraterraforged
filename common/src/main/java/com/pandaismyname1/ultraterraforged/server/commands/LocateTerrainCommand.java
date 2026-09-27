package com.pandaismyname1.ultraterraforged.server.commands;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import com.pandaismyname1.ultraterraforged.client.data.RTFTranslationKeys;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.RTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.TerrainLocator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;

public class LocateTerrainCommand {
    private static final DynamicCommandExceptionType ERROR_TERRAIN_NOT_FOUND = new DynamicCommandExceptionType(terrain -> Component.translatable(RTFTranslationKeys.TERRAIN_NOT_FOUND, terrain));

    // how far to search, in blocks
    private static final int MAX_DISTANCE = 24000;
    private static final long TIMEOUT_MILLIS = 30_000L;

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext) {
    	commandDispatcher.register(
    		Commands.literal("rtf").requires((stack) -> stack.hasPermission(2)).then(
    			Commands.literal("locate").then(
    				TerrainArgument.terrain("terrain").executes((ctx) -> {
	    				CommandSourceStack stack = ctx.getSource();
	    				Terrain terrain = TerrainArgument.getTerrain(ctx, "terrain");
	    				String terrainName = terrain.getName();
	    				BlockPos origin = BlockPos.containing(stack.getPosition());
	    				@Nullable
	    				BlockPos result = locate(stack, terrain, origin);
	    				if(result != null) {
	    			        int distance = Mth.floor(dist(origin.getX(), origin.getZ(), result.getX(), result.getZ()));
		    			    stack.sendSuccess(() -> Component.translatable(RTFTranslationKeys.TERRAIN_FOUND, terrainName, createTeleportMessage(result), distance), false);
		    			    return Command.SINGLE_SUCCESS;
	    				}
	    	            throw ERROR_TERRAIN_NOT_FOUND.create(terrainName);
    				})
    			)
	    	)
    	);
    }

    private static Component createTeleportMessage(BlockPos pos) {
        return ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())).withStyle(s -> s
        	.withColor(ChatFormatting.GREEN)
        	.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))
        	.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
        );
    }

    @Nullable
    private static BlockPos locate(CommandSourceStack commandSourceStack, Terrain target, BlockPos origin) {
    	ServerLevel level = commandSourceStack.getLevel();

    	@Nullable
    	GeneratorContext generatorContext;
    	if((Object) level.getChunkSource().randomState() instanceof RTFRandomState rtfRandomState && (generatorContext = rtfRandomState.generatorContext()) != null) {
    		@Nullable
    		TerrainLocator.Found found = TerrainLocator.locate(generatorContext.lookup, target, origin.getX(), origin.getZ(), MAX_DISTANCE, TIMEOUT_MILLIS);
    		if (found != null) {
    			// the height of the ground there, not of the player: they'd land inside a hill or high in the air
    			return new BlockPos(found.x(), TerrainLocator.surface(generatorContext.lookup, generatorContext.levels, found.x(), found.z()), found.z());
    		}
    	}
    	return null;
    }

    private static float dist(int x0, int z0, int x1, int z1) {
        int deltaX = x1 - x0;
        int deltaZ = z1 - z0;
        return Mth.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
}

