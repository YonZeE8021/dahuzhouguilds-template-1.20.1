/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
 *  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
 *  net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
 *  net.minecraft.Formatting
 *  net.minecraft.ServerCommandSource
 *  net.minecraft.Text
 *  net.minecraft.Team
 *  net.minecraft.ServerScoreboard
 *  net.minecraft.ServerPlayerEntity
 */
package net.demoniconpc.guildsremasteredmod;

import com.mojang.brigadier.CommandDispatcher;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import net.demoniconpc.guildsremasteredmod.GuildsConfig;
import net.demoniconpc.guildsremasteredmod.commands.AllyCommand;
import net.demoniconpc.guildsremasteredmod.commands.GuildCommand;
import net.demoniconpc.guildsremasteredmod.data.GuildBankManager;
import net.demoniconpc.guildsremasteredmod.data.GuildDataManager;
import net.demoniconpc.guildsremasteredmod.guild.Guild;
import net.demoniconpc.guildsremasteredmod.util.AllyChatBridgeManager;
import net.demoniconpc.guildsremasteredmod.util.DamageHandler;
import net.demoniconpc.guildsremasteredmod.util.DelayedTeleportScheduler;
import net.demoniconpc.guildsremasteredmod.util.GuildBankInventory;
import net.demoniconpc.guildsremasteredmod.util.GuildBankScreenHandler;
import net.demoniconpc.guildsremasteredmod.util.GuildHungerBoundPrefixHelper;
import net.demoniconpc.guildsremasteredmod.util.GuildPrefixHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Formatting;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.network.ServerPlayerEntity;

public class GuildsRemasteredMod
implements ModInitializer {
    public void onInitialize() {
        GuildsConfig.load();
        DamageHandler.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> GuildCommand.register((CommandDispatcher<ServerCommandSource>)dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AllyCommand.register((CommandDispatcher<ServerCommandSource>)dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(server -> DelayedTeleportScheduler.tick(server));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (entity instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity)entity;
                DelayedTeleportScheduler.handleDamage(player, source, damageTaken);
                if (player.currentScreenHandler instanceof GuildBankScreenHandler) {
                    player.closeHandledScreen();
                    player.sendMessage((Text)Text.literal((String)"Your guild bank was closed because you took damage!"), false);
                }
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
            if (guild != null) {
                String motd = guild.getMotd();
                if (motd != null && !motd.isEmpty()) {
                    player.sendMessage((Text)Text.literal((String)"[Guild MOTD] ").append((Text)Text.literal((String)motd).styled(style -> style.withColor(Formatting.field_1075))), false);
                }
                String teamName = "guild_" + guild.getId().toString().substring(0, 8);
                ServerScoreboard scoreboard = server.getScoreboard();
                Team team = scoreboard.getTeam(teamName);
                String guildShortId = guild.getId().toString().substring(0, 8);
                Path bankFilePath = GuildBankManager.getBankFilePath(server, guildShortId);
                if (!Files.exists(bankFilePath, new LinkOption[0])) {
                    GuildBankManager.createBank(server, guildShortId);
                    System.out.println("[GuildBank] Created new bank for guild: " + guildShortId);
                }
                if (team != null) {
                    if (GuildsConfig.enableEssentialsCommandGuildPrefix) {
                        GuildPrefixHelper.applyGuildPrefix(player);
                    }
                    if (GuildsConfig.enableHungerBoundIntegration) {
                        GuildHungerBoundPrefixHelper.applyGuildHungerBoundPrefix(player);
                    }
                } else {
                    System.out.println("[GuildsRemasteredMod] Team " + teamName + " not found for guild " + guild.getName());
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
                int currentPage = GuildBankManager.getPlayerCurrentPage(player.getUuid());
                List<GuildBankInventory> pages = GuildBankManager.getOrLoadBankPages(server, guild.getShortenedId());
                GuildBankManager.saveAllPages(server, guild.getShortenedId(), pages);
                System.out.println("[GuildsRemasteredMod] Saved guild bank for " + guild.getName() + " on player disconnect: " + player.getName().getString());
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
            System.out.println("[GuildsRemasteredMod] Guilds & Banks saved successfully.");
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            GuildDataManager.loadAllGuilds(server);
            System.out.println("[GuildsRemasteredMod] Loaded " + GuildDataManager.getAllGuilds().size() + " guilds.");
            System.out.println("[GuildsRemasteredMod] Reinitializing ally chat bridges...");
            for (Guild guild : GuildDataManager.getAllGuilds()) {
                String shortIdA = guild.getShortenedId();
                GuildBankManager.getOrLoadBankPages(server, shortIdA);
                for (String shortIdB : guild.getAllies().keySet()) {
                    Guild allyGuild = GuildDataManager.getGuildByShortId(shortIdB);
                    if (allyGuild == null || !allyGuild.getAllies().containsKey(shortIdA) || AllyChatBridgeManager.isBridgeActive(shortIdA, shortIdB)) continue;
                    AllyChatBridgeManager.createBridge(shortIdA, shortIdB);
                    System.out.println("[GuildsRemasteredMod] Restored bridge between " + guild.getName() + " and " + allyGuild.getName());
                }
            }
            System.out.println("[GuildsRemasteredMod] Ally chat bridge reinitialization complete.");
            System.out.println("[GuildsRemasteredMod] Initialized successfully.");
        });
    }
}

