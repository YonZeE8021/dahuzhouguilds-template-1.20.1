/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.Formatting
 *  net.minecraft.ServerCommandSource
 *  net.minecraft.CommandManager
 *  net.minecraft.ClickEvent
 *  net.minecraft.ClickEvent$class_10609
 *  net.minecraft.Text
 *  net.minecraft.HoverEvent
 *  net.minecraft.HoverEvent$class_10613
 *  net.minecraft.Style
 *  net.minecraft.ServerPlayerEntity
 *  net.minecraft.MutableText
 *  net.minecraft.server.MinecraftServer
 */
package com.dahuzhou.dahuzhouguilds.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.AllyChatBridgeManager;
import net.minecraft.util.Formatting;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.server.MinecraftServer;

public class AllyCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"ally").then(CommandManager.literal((String)"chat").then(CommandManager.argument((String)"guildName", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            UUID currentTarget;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            UUID playerId = player.getUuid();
            Guild playerGuild = GuildDataManager.getGuildByPlayer(playerId);
            if (playerGuild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            String targetGuildName = StringArgumentType.getString((CommandContext)ctx, (String)"guildName");
            Guild targetGuild = GuildDataManager.getGuildByName(targetGuildName);
            if (targetGuild == null) {
                source.sendError((Text)Text.literal((String)"No guild found with that name."));
                return 0;
            }
            if (playerGuild.getId().equals(targetGuild.getId())) {
                source.sendError((Text)Text.literal((String)"You cannot ally-chat with your own guild."));
                return 0;
            }
            String playerShortId = playerGuild.getShortenedId();
            String targetShortId = targetGuild.getShortenedId();
            if (!playerGuild.getAllies().containsKey(targetShortId) || !targetGuild.getAllies().containsKey(playerShortId)) {
                source.sendError((Text)Text.literal((String)("You are not mutually allied with " + targetGuild.getName() + ".")));
                return 0;
            }
            if (!AllyChatBridgeManager.isBridgeActive(playerShortId, targetShortId)) {
                source.sendError((Text)Text.literal((String)("No active chat bridge with " + targetGuild.getName() + ".")));
                return 0;
            }
            UUID targetGuildId = targetGuild.getId();
            if (targetGuildId.equals(currentTarget = AllyChatBridgeManager.getTargetGuild(playerId))) {
                AllyChatBridgeManager.clearAllyChatToggle(playerId);
                source.sendFeedback(() -> Text.literal((String)("Ally chat with " + targetGuild.getName() + " disabled.")), false);
            } else {
                AllyChatBridgeManager.setAllyChatToggle(playerId, targetGuildId);
                source.sendFeedback(() -> Text.literal((String)("Ally chat with " + targetGuild.getName() + " enabled.")), false);
            }
            return 1;
        })))).then(CommandManager.literal((String)"togglepvp").then(CommandManager.argument((String)"guildName", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            UUID playerId = player.getUuid();
            Guild self = GuildDataManager.getGuildByPlayer(playerId);
            if (self == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!self.getRank(playerId).equalsIgnoreCase("guild master")) {
                source.sendError((Text)Text.literal((String)"Only guild masters can toggle PvP."));
                return 0;
            }
            String targetName = StringArgumentType.getString((CommandContext)ctx, (String)"guildName");
            Guild target = GuildDataManager.getGuildByName(targetName);
            if (target == null) {
                source.sendError((Text)Text.literal((String)"Guild not found."));
                return 0;
            }
            if (self.getId().equals(target.getId())) {
                source.sendError((Text)Text.literal((String)"You cannot toggle PvP with your own guild."));
                return 0;
            }
            String selfShort = self.getShortenedId();
            String targetShort = target.getShortenedId();
            if (!self.getAllies().containsKey(targetShort) || !target.getAllies().containsKey(selfShort)) {
                source.sendError((Text)Text.literal((String)"You are not allied with that guild."));
                return 0;
            }
            Guild.AllyInfo selfAlly = self.getAllies().getOrDefault(targetShort, new Guild.AllyInfo("UNKNOWN", false));
            Guild.AllyInfo targetAlly = target.getAllies().getOrDefault(selfShort, new Guild.AllyInfo("UNKNOWN", false));
            boolean selfPvpDisabled = Boolean.TRUE.equals(selfAlly.pvpDisabled);
            boolean targetPvpDisabled = Boolean.TRUE.equals(targetAlly.pvpDisabled);
            if (selfPvpDisabled && targetPvpDisabled) {
                MutableText confirmButton = Text.literal((String)"[YES]").setStyle(Style.EMPTY.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ally pvpdisableconfirm " + String.valueOf(target.getId()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to enable PvP"))));
                MutableText cancelButton = Text.literal((String)"[NO]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild cancel")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to cancel"))));
                player.sendMessage((Text)Text.literal((String)"PvP with ").append((Text)Text.literal((String)target.getName()).formatted(Formatting.YELLOW)).append((Text)Text.literal((String)" is currently DISABLED. Do you want to re-enable it? ")).append((Text)confirmButton).append((Text)Text.literal((String)" ")).append((Text)cancelButton), false);
                return 1;
            }
            ServerPlayerEntity targetMaster = server.getPlayerManager().getPlayer(target.getOwnerId());
            if (targetMaster == null) {
                source.sendError((Text)Text.literal((String)("The guild master of " + target.getName() + " is not online.")));
                return 0;
            }
            GuildDataManager.addPvpEnableRequest(target.getOwnerId(), self.getId());
            player.sendMessage((Text)Text.literal((String)("PvP request sent to " + target.getName())).formatted(Formatting.GREEN), false);
            MutableText accept = Text.literal((String)"[ACCEPT]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ally pvpenableaccept " + String.valueOf(self.getId()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to accept the PvP request"))));
            MutableText deny = Text.literal((String)"[DENY]").setStyle(Style.EMPTY.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ally pvpenabledeny " + String.valueOf(self.getId()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to deny the PvP request"))));
            targetMaster.sendMessage((Text)Text.literal((String)"Your guild has received a request to DISABLE PvP with ").append((Text)Text.literal((String)self.getName()).formatted(Formatting.YELLOW)).append((Text)Text.literal((String)". Click [ACCEPT] to disable PvP or [DENY] to leave it enabled.")).append((Text)accept).append((Text)Text.literal((String)" ")).append((Text)deny), false);
            return 1;
        })))).then(CommandManager.literal((String)"pvpenableaccept").then(CommandManager.argument((String)"guildId", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            ServerPlayerEntity player = ((ServerCommandSource)ctx.getSource()).getPlayer();
            MinecraftServer server = ((ServerCommandSource)ctx.getSource()).getServer();
            UUID receiverId = player.getUuid();
            UUID senderGuildId = UUID.fromString(StringArgumentType.getString((CommandContext)ctx, (String)"guildId"));
            Guild senderGuild = GuildDataManager.getGuildById(senderGuildId);
            Guild receiverGuild = GuildDataManager.getGuildByPlayer(receiverId);
            if (senderGuild == null || receiverGuild == null) {
                ((ServerCommandSource)ctx.getSource()).sendError((Text)Text.literal((String)"One of the guilds could not be found."));
                return 0;
            }
            String senderShort = senderGuild.getShortenedId();
            String receiverShort = receiverGuild.getShortenedId();
            senderGuild.setPvpToggle(receiverShort, true);
            receiverGuild.setPvpToggle(senderShort, true);
            GuildDataManager.saveGuild(server, senderGuild);
            GuildDataManager.saveGuild(server, receiverGuild);
            GuildDataManager.clearPvpRequest(receiverId);
            ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal((String)"PvP DISABLED between guilds.").formatted(Formatting.GREEN), false);
            return 1;
        })))).then(CommandManager.literal((String)"pvpenabledeny").then(CommandManager.argument((String)"guildId", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            ServerPlayerEntity player = ((ServerCommandSource)ctx.getSource()).getPlayer();
            UUID receiverId = player.getUuid();
            GuildDataManager.clearPvpRequest(receiverId);
            ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal((String)"PvP DISABLED request denied.").formatted(Formatting.RED), false);
            return 1;
        })))).then(CommandManager.literal((String)"pvpdisableconfirm").then(CommandManager.argument((String)"guildId", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            ServerPlayerEntity player = ((ServerCommandSource)ctx.getSource()).getPlayer();
            MinecraftServer server = ((ServerCommandSource)ctx.getSource()).getServer();
            UUID requesterId = player.getUuid();
            UUID targetGuildId = UUID.fromString(StringArgumentType.getString((CommandContext)ctx, (String)"guildId"));
            Guild self = GuildDataManager.getGuildByPlayer(requesterId);
            Guild target = GuildDataManager.getGuildById(targetGuildId);
            if (self == null || target == null) {
                ((ServerCommandSource)ctx.getSource()).sendError((Text)Text.literal((String)"Guild not found."));
                return 0;
            }
            String selfShort = self.getShortenedId();
            String targetShort = target.getShortenedId();
            self.setPvpToggle(targetShort, false);
            target.setPvpToggle(selfShort, false);
            GuildDataManager.saveGuild(server, self);
            GuildDataManager.saveGuild(server, target);
            ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal((String)"PvP has been RE-ENABLED for both guilds.").formatted(Formatting.RED), false);
            return 1;
        }))));
    }
}

