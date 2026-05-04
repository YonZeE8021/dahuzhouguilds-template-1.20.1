/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Text
 *  net.minecraft.Packet
 *  net.minecraft.Team
 *  net.minecraft.PlayerListS2CPacket
 *  net.minecraft.PlayerListS2CPacket$class_5893
 *  net.minecraft.ServerScoreboard
 *  net.minecraft.ServerPlayerEntity
 *  net.minecraft.MutableText
 *  net.minecraft.TeamS2CPacket
 *  net.minecraft.server.MinecraftServer
 */
package net.demoniconpc.guildsremasteredmod.util;

import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;
import net.demoniconpc.guildsremasteredmod.data.GuildDataManager;
import net.demoniconpc.guildsremasteredmod.guild.Guild;
import net.demoniconpc.guildsremasteredmod.util.GuildColorUtil;
import net.minecraft.text.Text;
import net.minecraft.network.packet.Packet;
import net.minecraft.scoreboard.Team;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.server.MinecraftServer;

public class GuildTeamUtil {
    public static void updateTeamForPlayer(ServerPlayerEntity player, MinecraftServer server) {
        String guildTag;
        Team team2;
        String playerName = player.getGameProfile().getName();
        System.out.println("[GuildTeamUtil] Starting team update for player: " + playerName);
        Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
        if (guild != null) {
            System.out.println("[GuildTeamUtil] Player " + playerName + " is in the guild: " + guild.getName());
        } else {
            System.out.println("[GuildTeamUtil] Player " + playerName + " is not in any guild.");
        }
        ServerScoreboard scoreboard = server.getScoreboard();
        for (Team team2 : scoreboard.getTeams()) {
            team2.getPlayerList().remove(playerName);
        }
        if (guild == null) {
            System.out.println("[GuildTeamUtil] Player " + playerName + " is not in a guild, skipping team update.");
            return;
        }
        String teamName = "guild_" + guild.getId().toString().substring(0, 8);
        team2 = scoreboard.getTeam(teamName);
        if (team2 == null) {
            team2 = scoreboard.addTeam(teamName);
        }
        if ((guildTag = guild.getName()).length() > 16) {
            guildTag = guildTag.substring(0, 16);
        }
        String guildColor = guild.getColor();
        MutableText prefix = Text.literal((String)("[" + guildTag + "] ")).formatted(GuildColorUtil.getFormatting(guildColor));
        team2.setDisplayName((Text)prefix.copy());
        team2.setPrefix((Text)prefix.copy());
        team2.getPlayerList().add(playerName);
        player.networkHandler.sendPacket((Packet)TeamS2CPacket.updateTeam((Team)team2, (boolean)true));
        server.getPlayerManager().sendToAll((Packet)new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.class_PlayerListS2CPacket$Action.field_29139), Collections.singletonList(player)));
        System.out.println("[GuildTeamUtil] Team update complete for player: " + playerName);
    }

    public static void updateTeamPrefixes(MinecraftServer server) {
        for (Guild guild : GuildDataManager.getAllGuilds()) {
            String teamName = "guild_" + guild.getId().toString().substring(0, 8);
            Team team = server.getScoreboard().getTeam(teamName);
            if (team == null) continue;
            String tag = guild.getName();
            MutableText prefix = Text.literal((String)("[" + tag + "] ")).formatted(GuildColorUtil.getFormatting(guild.getColor()));
            team.setDisplayName((Text)prefix.copy());
            team.setPrefix((Text)prefix.copy());
            System.out.println("[GuildTeamUtil] Updated team prefix for guild: " + guild.getName() + " with tag: " + tag);
        }
    }

    public static void forceUpdateForPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        GuildTeamUtil.updateTeamForPlayer(player, server);
    }

    public static void removePlayerFromTeam(ServerPlayerEntity player, MinecraftServer server) {
        String playerName = player.getGameProfile().getName();
        ServerScoreboard scoreboard = server.getScoreboard();
        for (Team team : scoreboard.getTeams()) {
            team.getPlayerList().remove(playerName);
        }
        System.out.println("[GuildTeamUtil] Removed player " + playerName + " from all teams.");
    }

    public static void clearTeamForPlayer(ServerPlayerEntity player, MinecraftServer server) {
        GuildTeamUtil.removePlayerFromTeam(player, server);
    }

    public static void applyGuildData(MinecraftServer server, ServerPlayerEntity player, UUID guildId) {
        Guild guild = GuildDataManager.loadGuild(server, guildId);
        if (guild == null) {
            System.out.println("[GuildTeamUtil] Guild not found for player " + player.getGameProfile().getName());
            return;
        }
        String teamName = "guild_" + guild.getId().toString().substring(0, 8);
        Team team = server.getScoreboard().getTeam(teamName);
        if (team != null) {
            MutableText prefix = Text.literal((String)("[" + guild.getName() + "] ")).formatted(GuildColorUtil.getFormatting(guild.getColor()));
            team.setPrefix((Text)prefix);
            team.setDisplayName((Text)prefix);
            server.getPlayerManager().sendToAll((Packet)new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.class_PlayerListS2CPacket$Action.field_29139), Collections.singletonList(player)));
        }
        System.out.println("[GuildTeamUtil] Applied guild data for player " + player.getGameProfile().getName());
    }
}

