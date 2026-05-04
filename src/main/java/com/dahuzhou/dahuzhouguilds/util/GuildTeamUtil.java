package com.dahuzhou.dahuzhouguilds.util;

import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GuildTeamUtil {
	public static void updateTeamForPlayer(ServerPlayerEntity player, MinecraftServer server) {
		String playerName = player.getGameProfile().getName();
		Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
		ServerScoreboard scoreboard = server.getScoreboard();
		for (Team existing : scoreboard.getTeams()) {
			existing.getPlayerList().remove(playerName);
		}
		if (guild == null) {
			return;
		}
		String teamName = "guild_" + guild.getId().toString().substring(0, 8);
		Team team = scoreboard.getTeam(teamName);
		if (team == null) {
			team = scoreboard.addTeam(teamName);
		}
		team.setPrefix(Text.empty());
		team.setSuffix(GuildDisplayUtil.guildSuffixBracketText(guild));
		team.setDisplayName(Text.literal(guild.getName()));
		team.setColor(Formatting.WHITE);
		team.getPlayerList().add(playerName);
		player.networkHandler.sendPacket(TeamS2CPacket.updateTeam(team, true));
		server.getPlayerManager().sendToAll(new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME), Collections.singletonList(player)));
	}

	public static void updateTeamPrefixes(MinecraftServer server) {
		for (Guild guild : GuildDataManager.getAllGuilds()) {
			String teamName = "guild_" + guild.getId().toString().substring(0, 8);
			Team team = server.getScoreboard().getTeam(teamName);
			if (team == null) {
				continue;
			}
			team.setPrefix(Text.empty());
			team.setSuffix(GuildDisplayUtil.guildSuffixBracketText(guild));
			team.setDisplayName(Text.literal(guild.getName()));
			team.setColor(Formatting.WHITE);
			GuildTeamUtil.broadcastTeamUpdate(server, team);
		}
	}

	/** 将队伍前缀/后缀变更同步给所有在线客户端（改名等场景需要）。 */
	public static void broadcastTeamUpdate(MinecraftServer server, Team team) {
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			p.networkHandler.sendPacket(TeamS2CPacket.updateTeam(team, true));
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
	}

	public static void clearTeamForPlayer(ServerPlayerEntity player, MinecraftServer server) {
		GuildTeamUtil.removePlayerFromTeam(player, server);
	}

	public static void applyGuildData(MinecraftServer server, ServerPlayerEntity player, UUID guildId) {
		Guild guild = GuildDataManager.loadGuild(server, guildId);
		if (guild == null) {
			return;
		}
		String teamName = "guild_" + guild.getId().toString().substring(0, 8);
		Team team = server.getScoreboard().getTeam(teamName);
		if (team != null) {
			team.setPrefix(Text.empty());
			team.setSuffix(GuildDisplayUtil.guildSuffixBracketText(guild));
			team.setDisplayName(Text.literal(guild.getName()));
			team.setColor(Formatting.WHITE);
			server.getPlayerManager().sendToAll(new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME), Collections.singletonList(player)));
		}
	}
}
