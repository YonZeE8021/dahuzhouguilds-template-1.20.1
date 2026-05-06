/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
 *  net.minecraft.ServerPlayerEntity
 */
package net.demoniconpc.guildsremasteredmod.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.demoniconpc.guildsremasteredmod.util.GuildTeamUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class DelayedGuildTaskManager {
    private static final Map<UUID, Integer> playerDelayTicks = new HashMap<UUID, Integer>();
    private static final Map<UUID, ServerPlayerEntity> trackedPlayers = new HashMap<UUID, ServerPlayerEntity>();

    public static void queueUpdate(ServerPlayerEntity player, int delayTicks) {
        UUID uuid = player.getUuid();
        playerDelayTicks.put(uuid, delayTicks);
        trackedPlayers.put(uuid, player);
        System.out.println("[DelayedGuildTaskManager] Queued update for player " + player.getName().getString() + " with " + delayTicks + " ticks delay.");
    }

    public static void registerTickHandler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, Integer>> iterator = playerDelayTicks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = iterator.next();
                UUID uuid = entry.getKey();
                int ticksLeft = entry.getValue() - 1;
                if (ticksLeft <= 0) {
                    ServerPlayerEntity player = trackedPlayers.get(uuid);
                    if (player != null && player.getServer() != null) {
                        System.out.println("[DelayedGuildTaskManager] Executing task for player " + player.getName().getString());
                        GuildTeamUtil.forceUpdateForPlayer(player);
                    }
                    iterator.remove();
                    trackedPlayers.remove(uuid);
                    System.out.println("[DelayedGuildTaskManager] Task completed for player " + String.valueOf(uuid));
                    continue;
                }
                entry.setValue(ticksLeft);
            }
        });
    }
}

