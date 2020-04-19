package us.sodiumlabs.mcapi.fabric.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import us.sodiumlabs.mcapi.fabric.service.CredCacheService;
import us.sodiumlabs.mcapi.fabric.service.CredInformation;

import java.util.Objects;
import java.util.Optional;


public class APICommands {
    public static void register(final CommandManager commandManager, final CredCacheService credCacheService) {
        commandManager.getDispatcher().register(CommandManager.literal("api")
            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2))
            .then(registerNewUser(credCacheService))
            .then(listUsers(credCacheService))
            .then(deleteUser(credCacheService))
        );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> deleteUser(CredCacheService credCacheService) {
        return CommandManager.literal("delete").then(CommandManager.argument("id", StringArgumentType.word()).executes(context -> {
            final String id = "$" + StringArgumentType.getString(context, "id");
            final ServerCommandSource serverCommandSource = context.getSource();
            final ServerPlayerEntity player = serverCommandSource.getPlayer();
            final Optional<CredInformation> information = credCacheService.getCredInformation(id);

            // only execute if the api user belongs to the calling user
            final boolean valid = information
                .map(i -> Objects.equals(i.ownerUuid, player.getUuid()))
                .orElse(false);

            if(!valid) {
                serverCommandSource.sendError(new LiteralText("No API user with id [" + id + "] exists! " +
                    "Did you accidentally enter the user's name?"));
                return Command.SINGLE_SUCCESS;
            }

            information.ifPresent(i -> {
                credCacheService.deleteCredInformation(id);

                final LiteralText out = new LiteralText("Deleted API user named " + i.name);
                serverCommandSource.sendFeedback(out, false);
            });

            return Command.SINGLE_SUCCESS;
        }));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> listUsers(CredCacheService credCacheService) {
        return CommandManager.literal("list").executes(context -> {
            final ServerCommandSource serverCommandSource = context.getSource();
            credCacheService.listUsers(serverCommandSource.getPlayer().getUuid()).forEach(c -> {
                final LiteralText out = new LiteralText(c.credId + ": " + c.name);
                serverCommandSource.sendFeedback(out, false);
            });

            return Command.SINGLE_SUCCESS;
        });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> registerNewUser(final CredCacheService credCacheService) {
        return CommandManager.literal("register").then(CommandManager.argument("name", StringArgumentType.word()).executes(context -> {
            final ServerCommandSource serverCommandSource = context.getSource();
            final CredInformation information = credCacheService.createCredInformation(
                StringArgumentType.getString(context, "name"),
                serverCommandSource.getPlayer().getUuid());

            final LiteralText out = new LiteralText("Registered API user " + information.credId + " with secret " + information.secret);
            serverCommandSource.sendFeedback(out, false);

            return Command.SINGLE_SUCCESS;
        }));
    }
}
