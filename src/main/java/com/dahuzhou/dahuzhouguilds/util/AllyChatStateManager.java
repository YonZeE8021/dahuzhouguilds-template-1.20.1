/*
 * Decompiled with CFR 0.152.
 */
package com.dahuzhou.dahuzhouguilds.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AllyChatStateManager {
    private static final Map<UUID, UUID> activeAllyChats = new HashMap<UUID, UUID>();

    public static void toggle(UUID playerId, UUID targetGuildId) {
        if (activeAllyChats.containsKey(playerId) && activeAllyChats.get(playerId).equals(targetGuildId)) {
            activeAllyChats.remove(playerId);
        } else {
            activeAllyChats.put(playerId, targetGuildId);
        }
    }

    public static boolean isChatToggled(UUID playerId) {
        return activeAllyChats.containsKey(playerId);
    }

    public static UUID getTargetGuildId(UUID playerId) {
        return activeAllyChats.get(playerId);
    }

    public static void clearAll(UUID guildId) {
        activeAllyChats.entrySet().removeIf(entry -> ((UUID)entry.getValue()).equals(guildId));
    }
}

