/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ServerPlayerEntity
 *  net.minecraft.server.MinecraftServer
 */
package net.demoniconpc.guildsremasteredmod.util;

import net.demoniconpc.guildsremasteredmod.GuildsConfig;
import net.demoniconpc.guildsremasteredmod.data.GuildDataManager;
import net.demoniconpc.guildsremasteredmod.guild.Guild;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

public class GuildPrefixHelper {
    public static void applyGuildPrefix(ServerPlayerEntity player) {
        if (!GuildsConfig.enableEssentialsCommandGuildPrefix) {
            return;
        }
        Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
        if (guild == null) {
            return;
        }
        String guildPrefix = "[" + guild.getName() + "] ";
        String json = String.format("{\"text\":\"%s\",\"color\":\"%s\"}", guildPrefix + player.getName().getString(), guild.getColor());
        MinecraftServer server = player.getServer();
        server.execute(() -> {
            server.getCommandManager().executeWithPrefix(server.getCommandSource().withLevel(4), "/nickname set " + player.getName().getString() + " " + json);
            System.out.println("[GuildPrefixHelper] Applied guild prefix to nickname: " + json);
        });
    }

    public static void clearNickname(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        server.execute(() -> {
            server.getCommandManager().executeWithPrefix(server.getCommandSource().withLevel(4), "/nickname set " + player.getName().getString() + " {\"text\":\"" + player.getName().getString() + "\",\"color\":\"gray\"}");
            System.out.println("[GuildPrefixHelper] Cleared nickname for player: " + player.getName().getString());
        });
    }
}

