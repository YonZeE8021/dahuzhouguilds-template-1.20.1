/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Formatting
 *  net.minecraft.DamageSource
 *  net.minecraft.Vec3d
 *  net.minecraft.Text
 *  net.minecraft.ServerPlayerEntity
 *  net.minecraft.server.MinecraftServer
 */
package com.dahuzhou.dahuzhouguilds.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import com.dahuzhou.dahuzhouguilds.GuildTexts;
import com.dahuzhou.dahuzhouguilds.GuildsConfig;
import net.minecraft.util.Formatting;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

public class DelayedTeleportScheduler {
    private static final Map<UUID, TeleportTask> activeTeleports = new HashMap<UUID, TeleportTask>();

    public static void schedule(ServerPlayerEntity player, int delayTicks, Runnable onComplete) {
        UUID playerId = player.getUuid();
        activeTeleports.remove(playerId);
        Vec3d initialPos = player.getPos();
        float initialYaw = player.getYaw();
        float initialPitch = player.getPitch();
        TeleportTask task = new TeleportTask(player, delayTicks, onComplete, initialPos, initialYaw, initialPitch);
        activeTeleports.put(playerId, task);
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, TeleportTask>> iterator = activeTeleports.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TeleportTask> entry = iterator.next();
            TeleportTask task = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                if (player != null) {
                    player.sendMessage(GuildTexts.t("teleport.cancel.death").formatted(Formatting.RED), false);
                }
                iterator.remove();
                continue;
            }
            if (GuildsConfig.cancelTeleportOnMove && !player.getPos().equals((Object)task.initialPos)) {
                player.sendMessage(GuildTexts.t("teleport.cancel.move").formatted(Formatting.RED), false);
                iterator.remove();
                continue;
            }
            --task.delayTicks;
            if (task.delayTicks > 0) continue;
            task.onComplete.run();
            iterator.remove();
        }
    }

    public static void handleDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        if (!GuildsConfig.cancelTeleportOnDamage) {
            return;
        }
        TeleportTask task = activeTeleports.remove(player.getUuid());
        if (task != null) {
            player.sendMessage(GuildTexts.t("teleport.cancel.damage").formatted(Formatting.RED), false);
        }
    }

    private static class TeleportTask {
        final ServerPlayerEntity player;
        int delayTicks;
        final Runnable onComplete;
        final Vec3d initialPos;
        final float initialYaw;
        final float initialPitch;

        public TeleportTask(ServerPlayerEntity player, int delayTicks, Runnable onComplete, Vec3d pos, float yaw, float pitch) {
            this.player = player;
            this.delayTicks = delayTicks;
            this.onComplete = onComplete;
            this.initialPos = pos;
            this.initialYaw = yaw;
            this.initialPitch = pitch;
        }
    }
}

