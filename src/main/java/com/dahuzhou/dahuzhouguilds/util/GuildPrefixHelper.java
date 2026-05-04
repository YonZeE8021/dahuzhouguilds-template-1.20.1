package com.dahuzhou.dahuzhouguilds.util;

import com.dahuzhou.dahuzhouguilds.GuildsConfig;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class GuildPrefixHelper {
	public static void applyGuildPrefix(ServerPlayerEntity player) {
		if (!GuildsConfig.enableEssentialsCommandGuildPrefix) {
			return;
		}
		Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
		if (guild == null) {
			return;
		}
		String playerName = player.getName().getString();
		String json = GuildDisplayUtil.essentialsNicknameJson(playerName, guild);
		MinecraftServer server = player.getServer();
		server.execute(() -> {
			server.getCommandManager().executeWithPrefix(server.getCommandSource().withLevel(4), "/nickname set " + playerName + " " + json);
		});
	}

	public static void clearNickname(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		String playerName = player.getName().getString();
		server.execute(() -> {
			server.getCommandManager().executeWithPrefix(server.getCommandSource().withLevel(4),
					"/nickname set " + playerName + " {\"text\":\"" + GuildDisplayUtil.jsonEscape(playerName) + "\"}");
		});
	}
}
