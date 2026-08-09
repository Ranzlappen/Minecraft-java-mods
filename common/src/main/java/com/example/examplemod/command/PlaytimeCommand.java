package com.example.examplemod.command;

import com.example.examplemod.Constants;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.config.ExampleConfig;
import com.example.examplemod.playtime.PlaytimeTracker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The {@code /playtime} command.
 *
 * <p><b>This is the "server-side mod" pattern in one class.</b> Commands are
 * dispatched entirely by the server: the client types text, the server parses
 * and answers. A vanilla client with no mods installed can run this command and
 * see the result, because the reply arrives as an ordinary chat packet.
 *
 * <p>That is what makes a mod "server-side only" — see
 * {@code docs/02-sides-client-vs-server.md#what-makes-a-mod-server-side-only}.
 *
 * <p>Registration differs per loader (Fabric has {@code CommandRegistrationCallback},
 * NeoForge has {@code RegisterCommandsEvent}), so each loader entrypoint calls
 * {@link #register(CommandDispatcher)} from its own hook. The command tree
 * itself — the part with all the logic — is shared.
 */
public final class PlaytimeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("playtime")
                        // No .requires() -> available to every player at permission 0.
                        .executes(PlaytimeCommand::self)
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(source -> ExampleConfig.get().allowQueryingOtherPlayers)
                                .executes(PlaytimeCommand::other))
                        .then(Commands.literal("reset")
                                .requires(source ->
                                        source.hasPermission(ExampleConfig.get().adminPermissionLevel))
                                .executes(PlaytimeCommand::reset))
        );
        Constants.LOG.debug("Registered /playtime");
    }

    /** {@code /playtime} — the caller's own time. */
    private static int self(CommandContext<CommandSourceStack> context) {
        final CommandSourceStack source = context.getSource();
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            // Reached when the console or a command block runs /playtime.
            source.sendFailure(Component.literal("Only a player can check their own playtime."));
            return 0;
        }
        return reply(source, player, "You have played for ");
    }

    /** {@code /playtime <player>} — someone else's time. */
    private static int other(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer target = EntityArgument.getPlayer(context, "target");
        return reply(context.getSource(), target, target.getGameProfile().getName() + " has played for ");
    }

    /** {@code /playtime reset} — clears all tracked time. Admin only. */
    private static int reset(CommandContext<CommandSourceStack> context) {
        ExampleMod.playtime().reset();
        // sendSuccess takes a Supplier so the Component is only built when the
        // message will actually be shown. The boolean is "also log to ops".
        context.getSource().sendSuccess(
                () -> Component.literal("Playtime tracking reset."), true);
        return 1;
    }

    private static int reply(CommandSourceStack source, ServerPlayer player, String prefix) {
        final String formatted = PlaytimeTracker.format(
                ExampleMod.playtime().totalFor(player.getUUID(), System.currentTimeMillis()));
        source.sendSuccess(() -> Component.literal(prefix + formatted), false);
        // Brigadier uses the return value as the command's "result" for
        // comparators and /execute store. 1 means success.
        return 1;
    }

    private PlaytimeCommand() {
    }
}
