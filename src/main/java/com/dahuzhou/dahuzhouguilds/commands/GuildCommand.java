/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.Formatting
 *  net.minecraft.PlayerEntity
 *  net.minecraft.PlayerInventory
 *  net.minecraft.ItemStack
 *  net.minecraft.Items
 *  net.minecraft.ServerCommandSource
 *  net.minecraft.CommandManager
 *  net.minecraft.Vec3d
 *  net.minecraft.ClickEvent
 *  net.minecraft.ClickEvent$class_10609
 *  net.minecraft.Text
 *  net.minecraft.HoverEvent
 *  net.minecraft.HoverEvent$class_10613
 *  net.minecraft.Style
 *  net.minecraft.Team
 *  net.minecraft.PositionFlag
 *  net.minecraft.Identifier
 *  net.minecraft.ServerScoreboard
 *  net.minecraft.ServerWorld
 *  net.minecraft.ServerPlayerEntity
 *  net.minecraft.NamedScreenHandlerFactory
 *  net.minecraft.MutableText
 *  net.minecraft.RegistryKey
 *  net.minecraft.RegistryKeys
 *  net.minecraft.server.MinecraftServer
 */
package com.dahuzhou.dahuzhouguilds.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.dahuzhou.dahuzhouguilds.GuildsConfig;
import com.dahuzhou.dahuzhouguilds.data.GuildBankManager;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.AllyChatBridgeManager;
import com.dahuzhou.dahuzhouguilds.util.DelayedTeleportScheduler;
import com.dahuzhou.dahuzhouguilds.util.GuildBankInventory;
import com.dahuzhou.dahuzhouguilds.util.GuildBankScreenHandler;
import com.dahuzhou.dahuzhouguilds.util.GuildColorUtil;
import com.dahuzhou.dahuzhouguilds.util.GuildHungerBoundPrefixHelper;
import com.dahuzhou.dahuzhouguilds.util.GuildPermissionsMenu;
import com.dahuzhou.dahuzhouguilds.util.GuildPrefixHelper;
import com.dahuzhou.dahuzhouguilds.util.GuildTeamUtil;
import net.minecraft.util.Formatting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.MutableText;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;

public class GuildCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"guild").then(CommandManager.literal((String)"create").then(CommandManager.argument((String)"name", (ArgumentType)StringArgumentType.word()).then(CommandManager.argument((String)"color", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            Formatting color;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            String name = StringArgumentType.getString((CommandContext)ctx, (String)"name");
            if (name.length() > 13) {
                source.sendError((Text)Text.literal((String)"The guild name must be 13 characters or fewer."));
                return 0;
            }
            String colorName = StringArgumentType.getString((CommandContext)ctx, (String)"color").toUpperCase();
            try {
                color = Formatting.valueOf((String)colorName);
                if (!color.isColor()) {
                    source.sendError((Text)Text.literal((String)"That is not a valid color."));
                    return 0;
                }
            }
            catch (IllegalArgumentException e) {
                source.sendError((Text)Text.literal((String)"Invalid color. Use names like red, blue, green."));
                return 0;
            }
            UUID playerId = player.getUuid();
            String playerName = player.getName().getString();
            if (GuildDataManager.getGuildByPlayer(playerId) != null) {
                source.sendError((Text)Text.literal((String)"You are already in a guild."));
                return 0;
            }
            if (GuildDataManager.isGuildNameTaken(name)) {
                source.sendError((Text)Text.literal((String)"A guild with that name already exists."));
                return 0;
            }
            Guild guild = new Guild(UUID.randomUUID(), name, color.getName(), Instant.now(), playerId, playerName, false);
            guild.addMember(playerId, playerName, "Guild Master");
            GuildDataManager.registerGuild(guild);
            GuildDataManager.saveGuild(source.getServer(), guild);
            String guildShortId = guild.getShortenedId();
            GuildBankManager.createBank(source.getServer(), guildShortId);
            System.out.println("[GuildBank] Created new bank for guild: " + guildShortId);
            String teamName = "guild_" + guild.getId().toString().substring(0, 8);
            ServerScoreboard scoreboard = source.getServer().getScoreboard();
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.addTeam(teamName);
                System.out.println("[GuildsRemastered] Created team: " + teamName);
            } else {
                System.out.println("[GuildsRemastered] Reused existing team: " + teamName);
            }
            source.getServer().getCommandManager().executeWithPrefix(source.getServer().getCommandSource(), "team join " + teamName + " " + player.getName().getString());
            String modifyTeamColorCommand = "/team modify " + teamName + " color " + color.getName().toLowerCase();
            source.getServer().getCommandManager().executeWithPrefix(source.getServer().getCommandSource(), modifyTeamColorCommand);
            System.out.println("[GuildsRemastered] Set team color for " + teamName + " to " + color.getName().toLowerCase());
            if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                GuildPrefixHelper.applyGuildPrefix(player);
            }
            if (GuildsConfig.enableHungerBoundIntegration) {
                GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(player);
            }
            source.sendFeedback(() -> Text.literal((String)"Guild ").append((Text)Text.literal((String)name).formatted(color)).append(" created successfully!"), false);
            return 1;
        }))))).then(((LiteralArgumentBuilder)CommandManager.literal((String)"info").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            GuildCommand.displayGuildInfo(source.getServer(), source, guild);
            return 1;
        })).then(CommandManager.argument((String)"name", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            ServerPlayerEntity player;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            String name = StringArgumentType.getString((CommandContext)ctx, (String)"name");
            Guild guild = GuildDataManager.getGuildByName(name);
            if (guild == null && (player = source.getServer().getPlayerManager().getPlayer(name)) != null) {
                guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            }
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"No guild found by that name or player."));
                return 0;
            }
            GuildCommand.displayGuildInfo(source.getServer(), source, guild);
            return 1;
        })))).then(CommandManager.literal((String)"disband").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!guild.getOwnerId().equals(player.getUuid())) {
                source.sendError((Text)Text.literal((String)"Only the guild owner can disband the guild."));
                return 0;
            }
            MutableText yesButton = Text.literal((String)"[YES]").setStyle(Style.EMPTY.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild disbandconfirm")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to disband your guild"))));
            MutableText noButton = Text.literal((String)"[NO]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild cancel")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to cancel"))));
            player.sendMessage((Text)Text.literal((String)"Are you sure you want to disband your guild? ").formatted(Formatting.GOLD).append((Text)yesButton).append((Text)Text.literal((String)" ")).append((Text)noButton), false);
            return 1;
        }))).then(CommandManager.literal((String)"disbandconfirm").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!guild.getOwnerId().equals(playerId)) {
                source.sendError((Text)Text.literal((String)"Only the guild owner can disband the guild."));
                return 0;
            }
            String teamName = "guild_" + guild.getId().toString().substring(0, 8);
            ServerScoreboard scoreboard = server.getScoreboard();
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                String modifyPrefixCommand = "/team modify " + teamName + " prefix \"\"";
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), modifyPrefixCommand);
                System.out.println("[GuildsRemasteredMod] Removed prefix for team: " + teamName);
                team.getPlayerList().clear();
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), "team remove " + teamName);
                System.out.println("[GuildsRemasteredMod] Removed team: " + teamName);
            } else {
                System.out.println("[GuildsRemasteredMod] Team not found for removal: " + teamName);
            }
            for (UUID memberId : guild.getMembers().keySet()) {
                ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                if (member == null) continue;
                if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                    GuildPrefixHelper.clearNickname(member);
                }
                if (GuildsConfig.enableHungerBoundIntegration) {
                    GuildHungerBoundPrefixHelper.clearNickname(member);
                }
                GuildTeamUtil.clearTeamForPlayer(member, server);
                GuildTeamUtil.forceUpdateForPlayer(member);
            }
            UUID disbandedId = guild.getId();
            String disbandedShortId = "guild_" + disbandedId.toString().substring(0, 8);
            String disbandedName = guild.getName();
            for (String allyShortId : guild.getAllies().keySet()) {
                Map<String, Guild.AllyInfo> theirAllies;
                Guild allyGuild = GuildDataManager.getGuildByShortId(allyShortId);
                if (allyGuild == null || !(theirAllies = allyGuild.getAllies()).containsKey(disbandedShortId)) continue;
                theirAllies.remove(disbandedShortId);
                GuildDataManager.saveGuild(server, allyGuild);
                for (UUID memberId : allyGuild.getMembers().keySet()) {
                    ServerPlayerEntity allyPlayer = server.getPlayerManager().getPlayer(memberId);
                    if (allyPlayer == null) continue;
                    allyPlayer.sendMessage((Text)Text.literal((String)"Your ally ").append((Text)Text.literal((String)disbandedName).formatted(Formatting.YELLOW)).append((Text)Text.literal((String)" has disbanded and is no longer an ally.")));
                }
            }
            GuildDataManager.removeAllInvitesToGuild(guild.getId());
            GuildDataManager.deleteGuild(server, guild.getId());
            String guildShortId = guild.getShortenedId();
            GuildBankManager.deleteBank(server, guildShortId);
            source.sendFeedback(() -> Text.literal((String)"Guild ").append((Text)Text.literal((String)guild.getName()).formatted(Formatting.RED)).append(" has been disbanded."), false);
            return 1;
        }))).then(CommandManager.literal((String)"invite").then(CommandManager.argument((String)"player", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> {
            for (ServerPlayerEntity p : ((ServerCommandSource)ctx.getSource()).getServer().getPlayerManager().getPlayerList()) {
                builder.suggest(p.getName().getString());
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity inviter = source.getPlayer();
            MinecraftServer server = source.getServer();
            String targetName = StringArgumentType.getString((CommandContext)ctx, (String)"player");
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
            System.out.println("[GuildsRemastered] Attempting to invite " + targetName + " by player " + inviter.getName().getString());
            if (target == null) {
                source.sendError((Text)Text.literal((String)"Player not found."));
                System.out.println("[GuildsRemastered] Player not found: " + targetName);
                return 0;
            }
            UUID inviterId = inviter.getUuid();
            UUID targetId = target.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(inviterId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                System.out.println("[GuildsRemastered] Inviter not in a guild: " + inviter.getName().getString());
                return 0;
            }
            String inviterRank = guild.getRank(inviterId).toLowerCase();
            System.out.println("[GuildsRemastered] Inviter rank: " + inviterRank);
            if (!guild.hasRankPermission(inviterId, "canInvite")) {
                source.sendError((Text)Text.literal((String)"You do not have permission to invite players to the guild."));
                return 0;
            }
            if (GuildDataManager.getGuildByPlayer(targetId) != null) {
                source.sendError((Text)Text.literal((String)"That player is already in a guild."));
                System.out.println("[GuildsRemastered] Target player is already in a guild: " + targetName);
                return 0;
            }
            GuildDataManager.addInvite(targetId, guild.getId());
            inviter.sendMessage((Text)Text.literal((String)("Invitation sent to " + target.getName().getString())).formatted(Formatting.GREEN));
            MutableText acceptButton = Text.literal((String)"[ACCEPT]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild accept")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to join the guild"))));
            target.sendMessage((Text)Text.literal((String)"You have been invited to join the guild ").append((Text)Text.literal((String)guild.getName()).formatted(Formatting.YELLOW)).append((Text)Text.literal((String)". ")).append((Text)acceptButton), false);
            System.out.println("[GuildsRemastered] Invitation sent to " + targetName + " for guild " + guild.getName());
            return 1;
        })))).then(CommandManager.literal((String)"accept").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            UUID guildId = GuildDataManager.getInvite(playerId);
            System.out.println("[GuildsRemastered] Player " + player.getName().getString() + " is attempting to accept an invite.");
            if (guildId == null) {
                source.sendError((Text)Text.literal((String)"You do not have any pending invitations."));
                System.out.println("[GuildsRemastered] No pending invites for " + player.getName().getString());
                return 0;
            }
            if (GuildDataManager.getGuildByPlayer(playerId) != null) {
                source.sendError((Text)Text.literal((String)"You are already in a guild."));
                System.out.println("[GuildsRemastered] Player " + player.getName().getString() + " is already in a guild.");
                return 0;
            }
            Guild guild = GuildDataManager.getGuildById(guildId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"The guild you were invited to no longer exists."));
                System.out.println("[GuildsRemastered] Guild no longer exists: " + String.valueOf(guildId));
                return 0;
            }
            guild.addMember(playerId, player.getName().getString(), "Initiate");
            GuildDataManager.removeInvite(playerId);
            GuildDataManager.saveGuild(source.getServer(), guild);
            String teamName = "guild_" + guild.getId().toString().substring(0, 8);
            source.getServer().getCommandManager().executeWithPrefix(source.getServer().getCommandSource(), "team join " + teamName + " " + player.getName().getString());
            if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                GuildPrefixHelper.applyGuildPrefix(player);
            }
            if (GuildsConfig.enableHungerBoundIntegration) {
                GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(player);
            }
            Objects.requireNonNull(player.getServer()).getPlayerManager().broadcast((Text)Text.literal((String)(player.getName().getString() + " has joined the guild " + guild.getName() + "!")), false);
            source.sendFeedback(() -> Text.literal((String)"You have joined the guild ").append((Text)Text.literal((String)guild.getName()).formatted(Formatting.YELLOW)).append("!"), false);
            if (guild.getMotd() != null && !guild.getMotd().isEmpty()) {
                player.sendMessage((Text)Text.literal((String)"Guild MOTD: ").append((Text)Text.literal((String)guild.getMotd()).styled(style -> style.withColor(Formatting.YELLOW).withItalic(Boolean.valueOf(true)))), false);
            }
            System.out.println("[GuildsRemastered] Player " + player.getName().getString() + " joined guild: " + guild.getName());
            return 1;
        }))).then(CommandManager.literal((String)"quit").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (guild.getOwnerId().equals(playerId)) {
                source.sendError((Text)Text.literal((String)"You are the Guild Master. Use /guild disband instead."));
                return 0;
            }
            String playerName = player.getName().getString();
            guild.removeMember(playerId);
            GuildDataManager.saveGuild(server, guild);
            if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                GuildPrefixHelper.clearNickname(player);
            }
            if (GuildsConfig.enableHungerBoundIntegration) {
                GuildHungerBoundPrefixHelper.clearNickname(player);
            }
            GuildTeamUtil.clearTeamForPlayer(player, server);
            GuildTeamUtil.forceUpdateForPlayer(player);
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), "team leave " + playerName);
            player.sendMessage((Text)Text.literal((String)("You have left the guild " + guild.getName())).formatted(Formatting.GOLD));
            for (UUID memberId : guild.getMembers().keySet()) {
                ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                if (member == null) continue;
                member.sendMessage((Text)Text.literal((String)(playerName + " has left the guild.")).formatted(Formatting.GRAY));
            }
            return 1;
        }))).then(CommandManager.literal((String)"kick").then(CommandManager.argument((String)"player", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> {
            ServerPlayerEntity executor = ((ServerCommandSource)ctx.getSource()).getPlayer();
            Guild guild = GuildDataManager.getGuildByPlayer(executor.getUuid());
            if (guild != null && guild.hasRankPermission(executor.getUuid(), "canKick")) {
                for (Map.Entry<UUID, Guild.GuildMemberInfo> entry : guild.getMembers().entrySet()) {
                    UUID memberId = entry.getKey();
                    Guild.GuildMemberInfo info = entry.getValue();
                    if (memberId.equals(executor.getUuid()) || memberId.equals(guild.getOwnerId())) continue;
                    builder.suggest(info.name);
                }
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity executor = source.getPlayer();
            MinecraftServer server = source.getServer();
            String targetName = StringArgumentType.getString((CommandContext)ctx, (String)"player");
            Guild guild = GuildDataManager.getGuildByPlayer(executor.getUuid());
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!guild.hasRankPermission(executor.getUuid(), "canKick")) {
                source.sendError((Text)Text.literal((String)"You do not have permission to kick members from the guild."));
                return 0;
            }
            UUID targetId = null;
            Guild.GuildMemberInfo targetInfo = null;
            for (Map.Entry<UUID, Guild.GuildMemberInfo> entry : guild.getMembers().entrySet()) {
                if (!entry.getValue().name.equalsIgnoreCase(targetName)) continue;
                targetId = entry.getKey();
                targetInfo = entry.getValue();
                break;
            }
            if (targetId == null || targetId.equals(executor.getUuid())) {
                source.sendError((Text)Text.literal((String)"Invalid target. You cannot kick yourself or a non-member."));
                return 0;
            }
            if (targetId.equals(guild.getOwnerId())) {
                source.sendError((Text)Text.literal((String)"You cannot kick the Guild Master."));
                return 0;
            }
            guild.removeMember(targetId);
            GuildDataManager.saveGuild(server, guild);
            String kickedName = targetInfo.name;
            source.sendFeedback(() -> Text.literal((String)(kickedName + " has been kicked from the guild.")), false);
            ServerPlayerEntity kicked = server.getPlayerManager().getPlayer(targetId);
            if (kicked != null) {
                if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                    GuildPrefixHelper.clearNickname(kicked);
                }
                if (GuildsConfig.enableHungerBoundIntegration) {
                    GuildHungerBoundPrefixHelper.clearNickname(kicked);
                }
                GuildTeamUtil.clearTeamForPlayer(kicked, server);
                GuildTeamUtil.forceUpdateForPlayer(kicked);
                kicked.sendMessage((Text)Text.literal((String)("You have been kicked from the guild " + guild.getName())).formatted(Formatting.RED));
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), "team leave " + kickedName);
            }
            for (UUID memberId : guild.getMembers().keySet()) {
                ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                if (member == null) continue;
                member.sendMessage((Text)Text.literal((String)(kickedName + " has been kicked from the guild.")).formatted(Formatting.GRAY));
            }
            return 1;
        })))).then(CommandManager.literal((String)"chat").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            GuildDataManager.ChatStateManager.toggleGuildChat(playerId);
            boolean enabled = GuildDataManager.ChatStateManager.isGuildChatEnabled(playerId);
            if (enabled) {
                player.sendMessage((Text)Text.literal((String)"\ud83d\udfe2 Guild Chat Enabled! ").formatted(Formatting.GREEN).append((Text)Text.literal((String)"Your messages will now only go to your guild.")).formatted(Formatting.GRAY), false);
            } else {
                player.sendMessage((Text)Text.literal((String)"\ud83d\udd34 Guild Chat Disabled. ").formatted(Formatting.RED).append((Text)Text.literal((String)"You're now talking in global chat.")).formatted(Formatting.GRAY), false);
            }
            return 1;
        }))).then(CommandManager.literal((String)"promote").then(CommandManager.argument((String)"player", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> {
            ServerPlayerEntity executor = ((ServerCommandSource)ctx.getSource()).getPlayer();
            Guild guild = GuildDataManager.getGuildByPlayer(executor.getUuid());
            if (guild != null && guild.hasRankPermission(executor.getUuid(), "canPromote")) {
                UUID executorId = executor.getUuid();
                for (Map.Entry<UUID, Guild.GuildMemberInfo> entry : guild.getMembers().entrySet()) {
                    String rank;
                    UUID memberId = entry.getKey();
                    Guild.GuildMemberInfo info = entry.getValue();
                    if (memberId.equals(executorId) || !(rank = info.rank.toLowerCase()).equals("initiate") && !rank.equals("member")) continue;
                    builder.suggest(info.name);
                }
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity executor = source.getPlayer();
            MinecraftServer server = source.getServer();
            String targetName = StringArgumentType.getString((CommandContext)ctx, (String)"player");
            Guild guild = GuildDataManager.getGuildByPlayer(executor.getUuid());
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!guild.hasRankPermission(executor.getUuid(), "canPromote")) {
                source.sendError((Text)Text.literal((String)"You do not have permission to promote members in the guild."));
                return 0;
            }
            UUID targetId = null;
            Guild.GuildMemberInfo targetInfo = null;
            for (Map.Entry<UUID, Guild.GuildMemberInfo> entry : guild.getMembers().entrySet()) {
                if (!entry.getValue().name.equalsIgnoreCase(targetName)) continue;
                targetId = entry.getKey();
                targetInfo = entry.getValue();
                break;
            }
            if (targetId == null || targetId.equals(executor.getUuid())) {
                source.sendError((Text)Text.literal((String)"Invalid target. You cannot promote yourself or a non-member."));
                return 0;
            }
            String currentRank = targetInfo.rank.toLowerCase();
            String newRank = null;
            if ("initiate".equals(currentRank)) {
                newRank = "Member";
            } else if ("member".equals(currentRank)) {
                newRank = "Officer";
            }
            if (newRank == null) {
                source.sendError((Text)Text.literal((String)("Cannot promote " + targetInfo.name + " any further.")));
                return 0;
            }
            targetInfo.rank = newRank;
            GuildDataManager.saveGuild(server, guild);
            Guild.GuildMemberInfo finalTargetInfo = targetInfo;
            String promotedTo = newRank;
            source.sendFeedback(() -> Text.literal((String)(finalTargetInfo.name + " has been promoted to " + promotedTo + ".")), false);
            return 1;
        })))).then(CommandManager.literal((String)"demote").then(CommandManager.argument((String)"player", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> {
            ServerPlayerEntity executor = ((ServerCommandSource)ctx.getSource()).getPlayer();
            Guild guild = GuildDataManager.getGuildByPlayer(executor.getUuid());
            if (guild != null && guild.hasRankPermission(executor.getUuid(), "canDemote")) {
                UUID executorId = executor.getUuid();
                for (Map.Entry<UUID, Guild.GuildMemberInfo> entry : guild.getMembers().entrySet()) {
                    String rank;
                    UUID memberId = entry.getKey();
                    Guild.GuildMemberInfo info = entry.getValue();
                    if (memberId.equals(executorId) || memberId.equals(guild.getOwnerId()) || !(rank = info.rank.toLowerCase()).equals("officer") && !rank.equals("member")) continue;
                    builder.suggest(info.name);
                }
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity executor = source.getPlayer();
            MinecraftServer server = source.getServer();
            String targetName = StringArgumentType.getString((CommandContext)ctx, (String)"player");
            Guild guild = GuildDataManager.getGuildByPlayer(executor.getUuid());
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!guild.hasRankPermission(executor.getUuid(), "canDemote")) {
                source.sendError((Text)Text.literal((String)"You do not have permission to demote members in the guild."));
                return 0;
            }
            UUID executorId = executor.getUuid();
            UUID targetId = null;
            Guild.GuildMemberInfo targetInfo = null;
            for (Map.Entry<UUID, Guild.GuildMemberInfo> entry : guild.getMembers().entrySet()) {
                if (!entry.getValue().name.equalsIgnoreCase(targetName)) continue;
                targetId = entry.getKey();
                targetInfo = entry.getValue();
                break;
            }
            if (targetId == null || targetId.equals(executorId)) {
                source.sendError((Text)Text.literal((String)"Invalid target. You cannot demote yourself or a non-member."));
                return 0;
            }
            if (targetId.equals(guild.getOwnerId())) {
                source.sendError((Text)Text.literal((String)"You cannot demote the Guild Master."));
                return 0;
            }
            String currentRank = targetInfo.rank.toLowerCase();
            String newRank = null;
            if ("officer".equals(currentRank)) {
                newRank = "Member";
            } else if ("member".equals(currentRank)) {
                newRank = "Initiate";
            }
            if (newRank == null) {
                source.sendError((Text)Text.literal((String)("Cannot demote " + targetInfo.name + " any further.")));
                return 0;
            }
            targetInfo.rank = newRank;
            GuildDataManager.saveGuild(server, guild);
            Guild.GuildMemberInfo finalTargetInfo = targetInfo;
            String demotedTo = newRank;
            source.sendFeedback(() -> Text.literal((String)(finalTargetInfo.name + " has been demoted to " + demotedTo + ".")), false);
            return 1;
        })))).then(CommandManager.literal((String)"toggle_friendlyfire").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!guild.hasRankPermission(playerId, "canTogglePvP")) {
                source.sendError((Text)Text.literal((String)"Only the Guild Master or Officers can toggle friendly fire."));
                return 0;
            }
            String teamName = "guild_" + guild.getId().toString().substring(0, 8);
            ServerScoreboard scoreboard = player.getServer().getScoreboard();
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                source.sendError((Text)Text.literal((String)"Your guild team was not found."));
                return 0;
            }
            boolean current = team.isFriendlyFireAllowed();
            team.setFriendlyFireAllowed(!current);
            Formatting stateFmt = !current ? Formatting.GREEN : Formatting.RED;
            source.sendFeedback(() -> Text.literal((String)"Friendly fire has been ").append(Text.literal((String)(!current ? "enabled" : "disabled")).formatted(stateFmt)), true);
            return 1;
        }))).then(CommandManager.literal((String)"motd").then(CommandManager.argument((String)"message", (ArgumentType)StringArgumentType.greedyString()).executes(ctx -> {
            boolean isOfficer;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            Guild.GuildMemberInfo info = guild.getMembers().get(playerId);
            boolean bl = isOfficer = info != null && info.rank != null && info.rank.equalsIgnoreCase("officer");
            if (!guild.getOwnerId().equals(playerId) && !isOfficer) {
                source.sendError((Text)Text.literal((String)"Only the guild master or an officer can set the MOTD."));
                return 0;
            }
            String motd = StringArgumentType.getString((CommandContext)ctx, (String)"message");
            guild.setMotd(motd);
            GuildDataManager.saveGuild(source.getServer(), guild);
            source.sendFeedback(() -> Text.literal((String)"Guild MOTD updated to: ").append((Text)Text.literal((String)motd).styled(style -> style.withColor(Formatting.YELLOW))), false);
            return 1;
        })))).then(CommandManager.literal((String)"rename").then(CommandManager.argument((String)"name", (ArgumentType)StringArgumentType.word()).then(CommandManager.argument((String)"color", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            Formatting color;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            String newName = StringArgumentType.getString((CommandContext)ctx, (String)"name");
            if (newName.length() > 13) {
                source.sendError((Text)Text.literal((String)"The guild name must be 13 characters or fewer."));
                return 0;
            }
            String colorName = StringArgumentType.getString((CommandContext)ctx, (String)"color").toUpperCase();
            try {
                color = Formatting.valueOf((String)colorName);
                if (!color.isColor()) {
                    source.sendError((Text)Text.literal((String)"That is not a valid color."));
                    return 0;
                }
            }
            catch (IllegalArgumentException e) {
                source.sendError((Text)Text.literal((String)"Invalid color. Use names like red, blue, green."));
                return 0;
            }
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!guild.getOwnerId().equals(playerId)) {
                source.sendError((Text)Text.literal((String)"Only the guild master can rename the guild."));
                return 0;
            }
            if (GuildDataManager.isGuildNameTaken(newName)) {
                source.sendError((Text)Text.literal((String)"A guild with that name already exists."));
                return 0;
            }
            String oldName = guild.getName();
            guild.setName(newName);
            guild.setColor(color.getName().toLowerCase());
            guild.setPrefix(newName);
            GuildDataManager.saveGuild(source.getServer(), guild);
            UUID renamedGuildId = guild.getId();
            String renamedShortId = "guild_" + renamedGuildId.toString().substring(0, 8);
            MinecraftServer server = source.getServer();
            for (String allyShortId : guild.getAllies().keySet()) {
                Map<String, Guild.AllyInfo> theirAllies;
                Guild.AllyInfo info;
                Guild allyGuild = GuildDataManager.getGuildByShortId(allyShortId);
                if (allyGuild == null || (info = (theirAllies = allyGuild.getAllies()).get(renamedShortId)) == null) continue;
                info.name = newName;
                GuildDataManager.saveGuild(server, allyGuild);
                for (UUID memberId : allyGuild.getMembers().keySet()) {
                    ServerPlayerEntity allyPlayer = server.getPlayerManager().getPlayer(memberId);
                    if (allyPlayer == null) continue;
                    allyPlayer.sendMessage((Text)Text.literal((String)"Your ally ").append((Text)Text.literal((String)oldName).formatted(Formatting.GREEN)).append((Text)Text.literal((String)" has changed its name to ")).append((Text)Text.literal((String)newName).formatted(Formatting.GREEN)).append((Text)Text.literal((String)".")));
                }
            }
            String teamName = "guild_" + guild.getId().toString().substring(0, 8);
            ServerScoreboard scoreboard = server.getScoreboard();
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.setColor(color);
                System.out.println("[GuildsRemastered] Updated team color for " + teamName + " to " + color.getName().toLowerCase());
            }
            for (UUID memberId : guild.getMembers().keySet()) {
                ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                if (member == null) continue;
                if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                    GuildPrefixHelper.applyGuildPrefix(member);
                }
                if (!GuildsConfig.enableHungerBoundIntegration) continue;
                GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(member);
            }
            source.sendFeedback(() -> Text.literal((String)"Guild successfully renamed to ").append((Text)Text.literal((String)newName).formatted(color)).append((Text)Text.literal((String)".")), false);
            return 1;
        }))))).then(CommandManager.literal((String)"sethome").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!guild.hasRankPermission(playerId, "canSetHome")) {
                source.sendError((Text)Text.literal((String)"You do not have permission to use /guild sethome."));
                return 0;
            }
            Vec3d pos = player.getPos();
            String worldKey = player.getWorld().getRegistryKey().getValue().toString();
            guild.setHome(pos.getX(), pos.getY(), pos.getZ(), worldKey);
            GuildDataManager.saveGuild(source.getServer(), guild);
            source.sendFeedback(() -> Text.literal((String)"Guild home set!"), false);
            return 1;
        }))).then(CommandManager.literal((String)"home").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!guild.hasRankPermission(playerId, "canUseHome")) {
                source.sendError((Text)Text.literal((String)"You do not have permission to use the guild home."));
                return 0;
            }
            if (!guild.hasHome()) {
                source.sendError((Text)Text.literal((String)"Your guild has not set a home yet."));
                return 0;
            }
            String worldName = guild.getHomeWorld();
            String[] parts = worldName.split(":");
            if (parts.length != 2) {
                source.sendError((Text)Text.literal((String)("Invalid world identifier: " + worldName)));
                return 0;
            }
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(parts[0], parts[1]));
            ServerWorld world = source.getServer().getWorld(worldKey);
            if (world == null) {
                source.sendError((Text)Text.literal((String)"The world for the guild home could not be found."));
                return 0;
            }
            int delaySeconds = GuildsConfig.guildHomeTeleportDelaySeconds;
            int delayTicks = delaySeconds * 20;
            player.sendMessage((Text)Text.literal((String)("Teleporting to your guild home in " + delaySeconds + " seconds...")).formatted(Formatting.AQUA), false);
            DelayedTeleportScheduler.schedule(player, delayTicks, () -> {
                if (player.isAlive() && player.getWorld() != null) {
                    player.teleport(world, guild.getHomeX(), guild.getHomeY(), guild.getHomeZ(), player.getYaw(), player.getPitch());
                    player.sendMessage((Text)Text.literal((String)"You have arrived at your guild home.").formatted(Formatting.GREEN), false);
                }
            });
            return 1;
        }))).then(CommandManager.literal((String)"ally").then(CommandManager.argument((String)"guild", (ArgumentType)StringArgumentType.greedyString()).executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity sender = source.getPlayer();
            MinecraftServer server = source.getServer();
            String targetGuildName = StringArgumentType.getString((CommandContext)ctx, (String)"guild");
            Guild senderGuild = GuildDataManager.getGuildByPlayer(sender.getUuid());
            if (senderGuild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            String senderRank = senderGuild.getRank(sender.getUuid());
            if (!senderRank.equalsIgnoreCase("guild master")) {
                source.sendError((Text)Text.literal((String)"Only guild masters can propose alliances."));
                return 0;
            }
            Guild targetGuild = GuildDataManager.getGuildByName(targetGuildName);
            if (targetGuild == null) {
                source.sendError((Text)Text.literal((String)("Guild not found: " + targetGuildName)));
                return 0;
            }
            if (senderGuild.getId().equals(targetGuild.getId())) {
                source.sendError((Text)Text.literal((String)"You cannot ally with your own guild."));
                return 0;
            }
            String targetShortId = "guild_" + targetGuild.getId().toString().substring(0, 8);
            if (senderGuild.getAllies().containsKey(targetShortId)) {
                MutableText yesButton = Text.literal((String)"[YES]").setStyle(Style.EMPTY.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild allyrevoke " + targetShortId)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to revoke alliance"))));
                MutableText noButton = Text.literal((String)"[NO]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild cancel")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to cancel"))));
                sender.sendMessage((Text)Text.literal((String)"You are already allied with ").append((Text)Text.literal((String)targetGuild.getName()).formatted(Formatting.YELLOW)).append((Text)Text.literal((String)". Do you want to revoke this alliance? ")).append((Text)yesButton).append((Text)Text.literal((String)" ")).append((Text)noButton), false);
                return 0;
            }
            ServerPlayerEntity targetMaster = server.getPlayerManager().getPlayer(targetGuild.getOwnerId());
            if (targetMaster == null) {
                source.sendError((Text)Text.literal((String)("The guild master of " + targetGuild.getName() + " is not online.")));
                return 0;
            }
            GuildDataManager.addAllyRequest(targetGuild.getOwnerId(), senderGuild.getId());
            sender.sendMessage((Text)Text.literal((String)("Alliance request sent to " + targetGuild.getName())).formatted(Formatting.GREEN), false);
            MutableText acceptButton = Text.literal((String)"[ACCEPT]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild allyaccept " + String.valueOf(senderGuild.getId()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to accept the alliance"))));
            MutableText denyButton = Text.literal((String)"[DENY]").setStyle(Style.EMPTY.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild allydeny " + String.valueOf(senderGuild.getId()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal((String)"Click to deny the alliance"))));
            targetMaster.sendMessage((Text)Text.literal((String)"Your guild has received an alliance request from ").append((Text)Text.literal((String)senderGuild.getName()).formatted(Formatting.YELLOW)).append((Text)Text.literal((String)". ")).append((Text)acceptButton).append((Text)Text.literal((String)" ")).append((Text)denyButton), false);
            return 1;
        })))).then(CommandManager.literal((String)"allyaccept").then(CommandManager.argument((String)"guildId", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            UUID requesterGuildId;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            String guildIdStr = StringArgumentType.getString((CommandContext)ctx, (String)"guildId");
            try {
                requesterGuildId = UUID.fromString(guildIdStr);
            }
            catch (IllegalArgumentException e) {
                source.sendError((Text)Text.literal((String)"Invalid guild ID format."));
                return 0;
            }
            Guild receiverGuild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (receiverGuild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!receiverGuild.getOwnerId().equals(player.getUuid())) {
                source.sendError((Text)Text.literal((String)"Only the guild master can accept alliance requests."));
                return 0;
            }
            UUID expected = GuildDataManager.getAllyRequest(player.getUuid());
            if (!requesterGuildId.equals(expected)) {
                source.sendError((Text)Text.literal((String)"No pending alliance request from that guild."));
                return 0;
            }
            Guild requesterGuild = GuildDataManager.getGuildById(requesterGuildId);
            if (requesterGuild == null) {
                source.sendError((Text)Text.literal((String)"The requesting guild no longer exists."));
                return 0;
            }
            receiverGuild.addAlly(requesterGuild.getId(), requesterGuild.getName());
            requesterGuild.addAlly(receiverGuild.getId(), receiverGuild.getName());
            AllyChatBridgeManager.createBridge(receiverGuild.getShortenedId(), requesterGuild.getShortenedId());
            GuildDataManager.removeAllyRequest(player.getUuid());
            GuildDataManager.saveGuild(server, receiverGuild);
            GuildDataManager.saveGuild(server, requesterGuild);
            player.sendMessage((Text)Text.literal((String)("You are now allied with " + requesterGuild.getName() + "!")).formatted(Formatting.GREEN), false);
            ServerPlayerEntity requesterMaster = server.getPlayerManager().getPlayer(requesterGuild.getOwnerId());
            if (requesterMaster != null) {
                requesterMaster.sendMessage((Text)Text.literal((String)("Your alliance request was accepted by " + receiverGuild.getName() + "!")).formatted(Formatting.GREEN), false);
            }
            return 1;
        })))).then(CommandManager.literal((String)"allydeny").then(CommandManager.argument((String)"guildId", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> {
            UUID playerId = ((ServerCommandSource)ctx.getSource()).getPlayer().getUuid();
            UUID requesterId = GuildDataManager.getAllyRequest(playerId);
            if (requesterId != null) {
                builder.suggest(requesterId.toString());
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            UUID requesterGuildId;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            UUID playerId = player.getUuid();
            String guildIdStr = StringArgumentType.getString((CommandContext)ctx, (String)"guildId");
            try {
                requesterGuildId = UUID.fromString(guildIdStr);
            }
            catch (IllegalArgumentException e) {
                source.sendError((Text)Text.literal((String)"Invalid guild ID format."));
                return 0;
            }
            Guild receiverGuild = GuildDataManager.getGuildByPlayer(playerId);
            if (receiverGuild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!receiverGuild.getOwnerId().equals(playerId)) {
                source.sendError((Text)Text.literal((String)"Only the guild master can deny alliance requests."));
                return 0;
            }
            UUID expected = GuildDataManager.getAllyRequest(playerId);
            if (!requesterGuildId.equals(expected)) {
                source.sendError((Text)Text.literal((String)"You have no pending alliance request from that guild."));
                return 0;
            }
            Guild senderGuild = GuildDataManager.getGuildById(requesterGuildId);
            if (senderGuild == null) {
                source.sendError((Text)Text.literal((String)"That guild does not exist."));
                return 0;
            }
            GuildDataManager.removeAllyRequest(playerId);
            player.sendMessage((Text)Text.literal((String)"You have denied the alliance request from ").append((Text)Text.literal((String)senderGuild.getName()).formatted(Formatting.RED)));
            ServerPlayerEntity senderPlayer = server.getPlayerManager().getPlayer(senderGuild.getOwnerId());
            if (senderPlayer != null) {
                senderPlayer.sendMessage((Text)Text.literal((String)"Your alliance request to ").append((Text)Text.literal((String)receiverGuild.getName()).formatted(Formatting.RED)).append((Text)Text.literal((String)" was denied.")));
            }
            return 1;
        })))).then(CommandManager.literal((String)"allyrevoke").then(CommandManager.argument((String)"guildId", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> {
            UUID playerId = ((ServerCommandSource)ctx.getSource()).getPlayer().getUuid();
            Guild playerGuild = GuildDataManager.getGuildByPlayer(playerId);
            if (playerGuild != null) {
                for (Map.Entry<String, Guild.AllyInfo> ally : playerGuild.getAllies().entrySet()) {
                    String shortId = ally.getKey();
                    String name = ally.getValue().name;
                    builder.suggest(shortId, (Message)Text.literal((String)name));
                }
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            UUID playerId = player.getUuid();
            String shortId = StringArgumentType.getString((CommandContext)ctx, (String)"guildId");
            Guild selfGuild = GuildDataManager.getGuildByPlayer(playerId);
            if (selfGuild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!selfGuild.getOwnerId().equals(playerId)) {
                source.sendError((Text)Text.literal((String)"Only the guild master can revoke alliances."));
                return 0;
            }
            if (!selfGuild.getAllies().containsKey(shortId)) {
                source.sendError((Text)Text.literal((String)"You are not currently allied with that guild."));
                return 0;
            }
            Guild targetGuild = null;
            for (Guild g : GuildDataManager.getAllGuilds()) {
                if (!g.getShortenedId().equals(shortId)) continue;
                targetGuild = g;
                break;
            }
            if (targetGuild == null) {
                source.sendError((Text)Text.literal((String)"The target guild could not be found."));
                return 0;
            }
            selfGuild.getAllies().remove(shortId);
            String selfShortId = selfGuild.getShortenedId();
            targetGuild.getAllies().remove(selfShortId);
            GuildDataManager.saveGuild(server, selfGuild);
            GuildDataManager.saveGuild(server, targetGuild);
            AllyChatBridgeManager.removeBridge(selfShortId, shortId);
            player.sendMessage((Text)Text.literal((String)"You have revoked your alliance with ").append((Text)Text.literal((String)targetGuild.getName()).formatted(Formatting.RED)));
            ServerPlayerEntity targetOwner = server.getPlayerManager().getPlayer(targetGuild.getOwnerId());
            if (targetOwner != null) {
                targetOwner.sendMessage((Text)Text.literal((String)(selfGuild.getName() + " has revoked their alliance with your guild.")).formatted(Formatting.GRAY));
            }
            return 1;
        })))).then(CommandManager.literal((String)"bank").then(CommandManager.argument((String)"page", (ArgumentType)IntegerArgumentType.integer((int)1)).executes(context -> {
            int unlockedTabs;
            ServerCommandSource source = (ServerCommandSource)context.getSource();
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError((Text)Text.literal((String)"Command can only be run by a player."));
                return 0;
            }
            final Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild == null) {
                player.sendMessage((Text)Text.literal((String)"You must be in a guild to use this command!"), false);
                return 0;
            }
            final int requestedPage = IntegerArgumentType.getInteger((CommandContext)context, (String)"page");
            if (requestedPage > (unlockedTabs = guild.getUnlockedBankTabs())) {
                player.sendMessage((Text)Text.literal((String)("\u00a7cBank Tab " + requestedPage + " is locked. Donate more Netherite to unlock it!")), false);
                return 0;
            }
            String permissionKey = "canUseBankTab" + requestedPage;
            if (!guild.hasRankPermission(player.getUuid(), permissionKey)) {
                source.sendError((Text)Text.literal((String)"You do not have permission to access this bank page."));
                return 0;
            }
            final List<GuildBankInventory> guildBankPages = GuildBankManager.getOrLoadBankPages(player.getServer(), guild.getShortenedId());
            if (requestedPage < 1 || requestedPage > guildBankPages.size()) {
                player.sendMessage((Text)Text.literal((String)("Invalid page number. Please request a page between 1 and " + guildBankPages.size() + ".")), false);
                return 0;
            }
            final int startingPageIndex = requestedPage - 1;
            player.openHandledScreen(new NamedScreenHandlerFactory(){

                public Text getDisplayName() {
                    return Text.literal((String)(guild.getName() + " Guild Bank (Page " + requestedPage + ")"));
                }

                public GuildBankScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                    return new GuildBankScreenHandler(syncId, playerInventory, guildBankPages, startingPageIndex);
                }
            });
            player.sendMessage((Text)Text.literal((String)("Opened guild bank page " + requestedPage + ".")), false);
            return 1;
        })))).then(CommandManager.literal((String)"permissions").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild == null) {
                System.out.println("[GuildsRemastered] /guild permissions failed: player not in a guild (" + player.getName().getString() + ")");
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            if (!guild.getOwnerId().equals(player.getUuid())) {
                System.out.println("[GuildsRemastered] /guild permissions failed: player not guild master (" + player.getName().getString() + ")");
                source.sendError((Text)Text.literal((String)"Only the guild master can open the permissions menu."));
                return 0;
            }
            try {
                GuildPermissionsMenu.openForPlayer(player);
                System.out.println("[GuildsRemastered] /guild permissions opened successfully for " + player.getName().getString());
                return 1;
            }
            catch (Exception e) {
                System.out.println("[GuildsRemastered] ERROR: Failed to open guild permissions menu for " + player.getName().getString());
                e.printStackTrace();
                source.sendError((Text)Text.literal((String)"An unexpected error occurred opening the permissions menu. Please report this to an admin."));
                return 0;
            }
        }))).then(CommandManager.literal((String)"donate").executes(ctx -> {
            int currentProgress;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"\u00a7cYou are not in a guild."));
                return 0;
            }
            if (!guild.hasRankPermission(player.getUuid(), "canDonate")) {
                source.sendError((Text)Text.literal((String)"\u00a7cYou do not have permission to donate Netherite for your guild."));
                return 0;
            }
            ItemStack hand = player.getMainHandStack();
            if (!hand.isOf(Items.NETHERITE_INGOT)) {
                source.sendError((Text)Text.literal((String)"\u00a7cYou must hold Netherite Ingots in your main hand to donate."));
                return 0;
            }
            int held = hand.getCount();
            int nextTab = guild.getUnlockedBankTabs() + 1;
            if (nextTab > 9) {
                source.sendError((Text)Text.literal((String)"\u00a7eYour guild has already unlocked all 9 bank tabs."));
                return 0;
            }
            int cost = GuildCommand.getRequiredNetheriteForTab(nextTab);
            int needed = cost - (currentProgress = guild.getBankUnlockProgress(nextTab));
            if (needed <= 0) {
                source.sendError((Text)Text.literal((String)("\u00a7aTab " + nextTab + " is already unlocked or ready to unlock.")));
                return 0;
            }
            int toDeposit = Math.min(held, needed);
            guild.addBankUnlockProgress(nextTab, toDeposit);
            hand.decrement(toDeposit);
            player.setStackInHand(player.getActiveHand(), hand);
            GuildDataManager.saveGuild(server, guild);
            player.sendMessage((Text)Text.literal((String)("\u00a77Donated " + toDeposit + " Netherite Ingot(s) toward unlocking Bank Tab " + nextTab + ".")), false);
            int newProgress = guild.getBankUnlockProgress(nextTab);
            if (newProgress >= cost) {
                guild.setUnlockedBankTabs(nextTab);
                GuildDataManager.saveGuild(server, guild);
                for (UUID memberId : guild.getMembers().keySet()) {
                    ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                    if (member == null) continue;
                    member.sendMessage((Text)Text.literal((String)("\u00a7aYour guild has unlocked Bank Tab " + nextTab + "!")).formatted(Formatting.GREEN));
                }
            }
            return 1;
        }))).then(CommandManager.literal((String)"progress").executes(context -> {
            ServerCommandSource source = (ServerCommandSource)context.getSource();
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError((Text)Text.literal((String)"Only players can run this command."));
                return 0;
            }
            Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You are not in a guild."));
                return 0;
            }
            int unlocked = guild.getUnlockedBankTabs();
            if (unlocked >= 9) {
                player.sendMessage((Text)Text.literal((String)"\u00a76Guild Bank Progress:").append((Text)Text.literal((String)"\n\u00a7aYour guild has already unlocked all 9 bank pages!")), false);
                return 1;
            }
            int nextTab = unlocked + 1;
            int required = guild.getRequiredNetheriteForTab(nextTab);
            int progress = guild.getNetheriteDonatedForTab(nextTab);
            int remaining = Math.max(0, required - progress);
            player.sendMessage((Text)Text.literal((String)"\u00a76Guild Bank Progress:").append((Text)Text.literal((String)("\n\u00a7ePage " + nextTab + " requires \u00a7f" + required + " Netherite Ingots"))).append((Text)Text.literal((String)("\n\u00a77Progress: \u00a7e" + progress + " / " + required))).append((Text)Text.literal((String)("\n\u00a7a" + remaining + " more needed to unlock!"))), false);
            return 1;
        }))).then(CommandManager.literal((String)"unlock").executes(context -> {
            ServerCommandSource source = (ServerCommandSource)context.getSource();
            ServerPlayerEntity executor = source.getPlayer();
            Guild guild = GuildDataManager.getGuildByPlayer(executor.getUuid());
            if (guild == null) {
                source.sendError((Text)Text.literal((String)"You must be in a guild to unlock a bank tab."));
                return 0;
            }
            int nextTab = guild.getUnlockedBankTabs() + 1;
            int requiredNetherite = guild.getRequiredNetheriteForTab(nextTab);
            int currentDonated = guild.getBankUnlockProgress(nextTab);
            if (currentDonated < requiredNetherite) {
                int remainingAmount = requiredNetherite - currentDonated;
                source.sendError((Text)Text.literal((String)("You still need " + remainingAmount + " Netherite to unlock bank tab " + nextTab + ".")));
                return 0;
            }
            guild.setUnlockedBankTabs(nextTab);
            source.sendFeedback(() -> Text.literal((String)("Successfully unlocked bank tab " + nextTab + ".")), false);
            return 1;
        }))).then(CommandManager.literal((String)"cancel").executes(ctx -> {
            ((ServerCommandSource)ctx.getSource()).getPlayer().sendMessage((Text)Text.literal((String)"Action cancelled.").formatted(Formatting.GRAY));
            return 1;
        })));
    }

    public static int getRequiredNetheriteForTab(int nextTab) {
        int baseCost = 8;
        return baseCost * nextTab;
    }

    private static void displayGuildInfo(MinecraftServer server, ServerCommandSource source, Guild guild) {
        Formatting guildColor = GuildColorUtil.getFormatting(guild.getColor());
        source.sendMessage((Text)Text.literal((String)"Guild: ").append((Text)Text.literal((String)guild.getName()).formatted(guildColor)));
        if (guild.getMotd() != null && !guild.getMotd().isEmpty()) {
            source.sendMessage((Text)Text.literal((String)"MOTD: ").append((Text)Text.literal((String)guild.getMotd()).styled(style -> style.withColor(guildColor).withItalic(Boolean.valueOf(true)))));
        }
        boolean hasHome = guild.hasHome();
        MutableText homeLine = Text.literal((String)"Guild Home Set: ").formatted(Formatting.AQUA);
        homeLine.append(Text.literal((String)String.valueOf(hasHome)).formatted(hasHome ? Formatting.GREEN : Formatting.RED));
        source.sendMessage((Text)homeLine);
        source.sendMessage((Text)Text.literal((String)"Created: ").append((Text)Text.literal((String)guild.getFormattedDate()).formatted(Formatting.WHITE)));
        source.sendMessage((Text)Text.literal((String)"Guild Master: ").append((Text)Text.literal((String)guild.getOwnerName()).formatted(Formatting.AQUA)));
        source.sendMessage((Text)Text.literal((String)("Members (" + guild.getMembers().size() + "):")));
        ArrayList<String> online = new ArrayList<String>();
        ArrayList<String> offline = new ArrayList<String>();
        for (Map.Entry<UUID, Guild.GuildMemberInfo> entry : guild.getMembers().entrySet()) {
            UUID memberId = entry.getKey();
            Guild.GuildMemberInfo info = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(memberId);
            String display = "- " + info.name + " [" + info.rank + "]";
            if (player != null) {
                online.add(display);
                continue;
            }
            offline.add(display);
        }
        if (!online.isEmpty()) {
            source.sendMessage((Text)Text.literal((String)"Online:").formatted(Formatting.GREEN));
            for (String string : online) {
                source.sendMessage((Text)Text.literal((String)string).formatted(Formatting.GREEN));
            }
        }
        if (!offline.isEmpty()) {
            source.sendMessage((Text)Text.literal((String)"Offline:").formatted(Formatting.RED));
            for (String string : offline) {
                source.sendMessage((Text)Text.literal((String)string).formatted(Formatting.RED));
            }
        }
        if (!guild.getAllies().isEmpty()) {
            Collection<Guild.AllyInfo> allyInfos = guild.getAllies().values();
            MutableText alliesLine = Text.literal((String)"Allies: ").formatted(Formatting.AQUA);
            boolean first = true;
            for (Guild.AllyInfo ally : allyInfos) {
                if (!first) {
                    alliesLine.append((Text)Text.literal((String)", ").formatted(Formatting.GRAY));
                }
                MutableText nameText = Text.literal((String)ally.name).formatted(Formatting.GREEN);
                nameText = ally.pvpDisabled ? nameText.append((Text)Text.literal((String)" (PvP Off)").formatted(Formatting.RED)) : nameText.append((Text)Text.literal((String)" (PvP On)").formatted(Formatting.DARK_GREEN));
                alliesLine.append((Text)nameText);
                first = false;
            }
            source.sendMessage((Text)alliesLine);
        } else {
            MutableText noAllies = Text.literal((String)"").append((Text)Text.literal((String)"Allies: ").formatted(Formatting.AQUA)).append((Text)Text.literal((String)"N/A").formatted(Formatting.RED));
            source.sendMessage((Text)noAllies);
        }
    }
}

