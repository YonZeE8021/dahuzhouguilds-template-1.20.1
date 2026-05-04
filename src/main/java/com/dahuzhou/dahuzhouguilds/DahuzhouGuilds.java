package com.dahuzhou.dahuzhouguilds;

import com.dahuzhou.dahuzhouguilds.GuildTexts;
import com.dahuzhou.dahuzhouguilds.commands.AllyCommand;
import com.dahuzhou.dahuzhouguilds.commands.GuildCommand;
import com.dahuzhou.dahuzhouguilds.data.GuildBankManager;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.AllyChatBridgeManager;
import com.dahuzhou.dahuzhouguilds.util.DamageHandler;
import com.dahuzhou.dahuzhouguilds.util.DelayedTeleportScheduler;
import com.dahuzhou.dahuzhouguilds.util.GuildBankInventory;
import com.dahuzhou.dahuzhouguilds.util.GuildHungerBoundPrefixHelper;
import com.dahuzhou.dahuzhouguilds.util.GuildPrefixHelper;
import com.mojang.brigadier.CommandDispatcher;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DahuzhouGuilds implements ModInitializer {
	public static final String MOD_ID = "dahuzhouguilds";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		GuildsConfig.load();
		DamageHandler.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				GuildCommand.register((CommandDispatcher<ServerCommandSource>) dispatcher));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				AllyCommand.register((CommandDispatcher<ServerCommandSource>) dispatcher));
		ServerTickEvents.END_SERVER_TICK.register(DelayedTeleportScheduler::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
			if (guild != null) {
				String motd = guild.getMotd();
				if (motd != null && !motd.isEmpty()) {
					player.sendMessage(
							GuildTexts.t("join.guild_motd_broadcast_prefix").append(Text.literal(motd).styled(style -> style.withColor(Formatting.YELLOW))),
							false);
				}
				String teamName = "guild_" + guild.getId().toString().substring(0, 8);
				ServerScoreboard scoreboard = server.getScoreboard();
				Team team = scoreboard.getTeam(teamName);
				String guildShortId = guild.getId().toString().substring(0, 8);
				Path bankFilePath = GuildBankManager.getBankFilePath(server, guildShortId);
				if (!Files.exists(bankFilePath)) {
					GuildBankManager.createBank(server, guildShortId);
					LOGGER.info("[GuildBank] Created new bank for guild: {}", guildShortId);
				}
				if (team != null) {
					if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
						GuildPrefixHelper.applyGuildPrefix(player);
					}
					if (GuildsConfig.enableHungerBoundIntegration) {
						GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(player);
					}
				} else {
					LOGGER.warn("[{}] Team {} not found for guild {}", MOD_ID, teamName, guild.getName());
				}
			} else {
				if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
					GuildPrefixHelper.clearNickname(player);
				}
				if (GuildsConfig.enableHungerBoundIntegration) {
					GuildHungerBoundPrefixHelper.clearNickname(player);
				}
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.player;
			if (player == null) {
				return;
			}
			Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
			if (guild != null) {
				GuildBankManager.getPlayerCurrentPage(player.getUuid());
				List<GuildBankInventory> pages = GuildBankManager.getOrLoadBankPages(server, guild.getShortenedId());
				GuildBankManager.saveAllPages(server, guild.getShortenedId(), pages);
				LOGGER.info("[{}] Saved guild bank for {} on player disconnect: {}", MOD_ID, guild.getName(), player.getName().getString());
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
				GuildPrefixHelper.applyGuildPrefix(newPlayer);
			}
			if (GuildsConfig.enableHungerBoundIntegration) {
				GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(newPlayer);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			for (Guild guild : GuildDataManager.getAllGuilds()) {
				GuildDataManager.saveGuild(server, guild);
			}
			GuildBankManager.saveAll(server);
			LOGGER.info("[{}] Guilds & banks saved successfully.", MOD_ID);
		});
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			GuildDataManager.loadAllGuilds(server);
			LOGGER.info("[{}] Loaded {} guilds.", MOD_ID, GuildDataManager.getAllGuilds().size());
			LOGGER.info("[{}] Reinitializing ally chat bridges...", MOD_ID);
			for (Guild guild : GuildDataManager.getAllGuilds()) {
				String shortIdA = guild.getShortenedId();
				GuildBankManager.getOrLoadBankPages(server, shortIdA);
				for (String shortIdB : guild.getAllies().keySet()) {
					Guild allyGuild = GuildDataManager.getGuildByShortId(shortIdB);
					if (allyGuild == null
							|| !allyGuild.getAllies().containsKey(shortIdA)
							|| AllyChatBridgeManager.isBridgeActive(shortIdA, shortIdB)) {
						continue;
					}
					AllyChatBridgeManager.createBridge(shortIdA, shortIdB);
					LOGGER.info("[{}] Restored bridge between {} and {}", MOD_ID, guild.getName(), allyGuild.getName());
				}
			}
			LOGGER.info("[{}] Ally chat bridge reinitialization complete. Initialized successfully.", MOD_ID);
		});
	}
}
