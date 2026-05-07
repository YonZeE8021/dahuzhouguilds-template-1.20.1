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
import com.dahuzhou.dahuzhouguilds.GuildTexts;
import com.dahuzhou.dahuzhouguilds.GuildsConfig;
import com.dahuzhou.dahuzhouguilds.data.GuildBankManager;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.AllyChatBridgeManager;
import com.dahuzhou.dahuzhouguilds.util.DelayedTeleportScheduler;
import com.dahuzhou.dahuzhouguilds.util.GuildBankInventory;
import com.dahuzhou.dahuzhouguilds.util.GuildBankScreenHandler;
import com.dahuzhou.dahuzhouguilds.util.GuildDisplayUtil;
import com.dahuzhou.dahuzhouguilds.util.GuildColorUtil;
import com.dahuzhou.dahuzhouguilds.util.GuildNameUtil;
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
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"guild").then(CommandManager.literal((String)"create").then(CommandManager.argument((String)"name", (ArgumentType)StringArgumentType.string()).then(CommandManager.argument((String)"color", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            Formatting color;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            String name = StringArgumentType.getString((CommandContext)ctx, (String)"name");
            if (!GuildNameUtil.isWithinMaxLength(name)) {
                source.sendError(GuildTexts.t("error.guild_name_too_long", GuildNameUtil.MAX_GUILD_NAME_CODE_POINTS));
                return 0;
            }
            String colorName = StringArgumentType.getString((CommandContext)ctx, (String)"color").toUpperCase();
            try {
                color = Formatting.valueOf((String)colorName);
                if (!color.isColor()) {
                    source.sendError(GuildTexts.t("error.color_not_color"));
                    return 0;
                }
            }
            catch (IllegalArgumentException e) {
                source.sendError(GuildTexts.t("error.color_invalid_name"));
                return 0;
            }
            UUID playerId = player.getUuid();
            String playerName = player.getName().getString();
            if (GuildDataManager.getGuildByPlayer(playerId) != null) {
                source.sendError(GuildTexts.t("error.already_in_guild"));
                return 0;
            }
            if (GuildDataManager.isGuildNameTaken(name)) {
                source.sendError(GuildTexts.t("error.guild_name_taken"));
                return 0;
            }
            Guild guild = new Guild(UUID.randomUUID(), name, color.getName(), Instant.now(), playerId, playerName, false);
            guild.addMember(playerId, playerName, "Guild Master");
            GuildDataManager.registerGuild(guild);
            GuildDataManager.saveGuild(source.getServer(), guild);
            String guildShortId = guild.getShortenedId();
            if (GuildsConfig.enableGuildBank) {
                GuildBankManager.createBank(source.getServer(), guildShortId);
                System.out.println("[GuildBank] Created new bank for guild: " + guildShortId);
            }
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
            GuildTeamUtil.forceUpdateForPlayer(player);
            if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                GuildPrefixHelper.applyGuildPrefix(player);
            }
            if (GuildsConfig.enableHungerBoundIntegration) {
                GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(player);
            }
            source.sendFeedback(() -> GuildTexts.t("create.success_prefix").append(Text.literal(name).formatted(color)).append(GuildTexts.t("create.success_suffix")), false);
            return 1;
        }))))).then(((LiteralArgumentBuilder)CommandManager.literal((String)"info").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            GuildCommand.displayGuildInfo(source.getServer(), source, guild);
            return 1;
        })).then(CommandManager.argument((String)"name", (ArgumentType)StringArgumentType.string()).executes(ctx -> {
            ServerPlayerEntity player;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            String name = StringArgumentType.getString((CommandContext)ctx, (String)"name");
            Guild guild = GuildDataManager.getGuildByName(name);
            if (guild == null && (player = source.getServer().getPlayerManager().getPlayer(name)) != null) {
                guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            }
            if (guild == null) {
                source.sendError(GuildTexts.t("error.info_not_found"));
                return 0;
            }
            GuildCommand.displayGuildInfo(source.getServer(), source, guild);
            return 1;
        })))).then(CommandManager.literal((String)"disband").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!guild.getOwnerId().equals(player.getUuid())) {
                source.sendError(GuildTexts.t("error.only_owner_disband"));
                return 0;
            }
            MutableText yesButton = GuildTexts.t("common.button_yes").setStyle(Style.EMPTY.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild disbandconfirm")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, GuildTexts.t("disband.hover_yes"))));
            MutableText noButton = GuildTexts.t("common.button_no").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild cancel")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, GuildTexts.t("common.hover_cancel"))));
            player.sendMessage(GuildTexts.t("disband.confirm_question").formatted(Formatting.GOLD).append(yesButton).append(Text.literal(" ")).append(noButton), false);
            return 1;
        }))).then(CommandManager.literal((String)"disbandconfirm").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!guild.getOwnerId().equals(playerId)) {
                source.sendError(GuildTexts.t("error.only_owner_disband"));
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
                    allyPlayer.sendMessage(GuildTexts.t("disband.ally_notice_prefix").append(Text.literal(disbandedName).formatted(Formatting.YELLOW)).append(GuildTexts.t("disband.ally_notice_suffix")));
                }
            }
            GuildDataManager.removeAllInvitesToGuild(guild.getId());
            GuildDataManager.deleteGuild(server, guild.getId());
            String guildShortId = guild.getShortenedId();
            GuildBankManager.deleteBank(server, guildShortId);
            source.sendFeedback(() -> GuildTexts.t("disband.success_prefix").append(Text.literal(guild.getName()).formatted(Formatting.RED)).append(GuildTexts.t("disband.success_suffix")), false);
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
                source.sendError(GuildTexts.t("error.player_not_found"));
                System.out.println("[GuildsRemastered] Player not found: " + targetName);
                return 0;
            }
            UUID inviterId = inviter.getUuid();
            UUID targetId = target.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(inviterId);
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                System.out.println("[GuildsRemastered] Inviter not in a guild: " + inviter.getName().getString());
                return 0;
            }
            String inviterRank = guild.getRank(inviterId).toLowerCase();
            System.out.println("[GuildsRemastered] Inviter rank: " + inviterRank);
            if (!guild.hasRankPermission(inviterId, "canInvite")) {
                source.sendError(GuildTexts.t("error.no_invite_permission"));
                return 0;
            }
            if (GuildDataManager.getGuildByPlayer(targetId) != null) {
                source.sendError(GuildTexts.t("error.target_already_in_guild"));
                System.out.println("[GuildsRemastered] Target player is already in a guild: " + targetName);
                return 0;
            }
            GuildDataManager.addInvite(targetId, guild.getId());
            inviter.sendMessage(GuildTexts.t("invite.sent", target.getName().getString()).formatted(Formatting.GREEN));
            MutableText acceptButton = GuildTexts.t("invite.button_accept").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild accept")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, GuildTexts.t("invite.hover_accept"))));
            target.sendMessage(GuildTexts.t("invite.target_line_prefix").append(Text.literal(guild.getName()).formatted(Formatting.YELLOW)).append(GuildTexts.t("invite.target_line_suffix")).append(acceptButton), false);
            System.out.println("[GuildsRemastered] Invitation sent to " + targetName + " for guild " + guild.getName());
            return 1;
        })))).then(CommandManager.literal((String)"accept").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            UUID guildId = GuildDataManager.getInvite(playerId);
            System.out.println("[GuildsRemastered] Player " + player.getName().getString() + " is attempting to accept an invite.");
            if (guildId == null) {
                source.sendError(GuildTexts.t("error.no_pending_invite"));
                System.out.println("[GuildsRemastered] No pending invites for " + player.getName().getString());
                return 0;
            }
            if (GuildDataManager.getGuildByPlayer(playerId) != null) {
                source.sendError(GuildTexts.t("error.already_in_guild"));
                System.out.println("[GuildsRemastered] Player " + player.getName().getString() + " is already in a guild.");
                return 0;
            }
            Guild guild = GuildDataManager.getGuildById(guildId);
            if (guild == null) {
                source.sendError(GuildTexts.t("error.invite_guild_gone"));
                System.out.println("[GuildsRemastered] Guild no longer exists: " + String.valueOf(guildId));
                return 0;
            }
            guild.addMember(playerId, player.getName().getString(), "Initiate");
            GuildDataManager.removeInvite(playerId);
            GuildDataManager.saveGuild(source.getServer(), guild);
            String teamName = "guild_" + guild.getId().toString().substring(0, 8);
            source.getServer().getCommandManager().executeWithPrefix(source.getServer().getCommandSource(), "team join " + teamName + " " + player.getName().getString());
            GuildTeamUtil.forceUpdateForPlayer(player);
            if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                GuildPrefixHelper.applyGuildPrefix(player);
            }
            if (GuildsConfig.enableHungerBoundIntegration) {
                GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(player);
            }
            Objects.requireNonNull(player.getServer()).getPlayerManager().broadcast(GuildTexts.t("join.broadcast", player.getName().getString(), guild.getName()), false);
            source.sendFeedback(() -> GuildTexts.t("join.feedback_prefix").append(Text.literal(guild.getName()).formatted(Formatting.YELLOW)).append(GuildTexts.t("join.feedback_suffix")), false);
            if (guild.getMotd() != null && !guild.getMotd().isEmpty()) {
                player.sendMessage(GuildTexts.t("join.motd_label").append(Text.literal(guild.getMotd()).styled(style -> style.withColor(Formatting.YELLOW).withItalic(Boolean.valueOf(true)))), false);
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
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (guild.getOwnerId().equals(playerId)) {
                source.sendError(GuildTexts.t("error.quit_is_master"));
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
            player.sendMessage(GuildTexts.t("quit.self", guild.getName()).formatted(Formatting.GOLD));
            for (UUID memberId : guild.getMembers().keySet()) {
                ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                if (member == null) continue;
                member.sendMessage(GuildTexts.t("quit.broadcast", playerName).formatted(Formatting.GRAY));
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
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!guild.hasRankPermission(executor.getUuid(), "canKick")) {
                source.sendError(GuildTexts.t("error.no_kick_permission"));
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
                source.sendError(GuildTexts.t("error.kick_invalid_target"));
                return 0;
            }
            if (targetId.equals(guild.getOwnerId())) {
                source.sendError(GuildTexts.t("error.cannot_kick_master"));
                return 0;
            }
            guild.removeMember(targetId);
            GuildDataManager.saveGuild(server, guild);
            String kickedName = targetInfo.name;
            source.sendFeedback(() -> GuildTexts.t("kick.executor_feedback", kickedName), false);
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
                kicked.sendMessage(GuildTexts.t("kick.target_message", guild.getName()).formatted(Formatting.RED));
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), "team leave " + kickedName);
            }
            for (UUID memberId : guild.getMembers().keySet()) {
                ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                if (member == null) continue;
                member.sendMessage(GuildTexts.t("kick.broadcast", kickedName).formatted(Formatting.GRAY));
            }
            return 1;
        })))).then(CommandManager.literal((String)"chat").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            GuildDataManager.ChatStateManager.toggleGuildChat(playerId);
            boolean enabled = GuildDataManager.ChatStateManager.isGuildChatEnabled(playerId);
            if (enabled) {
                player.sendMessage(GuildTexts.t("chat.toggle_on_title").formatted(Formatting.GREEN).append(GuildTexts.t("chat.toggle_on_sub").formatted(Formatting.GRAY)), false);
            } else {
                player.sendMessage(GuildTexts.t("chat.toggle_off_title").formatted(Formatting.RED).append(GuildTexts.t("chat.toggle_off_sub").formatted(Formatting.GRAY)), false);
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
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!guild.hasRankPermission(executor.getUuid(), "canPromote")) {
                source.sendError(GuildTexts.t("error.no_promote_permission"));
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
                source.sendError(GuildTexts.t("error.promote_invalid_target"));
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
                source.sendError(GuildTexts.t("error.promote_max", targetInfo.name));
                return 0;
            }
            targetInfo.rank = newRank;
            GuildDataManager.saveGuild(server, guild);
            Guild.GuildMemberInfo finalTargetInfo = targetInfo;
            String promotedTo = newRank;
            source.sendFeedback(() -> GuildTexts.t("promote.success", finalTargetInfo.name, promotedTo), false);
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
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!guild.hasRankPermission(executor.getUuid(), "canDemote")) {
                source.sendError(GuildTexts.t("error.no_demote_permission"));
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
                source.sendError(GuildTexts.t("error.demote_invalid_target"));
                return 0;
            }
            if (targetId.equals(guild.getOwnerId())) {
                source.sendError(GuildTexts.t("error.cannot_demote_master"));
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
                source.sendError(GuildTexts.t("error.demote_min", targetInfo.name));
                return 0;
            }
            targetInfo.rank = newRank;
            GuildDataManager.saveGuild(server, guild);
            Guild.GuildMemberInfo finalTargetInfo = targetInfo;
            String demotedTo = newRank;
            source.sendFeedback(() -> GuildTexts.t("demote.success", finalTargetInfo.name, demotedTo), false);
            return 1;
        })))).then(CommandManager.literal((String)"toggle_friendlyfire").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!guild.hasRankPermission(playerId, "canTogglePvP")) {
                source.sendError(GuildTexts.t("error.ff_no_permission"));
                return 0;
            }
            String teamName = "guild_" + guild.getId().toString().substring(0, 8);
            ServerScoreboard scoreboard = player.getServer().getScoreboard();
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                source.sendError(GuildTexts.t("error.team_not_found"));
                return 0;
            }
            boolean current = team.isFriendlyFireAllowed();
            team.setFriendlyFireAllowed(!current);
            Formatting stateFmt = !current ? Formatting.GREEN : Formatting.RED;
            source.sendFeedback(() -> GuildTexts.t("friendly_fire.prefix").append(GuildTexts.t(!current ? "friendly_fire.enabled" : "friendly_fire.disabled").formatted(stateFmt)), true);
            return 1;
        }))).then(CommandManager.literal((String)"motd").then(CommandManager.argument((String)"message", (ArgumentType)StringArgumentType.greedyString()).executes(ctx -> {
            boolean isOfficer;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            Guild.GuildMemberInfo info = guild.getMembers().get(playerId);
            boolean bl = isOfficer = info != null && info.rank != null && info.rank.equalsIgnoreCase("officer");
            if (!guild.getOwnerId().equals(playerId) && !isOfficer) {
                source.sendError(GuildTexts.t("error.motd_no_permission"));
                return 0;
            }
            String motd = StringArgumentType.getString((CommandContext)ctx, (String)"message");
            guild.setMotd(motd);
            GuildDataManager.saveGuild(source.getServer(), guild);
            source.sendFeedback(() -> GuildTexts.t("motd.updated_prefix").append(Text.literal(motd).styled(style -> style.withColor(Formatting.YELLOW))), false);
            return 1;
        })))).then(CommandManager.literal((String)"rename").then(CommandManager.argument((String)"name", (ArgumentType)StringArgumentType.string()).then(CommandManager.argument((String)"color", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            Formatting color;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            String newName = StringArgumentType.getString((CommandContext)ctx, (String)"name");
            if (!GuildNameUtil.isWithinMaxLength(newName)) {
                source.sendError(GuildTexts.t("error.guild_name_too_long", GuildNameUtil.MAX_GUILD_NAME_CODE_POINTS));
                return 0;
            }
            String colorName = StringArgumentType.getString((CommandContext)ctx, (String)"color").toUpperCase();
            try {
                color = Formatting.valueOf((String)colorName);
                if (!color.isColor()) {
                    source.sendError(GuildTexts.t("error.color_not_color"));
                    return 0;
                }
            }
            catch (IllegalArgumentException e) {
                source.sendError(GuildTexts.t("error.color_invalid_name"));
                return 0;
            }
            UUID playerId = player.getUuid();
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!guild.getOwnerId().equals(playerId)) {
                source.sendError(GuildTexts.t("error.rename_not_owner"));
                return 0;
            }
            if (GuildDataManager.isGuildNameTaken(newName)) {
                source.sendError(GuildTexts.t("error.guild_name_taken"));
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
                    allyPlayer.sendMessage(GuildTexts.t("rename.ally_notice_prefix").append(Text.literal(oldName).formatted(Formatting.GREEN)).append(GuildTexts.t("rename.ally_notice_mid")).append(Text.literal(newName).formatted(Formatting.GREEN)).append(GuildTexts.t("rename.ally_notice_suffix")));
                }
            }
            String teamName = "guild_" + guild.getId().toString().substring(0, 8);
            ServerScoreboard scoreboard = server.getScoreboard();
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.setPrefix(Text.empty());
                team.setSuffix(GuildDisplayUtil.guildSuffixBracketText(guild));
                team.setDisplayName(Text.literal(guild.getName()));
                team.setColor(Formatting.WHITE);
                GuildTeamUtil.broadcastTeamUpdate(server, team);
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
            source.sendFeedback(() -> GuildTexts.t("rename.success_prefix").append(Text.literal(newName).formatted(color)).append(GuildTexts.t("rename.success_suffix")), false);
            return 1;
        }))))).then(CommandManager.literal((String)"sethome").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            if (!GuildsConfig.enableGuildHome) {
                source.sendError(GuildTexts.t("error.feature_guild_home_disabled"));
                return 0;
            }
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!guild.hasRankPermission(playerId, "canSetHome")) {
                source.sendError(GuildTexts.t("error.no_sethome_permission"));
                return 0;
            }
            Vec3d pos = player.getPos();
            String worldKey = player.getWorld().getRegistryKey().getValue().toString();
            guild.setHome(pos.getX(), pos.getY(), pos.getZ(), worldKey);
            GuildDataManager.saveGuild(source.getServer(), guild);
            source.sendFeedback(() -> GuildTexts.t("home.set_success"), false);
            return 1;
        }))).then(CommandManager.literal((String)"home").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            UUID playerId = player.getUuid();
            if (!GuildsConfig.enableGuildHome) {
                source.sendError(GuildTexts.t("error.feature_guild_home_disabled"));
                return 0;
            }
            Guild guild = GuildDataManager.getGuildByPlayer(playerId);
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!guild.hasRankPermission(playerId, "canUseHome")) {
                source.sendError(GuildTexts.t("error.no_home_permission"));
                return 0;
            }
            if (!guild.hasHome()) {
                source.sendError(GuildTexts.t("error.home_not_set"));
                return 0;
            }
            String worldName = guild.getHomeWorld();
            String[] parts = worldName.split(":");
            if (parts.length != 2) {
                source.sendError(GuildTexts.t("error.home_invalid_world", worldName));
                return 0;
            }
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(parts[0], parts[1]));
            ServerWorld world = source.getServer().getWorld(worldKey);
            if (world == null) {
                source.sendError(GuildTexts.t("error.home_world_missing"));
                return 0;
            }
            int delaySeconds = GuildsConfig.guildHomeTeleportDelaySeconds;
            int delayTicks = delaySeconds * 20;
            player.sendMessage(GuildTexts.t("home.teleport_pending", delaySeconds).formatted(Formatting.AQUA), false);
            DelayedTeleportScheduler.schedule(player, delayTicks, () -> {
                if (player.isAlive() && player.getWorld() != null) {
                    player.teleport(world, guild.getHomeX(), guild.getHomeY(), guild.getHomeZ(), player.getYaw(), player.getPitch());
                    player.sendMessage(GuildTexts.t("home.teleport_done").formatted(Formatting.GREEN), false);
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
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            String senderRank = senderGuild.getRank(sender.getUuid());
            if (!senderRank.equalsIgnoreCase("guild master")) {
                source.sendError(GuildTexts.t("error.ally_only_master"));
                return 0;
            }
            Guild targetGuild = GuildDataManager.getGuildByName(targetGuildName);
            if (targetGuild == null) {
                source.sendError(GuildTexts.t("error.ally_guild_not_found", targetGuildName));
                return 0;
            }
            if (senderGuild.getId().equals(targetGuild.getId())) {
                source.sendError(GuildTexts.t("error.ally_self"));
                return 0;
            }
            String targetShortId = "guild_" + targetGuild.getId().toString().substring(0, 8);
            if (senderGuild.getAllies().containsKey(targetShortId)) {
                MutableText yesButton = GuildTexts.t("common.button_yes").setStyle(Style.EMPTY.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild allyrevoke " + targetShortId)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, GuildTexts.t("ally.revoke_hover_yes"))));
                MutableText noButton = GuildTexts.t("common.button_no").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild cancel")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, GuildTexts.t("common.hover_cancel"))));
                sender.sendMessage(GuildTexts.t("ally.already_allied_prefix").append(Text.literal(targetGuild.getName()).formatted(Formatting.YELLOW)).append(GuildTexts.t("ally.already_allied_suffix")).append(yesButton).append(Text.literal(" ")).append(noButton), false);
                return 0;
            }
            ServerPlayerEntity targetMaster = server.getPlayerManager().getPlayer(targetGuild.getOwnerId());
            if (targetMaster == null) {
                source.sendError(GuildTexts.t("error.ally_target_master_offline", targetGuild.getName()));
                return 0;
            }
            GuildDataManager.addAllyRequest(targetGuild.getOwnerId(), senderGuild.getId());
            sender.sendMessage(GuildTexts.t("ally.request_sent", targetGuild.getName()).formatted(Formatting.GREEN), false);
            MutableText acceptButton = GuildTexts.t("ally.button_accept").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild allyaccept " + String.valueOf(senderGuild.getId()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, GuildTexts.t("ally.hover_accept"))));
            MutableText denyButton = GuildTexts.t("ally.button_deny").setStyle(Style.EMPTY.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild allydeny " + String.valueOf(senderGuild.getId()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, GuildTexts.t("ally.hover_deny"))));
            targetMaster.sendMessage(GuildTexts.t("ally.incoming_prefix").append(Text.literal(senderGuild.getName()).formatted(Formatting.YELLOW)).append(GuildTexts.t("ally.incoming_suffix")).append(acceptButton).append(Text.literal(" ")).append(denyButton), false);
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
                source.sendError(GuildTexts.t("error.invalid_guild_id"));
                return 0;
            }
            Guild receiverGuild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (receiverGuild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!receiverGuild.getOwnerId().equals(player.getUuid())) {
                source.sendError(GuildTexts.t("error.ally_accept_not_master"));
                return 0;
            }
            UUID expected = GuildDataManager.getAllyRequest(player.getUuid());
            if (!requesterGuildId.equals(expected)) {
                source.sendError(GuildTexts.t("error.ally_no_pending"));
                return 0;
            }
            Guild requesterGuild = GuildDataManager.getGuildById(requesterGuildId);
            if (requesterGuild == null) {
                source.sendError(GuildTexts.t("error.ally_requester_gone"));
                return 0;
            }
            receiverGuild.addAlly(requesterGuild.getId(), requesterGuild.getName());
            requesterGuild.addAlly(receiverGuild.getId(), receiverGuild.getName());
            AllyChatBridgeManager.createBridge(receiverGuild.getShortenedId(), requesterGuild.getShortenedId());
            GuildDataManager.removeAllyRequest(player.getUuid());
            GuildDataManager.saveGuild(server, receiverGuild);
            GuildDataManager.saveGuild(server, requesterGuild);
            player.sendMessage(GuildTexts.t("ally.accepted_receiver", requesterGuild.getName()).formatted(Formatting.GREEN), false);
            ServerPlayerEntity requesterMaster = server.getPlayerManager().getPlayer(requesterGuild.getOwnerId());
            if (requesterMaster != null) {
                requesterMaster.sendMessage(GuildTexts.t("ally.accepted_requester", receiverGuild.getName()).formatted(Formatting.GREEN), false);
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
                source.sendError(GuildTexts.t("error.invalid_guild_id"));
                return 0;
            }
            Guild receiverGuild = GuildDataManager.getGuildByPlayer(playerId);
            if (receiverGuild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!receiverGuild.getOwnerId().equals(playerId)) {
                source.sendError(GuildTexts.t("error.ally_deny_not_master"));
                return 0;
            }
            UUID expected = GuildDataManager.getAllyRequest(playerId);
            if (!requesterGuildId.equals(expected)) {
                source.sendError(GuildTexts.t("error.ally_deny_no_pending"));
                return 0;
            }
            Guild senderGuild = GuildDataManager.getGuildById(requesterGuildId);
            if (senderGuild == null) {
                source.sendError(GuildTexts.t("error.ally_guild_missing"));
                return 0;
            }
            GuildDataManager.removeAllyRequest(playerId);
            player.sendMessage(GuildTexts.t("ally.denied_receiver_prefix").append(Text.literal(senderGuild.getName()).formatted(Formatting.RED)));
            ServerPlayerEntity senderPlayer = server.getPlayerManager().getPlayer(senderGuild.getOwnerId());
            if (senderPlayer != null) {
                senderPlayer.sendMessage(GuildTexts.t("ally.denied_sender_prefix").append(Text.literal(receiverGuild.getName()).formatted(Formatting.RED)).append(GuildTexts.t("ally.denied_sender_suffix")));
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
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!selfGuild.getOwnerId().equals(playerId)) {
                source.sendError(GuildTexts.t("error.ally_revoke_not_master"));
                return 0;
            }
            if (!selfGuild.getAllies().containsKey(shortId)) {
                source.sendError(GuildTexts.t("error.ally_not_allied"));
                return 0;
            }
            Guild targetGuild = null;
            for (Guild g : GuildDataManager.getAllGuilds()) {
                if (!g.getShortenedId().equals(shortId)) continue;
                targetGuild = g;
                break;
            }
            if (targetGuild == null) {
                source.sendError(GuildTexts.t("error.ally_target_missing"));
                return 0;
            }
            selfGuild.getAllies().remove(shortId);
            String selfShortId = selfGuild.getShortenedId();
            targetGuild.getAllies().remove(selfShortId);
            GuildDataManager.saveGuild(server, selfGuild);
            GuildDataManager.saveGuild(server, targetGuild);
            AllyChatBridgeManager.removeBridge(selfShortId, shortId);
            player.sendMessage(GuildTexts.t("ally.revoked_self_prefix").append(Text.literal(targetGuild.getName()).formatted(Formatting.RED)));
            ServerPlayerEntity targetOwner = server.getPlayerManager().getPlayer(targetGuild.getOwnerId());
            if (targetOwner != null) {
                targetOwner.sendMessage(GuildTexts.t("ally.revoked_notice_target", selfGuild.getName()).formatted(Formatting.GRAY));
            }
            return 1;
        })))).then(CommandManager.literal((String)"bank").then(CommandManager.argument((String)"page", (ArgumentType)IntegerArgumentType.integer((int)1)).executes(context -> {
            int unlockedTabs;
            ServerCommandSource source = (ServerCommandSource)context.getSource();
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(GuildTexts.t("error.players_only_command"));
                return 0;
            }
            if (!GuildsConfig.enableGuildBank) {
                source.sendError(GuildTexts.t("error.feature_guild_bank_disabled"));
                return 0;
            }
            final Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild == null) {
                player.sendMessage(GuildTexts.t("error.bank_need_guild"), false);
                return 0;
            }
            final int requestedPage = IntegerArgumentType.getInteger((CommandContext)context, (String)"page");
            if (requestedPage > (unlockedTabs = guild.getUnlockedBankTabs())) {
                player.sendMessage(GuildTexts.t("bank.tab_locked", requestedPage).formatted(Formatting.RED), false);
                return 0;
            }
            String permissionKey = "canUseBankTab" + requestedPage;
            if (!guild.hasRankPermission(player.getUuid(), permissionKey)) {
                source.sendError(GuildTexts.t("error.bank_no_permission"));
                return 0;
            }
            final List<GuildBankInventory> guildBankPages = GuildBankManager.getOrLoadBankPages(player.getServer(), guild.getShortenedId());
            if (requestedPage < 1 || requestedPage > guildBankPages.size()) {
                player.sendMessage(GuildTexts.t("bank.invalid_page", guildBankPages.size()), false);
                return 0;
            }
            final int startingPageIndex = requestedPage - 1;
            player.openHandledScreen(new NamedScreenHandlerFactory(){

                public Text getDisplayName() {
                    return GuildTexts.t("bank.screen_title", guild.getName(), requestedPage);
                }

                public GuildBankScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                    return new GuildBankScreenHandler(syncId, playerInventory, guildBankPages, startingPageIndex);
                }
            });
            player.sendMessage(GuildTexts.t("bank.opened_page", requestedPage), false);
            return 1;
        })))).then(CommandManager.literal((String)"permissions").executes(ctx -> {
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild == null) {
                System.out.println("[GuildsRemastered] /guild permissions failed: player not in a guild (" + player.getName().getString() + ")");
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            if (!guild.getOwnerId().equals(player.getUuid())) {
                System.out.println("[GuildsRemastered] /guild permissions failed: player not guild master (" + player.getName().getString() + ")");
                source.sendError(GuildTexts.t("error.permissions_not_master"));
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
                source.sendError(GuildTexts.t("error.permissions_open_failed"));
                return 0;
            }
        }))).then(CommandManager.literal((String)"donate").executes(ctx -> {
            int currentProgress;
            ServerCommandSource source = (ServerCommandSource)ctx.getSource();
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            if (!GuildsConfig.enableGuildBank) {
                source.sendError(GuildTexts.t("error.feature_guild_bank_disabled"));
                return 0;
            }
            Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild").formatted(Formatting.RED));
                return 0;
            }
            if (!guild.hasRankPermission(player.getUuid(), "canDonate")) {
                source.sendError(GuildTexts.t("error.donate_no_permission").formatted(Formatting.RED));
                return 0;
            }
            ItemStack hand = player.getMainHandStack();
            if (!hand.isOf(Items.NETHERITE_INGOT)) {
                source.sendError(GuildTexts.t("error.donate_need_ingot").formatted(Formatting.RED));
                return 0;
            }
            int held = hand.getCount();
            int nextTab = guild.getUnlockedBankTabs() + 1;
            if (nextTab > 9) {
                source.sendError(GuildTexts.t("error.donate_all_unlocked").formatted(Formatting.YELLOW));
                return 0;
            }
            int cost = GuildCommand.getRequiredNetheriteForTab(nextTab);
            int needed = cost - (currentProgress = guild.getBankUnlockProgress(nextTab));
            if (needed <= 0) {
                source.sendError(GuildTexts.t("error.donate_tab_ready", nextTab).formatted(Formatting.GREEN));
                return 0;
            }
            int toDeposit = Math.min(held, needed);
            guild.addBankUnlockProgress(nextTab, toDeposit);
            hand.decrement(toDeposit);
            player.setStackInHand(player.getActiveHand(), hand);
            GuildDataManager.saveGuild(server, guild);
            player.sendMessage(GuildTexts.t("donate.progress_message", toDeposit, nextTab).formatted(Formatting.GRAY), false);
            int newProgress = guild.getBankUnlockProgress(nextTab);
            if (newProgress >= cost) {
                guild.setUnlockedBankTabs(nextTab);
                GuildDataManager.saveGuild(server, guild);
                for (UUID memberId : guild.getMembers().keySet()) {
                    ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                    if (member == null) continue;
                    member.sendMessage(GuildTexts.t("donate.tab_unlocked_broadcast", nextTab).formatted(Formatting.GREEN));
                }
            }
            return 1;
        }))).then(CommandManager.literal((String)"progress").executes(context -> {
            ServerCommandSource source = (ServerCommandSource)context.getSource();
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(GuildTexts.t("error.players_only_progress"));
                return 0;
            }
            if (!GuildsConfig.enableGuildBank) {
                source.sendError(GuildTexts.t("error.feature_guild_bank_disabled"));
                return 0;
            }
            Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild == null) {
                source.sendError(GuildTexts.t("error.not_in_guild"));
                return 0;
            }
            int unlocked = guild.getUnlockedBankTabs();
            if (unlocked >= 9) {
                player.sendMessage(GuildTexts.t("bank.progress_header_gold").formatted(Formatting.GOLD).append(GuildTexts.t("bank.progress_all_done").formatted(Formatting.GREEN)), false);
                return 1;
            }
            int nextTab = unlocked + 1;
            int required = guild.getRequiredNetheriteForTab(nextTab);
            int progress = guild.getNetheriteDonatedForTab(nextTab);
            int remaining = Math.max(0, required - progress);
            player.sendMessage(GuildTexts.t("bank.progress_header_gold").formatted(Formatting.GOLD).append(GuildTexts.t("bank.progress_line_require", nextTab, required).formatted(Formatting.YELLOW)).append(GuildTexts.t("bank.progress_line_numbers", progress, required).formatted(Formatting.GRAY)).append(GuildTexts.t("bank.progress_line_remaining", remaining).formatted(Formatting.GREEN)), false);
            return 1;
        }))).then(CommandManager.literal((String)"unlock").executes(context -> {
            ServerCommandSource source = (ServerCommandSource)context.getSource();
            ServerPlayerEntity executor = source.getPlayer();
            if (!GuildsConfig.enableGuildBank) {
                source.sendError(GuildTexts.t("error.feature_guild_bank_disabled"));
                return 0;
            }
            Guild guild = GuildDataManager.getGuildByPlayer(executor.getUuid());
            if (guild == null) {
                source.sendError(GuildTexts.t("error.unlock_need_guild"));
                return 0;
            }
            int nextTab = guild.getUnlockedBankTabs() + 1;
            int requiredNetherite = guild.getRequiredNetheriteForTab(nextTab);
            int currentDonated = guild.getBankUnlockProgress(nextTab);
            if (currentDonated < requiredNetherite) {
                int remainingAmount = requiredNetherite - currentDonated;
                source.sendError(GuildTexts.t("error.unlock_need_more", remainingAmount, nextTab));
                return 0;
            }
            guild.setUnlockedBankTabs(nextTab);
            source.sendFeedback(() -> GuildTexts.t("unlock.success", nextTab), false);
            return 1;
        }))).then(CommandManager.literal((String)"cancel").executes(ctx -> {
            ((ServerCommandSource)ctx.getSource()).getPlayer().sendMessage(GuildTexts.t("common.action_cancelled").formatted(Formatting.GRAY));
            return 1;
        })));
    }

    public static int getRequiredNetheriteForTab(int nextTab) {
        int baseCost = 8;
        return baseCost * nextTab;
    }

    private static void displayGuildInfo(MinecraftServer server, ServerCommandSource source, Guild guild) {
        Formatting guildColor = GuildColorUtil.getFormatting(guild.getColor());
        source.sendMessage(GuildTexts.t("info.guild_label").append(Text.literal(guild.getName()).formatted(guildColor)));
        if (guild.getMotd() != null && !guild.getMotd().isEmpty()) {
            source.sendMessage(GuildTexts.t("info.motd_label").append(Text.literal(guild.getMotd()).styled(style -> style.withColor(guildColor).withItalic(Boolean.valueOf(true)))));
        }
        if (GuildsConfig.enableGuildHome) {
            boolean hasHome = guild.hasHome();
            MutableText homeLine = GuildTexts.t("info.home_set_label").formatted(Formatting.AQUA);
            homeLine.append(Text.literal(String.valueOf(hasHome)).formatted(hasHome ? Formatting.GREEN : Formatting.RED));
            source.sendMessage((Text)homeLine);
        }
        source.sendMessage(GuildTexts.t("info.created_label").append(Text.literal(guild.getFormattedDate()).formatted(Formatting.WHITE)));
        source.sendMessage(GuildTexts.t("info.master_label").append(Text.literal(guild.getOwnerName()).formatted(Formatting.AQUA)));
        source.sendMessage(GuildTexts.t("info.members_header", guild.getMembers().size()));
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
            source.sendMessage(GuildTexts.t("info.online_header").formatted(Formatting.GREEN));
            for (String string : online) {
                source.sendMessage(Text.literal(string).formatted(Formatting.GREEN));
            }
        }
        if (!offline.isEmpty()) {
            source.sendMessage(GuildTexts.t("info.offline_header").formatted(Formatting.RED));
            for (String string : offline) {
                source.sendMessage(Text.literal(string).formatted(Formatting.RED));
            }
        }
        if (!guild.getAllies().isEmpty()) {
            Collection<Guild.AllyInfo> allyInfos = guild.getAllies().values();
            MutableText alliesLine = GuildTexts.t("info.allies_header").formatted(Formatting.AQUA);
            boolean first = true;
            for (Guild.AllyInfo ally : allyInfos) {
                if (!first) {
                    alliesLine.append(Text.literal(", ").formatted(Formatting.GRAY));
                }
                MutableText nameText = Text.literal(ally.name).formatted(Formatting.GREEN);
                nameText = ally.pvpDisabled ? nameText.append(GuildTexts.t("info.ally_pvp_off").formatted(Formatting.RED)) : nameText.append(GuildTexts.t("info.ally_pvp_on").formatted(Formatting.DARK_GREEN));
                alliesLine.append((Text)nameText);
                first = false;
            }
            source.sendMessage((Text)alliesLine);
        } else {
            MutableText noAllies = Text.empty().append(GuildTexts.t("info.allies_header").formatted(Formatting.AQUA)).append(GuildTexts.t("info.allies_none").formatted(Formatting.RED));
            source.sendMessage((Text)noAllies);
        }
    }
}

