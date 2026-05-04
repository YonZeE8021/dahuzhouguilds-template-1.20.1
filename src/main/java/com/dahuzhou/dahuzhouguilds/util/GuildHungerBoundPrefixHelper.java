/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.ServerPlayerEntity
 *  net.minecraft.WorldSavePath
 *  net.minecraft.server.MinecraftServer
 */
package com.dahuzhou.dahuzhouguilds.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.dahuzhou.dahuzhouguilds.GuildsConfig;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.MinecraftServer;

public class GuildHungerBoundPrefixHelper {
    public static void applyGuildHungerBoundPrefix(ServerPlayerEntity player) {
        String formatted;
        if (!GuildsConfig.enableHungerBoundIntegration) {
            return;
        }
        Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
        String deathColor = GuildHungerBoundPrefixHelper.getDeathColorFromDeathCount(player);
        String playerName = player.getName().getString();
        if (guild != null) {
            String guildColor = guild.getColor();
            String guildPrefix = "[" + guild.getName() + "] ";
            formatted = String.format("<%s>%s<%s>%s", guildColor, guildPrefix, deathColor, playerName);
        } else {
            formatted = String.format("<%s>%s", deathColor, playerName);
        }
        MinecraftServer server = player.getServer();
        server.execute(() -> {
            server.getCommandManager().executeWithPrefix(server.getCommandSource().withLevel(4), "/nickname set " + playerName + " " + formatted);
            System.out.println("[GuildHungerBoundPrefixHelper] Set nickname for player: " + playerName);
        });
    }

    public static String getDeathColorFromDeathCount(ServerPlayerEntity player) {
        int deathCount = GuildHungerBoundPrefixHelper.getDeathCountFromPlayerDataManager(player);
        if (deathCount == 1) {
            return "yellow";
        }
        if (deathCount >= 2) {
            return "red";
        }
        return "green";
    }

    public static int getDeathCountFromPlayerDataManager(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        Path playerDataPath = server.getSavePath(WorldSavePath.ROOT).resolve("playerdata").resolve(player.getUuid().toString() + ".json");
        if (!Files.exists(playerDataPath)) {
            return 0;
        }
        try (BufferedReader reader = Files.newBufferedReader(playerDataPath)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return json.has("deathCount") ? json.get("deathCount").getAsInt() : 0;
        }
        catch (IOException e) {
            System.err.println("[GuildHungerBoundPrefixHelper] Failed to read deathCount from: " + playerDataPath);
            e.printStackTrace();
            return 0;
        }
    }

    public static void clearNickname(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        String deathColor = GuildHungerBoundPrefixHelper.getDeathColorFromDeathCount(player);
        String formatted = String.format("<%s>%s", deathColor, player.getName().getString());
        server.execute(() -> {
            server.getCommandManager().executeWithPrefix(server.getCommandSource().withLevel(4), "/nickname set " + player.getName().getString() + " " + formatted);
            System.out.println("[GuildHungerBoundPrefixHelper] Cleared guild info for player: " + player.getName().getString());
        });
    }
}

