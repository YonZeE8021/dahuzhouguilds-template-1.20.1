/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonDeserializationContext
 *  com.google.gson.JsonDeserializer
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonSerializationContext
 *  com.google.gson.JsonSerializer
 *  net.minecraft.WorldSavePath
 *  net.minecraft.server.MinecraftServer
 */
package com.dahuzhou.dahuzhouguilds.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.InstantTypeAdapter;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.MinecraftServer;

public class GuildDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Instant.class, (Object)new InstantTypeAdapter()).registerTypeAdapter(Guild.class, (Object)new GuildAdapter()).create();
    private static final Map<UUID, Guild> guilds = new HashMap<UUID, Guild>();
    public static final Map<UUID, UUID> pendingInvites = new HashMap<UUID, UUID>();
    private static final Map<UUID, UUID> invites = new HashMap<UUID, UUID>();
    private static final Map<UUID, UUID> playerGuildMemberships = new HashMap<UUID, UUID>();
    public static final Map<UUID, UUID> pendingAllyRequests = new HashMap<UUID, UUID>();
    private static final Map<UUID, UUID> pvpEnableRequests = new HashMap<UUID, UUID>();
    private static final Map<UUID, UUID> pendingPvpRequests = new HashMap<UUID, UUID>();

    public static void loadAllGuilds(MinecraftServer server) {
        File[] files;
        guilds.clear();
        Path dir = server.getSavePath(WorldSavePath.ROOT).resolve("guilds");
        File folder = dir.toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if ((files = folder.listFiles((d, name) -> name.endsWith(".json"))) == null) {
            return;
        }
        for (File file : files) {
            try (FileReader reader = new FileReader(file);){
                Guild guild = (Guild)GSON.fromJson((Reader)reader, Guild.class);
                if (guild == null) continue;
                guilds.put(guild.getId(), guild);
            }
            catch (Exception e) {
                System.err.println("[GuildsRemastered] Failed to load guild: " + file.getName());
                e.printStackTrace();
            }
        }
        System.out.println("[GuildDataManager] Loaded " + guilds.size() + " guild(s).");
        GuildDataManager.loadPlayerGuildMemberships(server);
    }

    private static void loadPlayerGuildMemberships(MinecraftServer server) {
    }

    public static void saveGuild(MinecraftServer server, Guild guild) {
        try {
            Path dir = server.getSavePath(WorldSavePath.ROOT).resolve("guilds");
            Files.createDirectories(dir);
            String shortId = guild.getId().toString().substring(0, 8);
            Path file = dir.resolve("guild_" + shortId + ".json");
            try (FileWriter writer = new FileWriter(file.toFile());){
                GSON.toJson((Object)guild, (Appendable)writer);
            }
            System.out.println("[GuildDataManager] Saved guild: " + guild.getName() + " as " + String.valueOf(file.getFileName()));
        }
        catch (IOException e) {
            System.err.println("[GuildDataManager] Failed to save guild: " + guild.getName());
            e.printStackTrace();
        }
    }

    public static void deleteGuild(MinecraftServer server, UUID guildId) {
        if (!guilds.containsKey(guildId)) {
            System.out.println("[GuildDataManager] No guild found with ID: " + String.valueOf(guildId));
            return;
        }
        guilds.remove(guildId);
        String shortenedGuildId = "guild_" + guildId.toString().substring(0, 8) + ".json";
        Path guildsDir = server.getSavePath(WorldSavePath.ROOT).resolve("guilds");
        Path file = guildsDir.resolve(shortenedGuildId);
        try {
            if (!Files.exists(guildsDir, new LinkOption[0])) {
                System.out.println("[GuildDataManager] Guilds directory not found, creating it.");
                Files.createDirectories(guildsDir);
            }
            if (Files.deleteIfExists(file)) {
                System.out.println("[GuildDataManager] Successfully deleted guild file: " + file.toString());
            } else {
                System.out.println("[GuildDataManager] Guild file not found: " + file.toString());
            }
        }
        catch (IOException e) {
            System.err.println("[GuildDataManager] Failed to delete guild file: " + file.toString());
            e.printStackTrace();
        }
    }

    public static void removeAllInvitesToGuild(UUID guildId) {
        invites.entrySet().removeIf(entry -> ((UUID)entry.getValue()).equals(guildId));
    }

    public static Collection<Guild> getAllGuilds() {
        return guilds.values();
    }

    public static Guild getGuildById(UUID id) {
        return guilds.get(id);
    }

    public static Guild getGuildByShortId(String shortId) {
        for (Guild guild : guilds.values()) {
            String guildShortId = "guild_" + guild.getId().toString().substring(0, 8);
            if (!guildShortId.equals(shortId)) continue;
            return guild;
        }
        return null;
    }

    public static Guild getGuildByPlayer(UUID playerId) {
        for (Guild guild : guilds.values()) {
            if (!guild.isMember(playerId)) continue;
            return guild;
        }
        return null;
    }

    public static String getGuildPrefixByPlayer(UUID playerId) {
        Guild guild = GuildDataManager.getGuildByPlayer(playerId);
        if (guild != null) {
            return "[" + guild.getName() + "]";
        }
        return "";
    }

    public static Guild getGuildByName(String name) {
        return guilds.values().stream().filter(g -> g.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /**
     * 解析指令中的公會識別：完整 UUID，或舊版 {@code guild_} 開頭的短 ID（與存檔 / 盟友鍵一致）。
     */
    public static Guild getGuildByIdInput(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            Guild byUuid = GuildDataManager.getGuildById(UUID.fromString(t));
            if (byUuid != null) {
                return byUuid;
            }
        }
        catch (IllegalArgumentException ignored) {
        }
        if (t.startsWith("guild_")) {
            return GuildDataManager.getGuildByShortId(t);
        }
        return null;
    }

    public static void clearAllInvites() {
        invites.clear();
    }

    public static void clearAllPendingAllyRequests() {
        pendingAllyRequests.clear();
    }

    public static void registerGuild(Guild guild) {
        guilds.put(guild.getId(), guild);
    }

    public static boolean isGuildNameTaken(String name) {
        return guilds.values().stream().anyMatch(g -> g.getName().equalsIgnoreCase(name));
    }

    public static void addInvite(UUID playerId, UUID guildId) {
        invites.put(playerId, guildId);
    }

    public static UUID getInvite(UUID playerId) {
        return invites.get(playerId);
    }

    public static void removeInvite(UUID playerId) {
        invites.remove(playerId);
    }

    public static UUID getGuildIdForPlayer(UUID playerId) {
        for (Guild guild : guilds.values()) {
            if (!guild.isMember(playerId)) continue;
            return guild.getId();
        }
        return null;
    }

    public static void addPvpEnableRequest(UUID targetGuildMasterId, UUID requesterGuildId) {
        pendingPvpRequests.put(targetGuildMasterId, requesterGuildId);
    }

    public static UUID getPvpEnableRequest(UUID targetGuildMasterId) {
        return pendingPvpRequests.get(targetGuildMasterId);
    }

    public static void clearPvpRequest(UUID targetGuildMasterId) {
        pendingPvpRequests.remove(targetGuildMasterId);
    }

    private static Path getGuildFilePath(MinecraftServer server, UUID guildId) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("guilds").resolve("guild_" + String.valueOf(guildId) + ".json");
    }

    public static Guild loadGuild(MinecraftServer server, UUID guildId) {
        Path path = GuildDataManager.getGuildFilePath(server, guildId);
        if (!Files.exists(path)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, Guild.class);
        }
        catch (IOException e) {
            System.err.println("[GuildDataManager] Failed to load guild from path: " + path);
            e.printStackTrace();
            return null;
        }
    }

    public static Guild getPlayerGuild(UUID playerUuid) {
        return guilds.get(playerUuid);
    }

    public static boolean isGuildDisplayEnabled() {
        return true;
    }

    public static void addAllyRequest(UUID targetOwner, UUID requesterGuildId) {
        pendingAllyRequests.put(targetOwner, requesterGuildId);
    }

    public static UUID getAllyRequest(UUID targetOwner) {
        return pendingAllyRequests.get(targetOwner);
    }

    public static void removeAllyRequest(UUID targetOwner) {
        pendingAllyRequests.remove(targetOwner);
    }

    private static class GuildAdapter
    implements JsonSerializer<Guild>,
    JsonDeserializer<Guild> {
        private GuildAdapter() {
        }

        public JsonElement serialize(Guild guild, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", guild.getShortenedId());
            obj.addProperty("name", guild.getName());
            obj.addProperty("color", guild.getColor());
            obj.addProperty("prefix", guild.getPrefix());
            obj.addProperty("motd", guild.getMotd());
            obj.add("createdAt", context.serialize((Object)guild.getCreatedAt()));
            obj.addProperty("ownerId", guild.getOwnerId().toString());
            obj.addProperty("ownerName", guild.getOwnerName());
            obj.addProperty("friendlyFire", Boolean.valueOf(guild.isFriendlyFireEnabled()));
            if (guild.hasHome()) {
                obj.addProperty("homeX", (Number)guild.getHomeX());
                obj.addProperty("homeY", (Number)guild.getHomeY());
                obj.addProperty("homeZ", (Number)guild.getHomeZ());
                obj.addProperty("homeWorld", guild.getHomeWorld());
            }
            JsonObject permsJson = new JsonObject();
            for (Map.Entry<String, Map<String, Boolean>> entry : guild.rankPermissions.entrySet()) {
                JsonObject jsonObject = new JsonObject();
                for (Map.Entry<String, Boolean> entry2 : entry.getValue().entrySet()) {
                    jsonObject.addProperty(entry2.getKey(), entry2.getValue());
                }
                permsJson.add(entry.getKey(), (JsonElement)jsonObject);
            }
            obj.add("rank_permissions", (JsonElement)permsJson);
            JsonObject alliesObj = new JsonObject();
            for (Map.Entry<String, Guild.AllyInfo> entry : guild.getAllies().entrySet()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("name", entry.getValue().name);
                jsonObject.addProperty("pvpDisabled", Boolean.valueOf(entry.getValue().pvpDisabled));
                alliesObj.add(entry.getKey(), (JsonElement)jsonObject);
            }
            obj.add("allies", (JsonElement)alliesObj);
            JsonArray jsonArray = new JsonArray();
            for (UUID uUID : guild.getEnemies()) {
                jsonArray.add(uUID.toString());
            }
            obj.add("enemies", (JsonElement)jsonArray);
            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<UUID, Guild.GuildMemberInfo> entry : guild.getMembers().entrySet()) {
                JsonObject member = new JsonObject();
                member.addProperty("name", entry.getValue().name);
                member.addProperty("rank", entry.getValue().rank);
                jsonObject.add(entry.getKey().toString(), (JsonElement)member);
            }
            obj.add("members", (JsonElement)jsonObject);
            obj.addProperty("unlockedBankTabs", (Number)guild.getUnlockedBankTabs());
            JsonObject jsonObject2 = new JsonObject();
            for (Map.Entry<Integer, Integer> entry : guild.getBankUnlockMap().entrySet()) {
                jsonObject2.addProperty(String.valueOf(entry.getKey()), (Number)entry.getValue());
            }
            obj.add("bankTabUnlockProgress", (JsonElement)jsonObject2);
            return obj;
        }

        public Guild deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String idStr = obj.get("id").getAsString();
            UUID id = UUID.fromString(idStr.replace("guild_", "") + "-0000-0000-0000-000000000000");
            String name = obj.get("name").getAsString();
            String color = obj.get("color").getAsString();
            Instant createdAt = (Instant)context.deserialize(obj.get("createdAt"), Instant.class);
            UUID ownerId = UUID.fromString(obj.get("ownerId").getAsString());
            String ownerName = obj.get("ownerName").getAsString();
            boolean friendlyFire = obj.get("friendlyFire").getAsBoolean();
            Guild guild = new Guild(id, name, color, createdAt, ownerId, ownerName, friendlyFire);
            if (obj.has("prefix")) {
                guild.setPrefix(obj.get("prefix").getAsString());
            }
            if (obj.has("motd")) {
                guild.setMotd(obj.get("motd").getAsString());
            } else {
                guild.setMotd("Default Guild MOTD - change with /guild motd [message]");
            }
            if (obj.has("homeX") && obj.has("homeY") && obj.has("homeZ") && obj.has("homeWorld")) {
                double x = obj.get("homeX").getAsDouble();
                double y = obj.get("homeY").getAsDouble();
                double z = obj.get("homeZ").getAsDouble();
                String world = obj.get("homeWorld").getAsString();
                guild.setHome(x, y, z, world);
            }
            if (obj.has("rank_permissions")) {
                JsonObject permsJson = obj.getAsJsonObject("rank_permissions");
                for (Map.Entry rankEntry : permsJson.entrySet()) {
                    String rank = (String)rankEntry.getKey();
                    JsonObject perms = ((JsonElement)rankEntry.getValue()).getAsJsonObject();
                    HashMap<String, Boolean> permMap = new HashMap<String, Boolean>();
                    for (Map.Entry permEntry : perms.entrySet()) {
                        permMap.put((String)permEntry.getKey(), ((JsonElement)permEntry.getValue()).getAsBoolean());
                    }
                    guild.setRankPermissions(rank, permMap);
                }
            }
            if (obj.has("allies")) {
                JsonObject alliesObj = obj.getAsJsonObject("allies");
                for (Map.Entry entry : alliesObj.entrySet()) {
                    String shortId = (String)entry.getKey();
                    JsonObject allyData = ((JsonElement)entry.getValue()).getAsJsonObject();
                    String allyName = allyData.get("name").getAsString();
                    boolean pvpDisabled = allyData.has("pvpDisabled") && allyData.get("pvpDisabled").getAsBoolean();
                    Guild.AllyInfo info = new Guild.AllyInfo(allyName, pvpDisabled);
                    guild.getAllies().put(shortId, info);
                }
            }
            if (obj.has("enemies")) {
                JsonArray enemiesArray = obj.getAsJsonArray("enemies");
                for (JsonElement element : enemiesArray) {
                    guild.addEnemy(UUID.fromString(element.getAsString()));
                }
            }
            JsonObject members = obj.getAsJsonObject("members");
            for (Map.Entry entry : members.entrySet()) {
                UUID memberId = UUID.fromString((String)entry.getKey());
                JsonObject info = ((JsonElement)entry.getValue()).getAsJsonObject();
                String playerName = info.get("name").getAsString();
                String rank = info.get("rank").getAsString();
                guild.addMember(memberId, playerName, rank);
            }
            if (obj.has("unlockedBankTabs")) {
                guild.setUnlockedBankTabs(obj.get("unlockedBankTabs").getAsInt());
            }
            if (obj.has("bankTabUnlockProgress")) {
                JsonObject tabProgressObj = obj.getAsJsonObject("bankTabUnlockProgress");
                for (Map.Entry entry : tabProgressObj.entrySet()) {
                    try {
                        int tab = Integer.parseInt((String)entry.getKey());
                        int value = ((JsonElement)entry.getValue()).getAsInt();
                        guild.addBankUnlockProgress(tab, value);
                    }
                    catch (NumberFormatException numberFormatException) {}
                }
            }
            return guild;
        }
    }

    public static class ChatStateManager {
        private static final Set<UUID> guildChatEnabled = new HashSet<UUID>();

        public static boolean isGuildChatEnabled(UUID playerId) {
            return guildChatEnabled.contains(playerId);
        }

        public static boolean toggleGuildChat(UUID playerId) {
            if (!guildChatEnabled.remove(playerId)) {
                guildChatEnabled.add(playerId);
                return true;
            }
            return false;
        }
    }
}

