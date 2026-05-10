package com.dahuzhou.dahuzhouguilds.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Map;
import java.util.UUID;
import com.dahuzhou.dahuzhouguilds.GuildTexts;
import com.dahuzhou.dahuzhouguilds.GuildsConfig;
import com.dahuzhou.dahuzhouguilds.data.GuildBankManager;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.AllyChatBridgeManager;
import com.dahuzhou.dahuzhouguilds.util.GuildHungerBoundPrefixHelper;
import com.dahuzhou.dahuzhouguilds.util.GuildPrefixHelper;
import com.dahuzhou.dahuzhouguilds.util.GuildTeamUtil;
import net.minecraft.util.Formatting;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

public final class GuildAdminCommand {
    private static final int OP_LEVEL = 2;

    private GuildAdminCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("guildadmin")
                .requires(s -> s.hasPermissionLevel(OP_LEVEL))
                .then(CommandManager.literal("reload").executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    GuildDataManager.loadAllGuilds(source.getServer());
                    source.sendFeedback(() -> GuildTexts.t("admin.reload_done"), true);
                    return 1;
                }))
                .then(CommandManager.literal("list").executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    int n = 0;
                    for (Guild g : GuildDataManager.getAllGuilds()) {
                        source.sendMessage(GuildTexts.t("admin.list_line", g.getName(), g.getId().toString()).formatted(Formatting.GRAY));
                        ++n;
                    }
                    final int total = n;
                    source.sendFeedback(() -> GuildTexts.t("admin.list_footer", total), true);
                    return 1;
                }))
                .then(CommandManager.literal("info").then(CommandManager.argument("guildId", StringArgumentType.word()).executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    Guild g = resolveGuild(ctx, "guildId");
                    if (g == null) {
                        return 0;
                    }
                    GuildCommand.displayGuildInfo(source.getServer(), source, g);
                    return 1;
                })))
                .then(CommandManager.literal("disband").then(CommandManager.argument("guildId", StringArgumentType.word()).executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    MinecraftServer server = source.getServer();
                    Guild g = resolveGuild(ctx, "guildId");
                    if (g == null) {
                        return 0;
                    }
                    final String name = g.getName();
                    GuildCommand.disbandGuildCompletely(server, g);
                    source.sendFeedback(() -> GuildTexts.t("admin.disband_done", name), true);
                    return 1;
                })))
                .then(CommandManager.literal("kick").then(CommandManager.argument("guildId", StringArgumentType.word()).then(CommandManager.argument("player", StringArgumentType.word()).executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    MinecraftServer server = source.getServer();
                    Guild guild = resolveGuild(ctx, "guildId");
                    if (guild == null) {
                        return 0;
                    }
                    String targetName = StringArgumentType.getString(ctx, "player");
                    UUID targetId = null;
                    Guild.GuildMemberInfo targetInfo = null;
                    for (Map.Entry<UUID, Guild.GuildMemberInfo> e : guild.getMembers().entrySet()) {
                        if (e.getValue().name.equalsIgnoreCase(targetName)) {
                            targetId = e.getKey();
                            targetInfo = e.getValue();
                            break;
                        }
                    }
                    if (targetId == null) {
                        source.sendError(GuildTexts.t("admin.kick_not_member"));
                        return 0;
                    }
                    if (targetId.equals(guild.getOwnerId())) {
                        source.sendError(GuildTexts.t("admin.kick_is_owner"));
                        return 0;
                    }
                    guild.removeMember(targetId);
                    GuildDataManager.saveGuild(server, guild);
                    String kickedName = targetInfo.name;
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
                        server.getCommandManager().executeWithPrefix(server.getCommandSource(), "team leave " + kickedName);
                    }
                    source.sendFeedback(() -> GuildTexts.t("admin.kick_done", kickedName, guild.getName()), true);
                    return 1;
                }))))
                .then(CommandManager.literal("transfer").then(CommandManager.argument("guildId", StringArgumentType.word()).then(CommandManager.argument("player", StringArgumentType.word()).executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    MinecraftServer server = source.getServer();
                    Guild guild = resolveGuild(ctx, "guildId");
                    if (guild == null) {
                        return 0;
                    }
                    String targetName = StringArgumentType.getString(ctx, "player");
                    UUID newOwnerId = null;
                    Guild.GuildMemberInfo newOwnerInfo = null;
                    for (Map.Entry<UUID, Guild.GuildMemberInfo> e : guild.getMembers().entrySet()) {
                        if (e.getValue().name.equalsIgnoreCase(targetName)) {
                            newOwnerId = e.getKey();
                            newOwnerInfo = e.getValue();
                            break;
                        }
                    }
                    if (newOwnerId == null) {
                        source.sendError(GuildTexts.t("admin.transfer_not_member"));
                        return 0;
                    }
                    UUID oldOwnerId = guild.getOwnerId();
                    if (newOwnerId.equals(oldOwnerId)) {
                        source.sendError(GuildTexts.t("admin.transfer_already_owner"));
                        return 0;
                    }
                    Guild.GuildMemberInfo oldMasterMember = guild.getMembers().get(oldOwnerId);
                    if (oldMasterMember != null) {
                        oldMasterMember.rank = "Officer";
                    }
                    newOwnerInfo.rank = "Guild Master";
                    guild.setOwner(newOwnerId, newOwnerInfo.name);
                    GuildDataManager.saveGuild(server, guild);
                    final String newMasterName = newOwnerInfo.name;
                    final String guildName = guild.getName();
                    ServerPlayerEntity newMasterPlayer = server.getPlayerManager().getPlayer(newOwnerId);
                    if (newMasterPlayer != null) {
                        if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                            GuildPrefixHelper.applyGuildPrefix(newMasterPlayer);
                        }
                        if (GuildsConfig.enableHungerBoundIntegration) {
                            GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(newMasterPlayer);
                        }
                        GuildTeamUtil.forceUpdateForPlayer(newMasterPlayer);
                    }
                    ServerPlayerEntity oldMasterPlayer = server.getPlayerManager().getPlayer(oldOwnerId);
                    if (oldMasterPlayer != null) {
                        if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                            GuildPrefixHelper.applyGuildPrefix(oldMasterPlayer);
                        }
                        if (GuildsConfig.enableHungerBoundIntegration) {
                            GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(oldMasterPlayer);
                        }
                        GuildTeamUtil.forceUpdateForPlayer(oldMasterPlayer);
                    }
                    source.sendFeedback(() -> GuildTexts.t("admin.transfer_done", guildName, newMasterName), true);
                    return 1;
                }))))
                .then(CommandManager.literal("addmember").then(CommandManager.argument("guildId", StringArgumentType.word()).then(CommandManager.argument("player", StringArgumentType.word()).executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    MinecraftServer server = source.getServer();
                    Guild guild = resolveGuild(ctx, "guildId");
                    if (guild == null) {
                        return 0;
                    }
                    String targetName = StringArgumentType.getString(ctx, "player");
                    ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
                    if (target == null) {
                        source.sendError(GuildTexts.t("error.player_not_found"));
                        return 0;
                    }
                    UUID tid = target.getUuid();
                    if (GuildDataManager.getGuildByPlayer(tid) != null) {
                        source.sendError(GuildTexts.t("admin.addmember_already_in_guild"));
                        return 0;
                    }
                    if (guild.isMember(tid)) {
                        source.sendError(GuildTexts.t("admin.addmember_already_in_this_guild"));
                        return 0;
                    }
                    guild.addMember(tid, target.getName().getString(), "Initiate");
                    GuildDataManager.saveGuild(server, guild);
                    String teamName = "guild_" + guild.getId().toString().substring(0, 8);
                    server.getCommandManager().executeWithPrefix(server.getCommandSource(), "team join " + teamName + " " + target.getName().getString());
                    GuildTeamUtil.forceUpdateForPlayer(target);
                    if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                        GuildPrefixHelper.applyGuildPrefix(target);
                    }
                    if (GuildsConfig.enableHungerBoundIntegration) {
                        GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(target);
                    }
                    source.sendFeedback(() -> GuildTexts.t("admin.addmember_done", target.getName().getString(), guild.getName()), true);
                    return 1;
                }))))
                .then(CommandManager.literal("removeally").then(CommandManager.argument("guildId", StringArgumentType.word()).then(CommandManager.argument("allyGuildId", StringArgumentType.word()).executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    MinecraftServer server = source.getServer();
                    Guild a = resolveGuild(ctx, "guildId");
                    if (a == null) {
                        return 0;
                    }
                    String allyRaw = StringArgumentType.getString(ctx, "allyGuildId");
                    Guild b = GuildDataManager.getGuildByIdInput(allyRaw);
                    if (b == null) {
                        source.sendError(GuildTexts.t("error.guild_id_not_found"));
                        return 0;
                    }
                    if (a.getId().equals(b.getId())) {
                        source.sendError(GuildTexts.t("admin.removeally_same"));
                        return 0;
                    }
                    String shortA = a.getShortenedId();
                    String shortB = b.getShortenedId();
                    a.getAllies().remove(shortB);
                    b.getAllies().remove(shortA);
                    GuildDataManager.saveGuild(server, a);
                    GuildDataManager.saveGuild(server, b);
                    AllyChatBridgeManager.removeBridge(shortA, shortB);
                    source.sendFeedback(() -> GuildTexts.t("admin.removeally_done", a.getName(), b.getName()), true);
                    return 1;
                }))))
                .then(CommandManager.literal("clearinvites").executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    GuildDataManager.clearAllInvites();
                    source.sendFeedback(() -> GuildTexts.t("admin.clearinvites_all"), true);
                    return 1;
                }).then(CommandManager.argument("guildId", StringArgumentType.word()).executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    Guild g = resolveGuild(ctx, "guildId");
                    if (g == null) {
                        return 0;
                    }
                    GuildDataManager.removeAllInvitesToGuild(g.getId());
                    source.sendFeedback(() -> GuildTexts.t("admin.clearinvites_guild", g.getName()), true);
                    return 1;
                })))
                .then(CommandManager.literal("clearallyrequests").executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    GuildDataManager.clearAllPendingAllyRequests();
                    source.sendFeedback(() -> GuildTexts.t("admin.clearallyrequests_done"), true);
                    return 1;
                }))
                .then(CommandManager.literal("saveall").executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    MinecraftServer server = source.getServer();
                    int n = 0;
                    for (Guild g : GuildDataManager.getAllGuilds()) {
                        GuildDataManager.saveGuild(server, g);
                        ++n;
                    }
                    final int total = n;
                    source.sendFeedback(() -> GuildTexts.t("admin.saveall_done", total), true);
                    return 1;
                }))
                .then(CommandManager.literal("setmotd").then(CommandManager.argument("guildId", StringArgumentType.word()).then(CommandManager.argument("message", StringArgumentType.greedyString()).executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    MinecraftServer server = source.getServer();
                    Guild g = resolveGuild(ctx, "guildId");
                    if (g == null) {
                        return 0;
                    }
                    String motd = StringArgumentType.getString(ctx, "message");
                    g.setMotd(motd);
                    GuildDataManager.saveGuild(server, g);
                    source.sendFeedback(() -> GuildTexts.t("admin.setmotd_done", g.getName()), true);
                    return 1;
                }))))
                .then(CommandManager.literal("deletebank").then(CommandManager.argument("guildId", StringArgumentType.word()).executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    MinecraftServer server = source.getServer();
                    Guild g = resolveGuild(ctx, "guildId");
                    if (g == null) {
                        return 0;
                    }
                    GuildBankManager.deleteBank(server, g.getShortenedId());
                    source.sendFeedback(() -> GuildTexts.t("admin.deletebank_done", g.getName()), true);
                    return 1;
                }))));
    }

    private static Guild resolveGuild(CommandContext<ServerCommandSource> ctx, String arg) {
        ServerCommandSource source = ctx.getSource();
        String raw = StringArgumentType.getString(ctx, arg);
        Guild g = GuildDataManager.getGuildByIdInput(raw);
        if (g == null) {
            source.sendError(GuildTexts.t("error.guild_id_not_found"));
        }
        return g;
    }
}
