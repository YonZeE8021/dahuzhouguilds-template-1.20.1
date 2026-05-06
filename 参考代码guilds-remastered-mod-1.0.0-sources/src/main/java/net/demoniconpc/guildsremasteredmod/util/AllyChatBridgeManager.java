/*
 * Decompiled with CFR 0.152.
 */
package net.demoniconpc.guildsremasteredmod.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AllyChatBridgeManager {
    private static final Set<String> activeBridges = new HashSet<String>();
    private static final Map<UUID, UUID> allyChatToggles = new HashMap<UUID, UUID>();

    public static String getBridgeId(String shortIdA, String shortIdB) {
        return shortIdA.compareTo(shortIdB) < 0 ? "bridge_" + shortIdA + "_" + shortIdB : "bridge_" + shortIdB + "_" + shortIdA;
    }

    public static void createBridge(String shortIdA, String shortIdB) {
        activeBridges.add(AllyChatBridgeManager.getBridgeId(shortIdA, shortIdB));
    }

    public static void removeBridge(String shortIdA, String shortIdB) {
        activeBridges.remove(AllyChatBridgeManager.getBridgeId(shortIdA, shortIdB));
    }

    public static boolean isBridgeActive(String shortIdA, String shortIdB) {
        return activeBridges.contains(AllyChatBridgeManager.getBridgeId(shortIdA, shortIdB));
    }

    public static void setAllyChatToggle(UUID playerId, UUID targetGuildId) {
        allyChatToggles.put(playerId, targetGuildId);
    }

    public static void clearAllyChatToggle(UUID playerId) {
        allyChatToggles.remove(playerId);
    }

    public static UUID getTargetGuild(UUID playerId) {
        return allyChatToggles.get(playerId);
    }

    public static boolean isChatToggled(UUID playerId) {
        return allyChatToggles.containsKey(playerId);
    }
}

