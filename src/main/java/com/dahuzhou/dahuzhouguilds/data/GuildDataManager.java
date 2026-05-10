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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
            try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
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
        GuildDataManager.finishLoad(server);
        GuildDataManager.loadPlayerGuildMemberships(server);
    }

    /**
     * 分配四位數字編號、遷移盟友鍵與公會銀行檔名，並回寫存檔。
     */
    private static void finishLoad(MinecraftServer server) {
        GuildDataManager.ensureAllGuildsHaveNumericIds();
        GuildDataManager.migrateAllAllyKeys();
        GuildBankManager.migrateLegacyBankFiles(server, guilds.values());
        GuildBankManager.clearBankCache();
        for (Guild g : guilds.values()) {
            GuildDataManager.saveGuild(server, g);
        }
    }

    private static int nextFreeNumericId(Set<Integer> used) {
        for (int i = 0; i < 10000; ++i) {
            if (!used.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    private static void ensureAllGuildsHaveNumericIds() {
        HashSet<Integer> used = new HashSet<Integer>();
        ArrayList<Guild> needNew = new ArrayList<Guild>();
        for (Guild g : guilds.values()) {
            int n = g.getNumericPublicId();
            if (n >= 0 && n <= 9999 && !used.contains(n)) {
                used.add(n);
                continue;
            }
            needNew.add(g);
        }
        for (Guild g : needNew) {
            int free = GuildDataManager.nextFreeNumericId(used);
            if (free < 0) {
                System.err.println("[GuildDataManager] Cannot assign numeric guild id (max 10000 guilds): " + g.getName());
                continue;
            }
            g.setNumericPublicId(free);
            used.add(free);
        }
    }

    private static String resolveAllyKeyToPublicShort(String key) {
        if (key == null) {
            return null;
        }
        if (key.matches("\\d{4}")) {
            return key;
        }
        String rest = key.startsWith("guild_") ? key.substring("guild_".length()) : key;
        if (rest.matches("[0-9a-fA-F]{8}")) {
            for (Guild g : guilds.values()) {
                if (!g.getId().toString().startsWith(rest)) continue;
                return g.getShortenedId();
            }
        }
        return null;
    }

    private static void migrateAllAllyKeys() {
        for (Guild guild : guilds.values()) {
            Map<String, Guild.AllyInfo> allies = guild.getAllies();
            if (allies.isEmpty()) {
                continue;
            }
            LinkedHashMap<String, Guild.AllyInfo> replacement = new LinkedHashMap<String, Guild.AllyInfo>();
            boolean changed = false;
            for (Map.Entry<String, Guild.AllyInfo> e : new ArrayList<Map.Entry<String, Guild.AllyInfo>>(allies.entrySet())) {
                String key = e.getKey();
                Guild.AllyInfo info = e.getValue();
                String newKey = GuildDataManager.resolveAllyKeyToPublicShort(key);
                if (newKey == null) {
                    newKey = key;
                }
                if (!newKey.equals(key)) {
                    changed = true;
                }
                replacement.putIfAbsent(newKey, info);
            }
            if (!changed) continue;
            allies.clear();
            allies.putAll(replacement);
        }
    }

    public static void assignNumericIdForNewGuild(Guild guild) {
        if (guild.getNumericPublicId() >= 0 && guild.getNumericPublicId() <= 9999) {
            return;
        }
        HashSet<Integer> used = new HashSet<Integer>();
        for (Guild o : guilds.values()) {
            if (o == guild) continue;
            int n = o.getNumericPublicId();
            if (n < 0 || n > 9999) continue;
            used.add(n);
        }
        int free = GuildDataManager.nextFreeNumericId(used);
        if (free < 0) {
            throw new IllegalStateException("No free guild numeric id (max 10000 guilds)");
        }
        guild.setNumericPublicId(free);
    }

    private static void loadPlayerGuildMemberships(MinecraftServer server) {
    }

    public static void saveGuild(MinecraftServer server, Guild guild) {
        try {
            Path dir = server.getSavePath(WorldSavePath.ROOT).resolve("guilds");
            Files.createDirectories(dir);
            String shortId = guild.getId().toString().substring(0, 8);
            Path file = dir.resolve("guild_" + shortId + ".json");
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
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
        if (shortId == null) {
            return null;
        }
        String t = shortId.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.startsWith("guild_")) {
            String rest = t.substring("guild_".length());
            if (rest.matches("[0-9a-fA-F]{8}")) {
                for (Guild guild : guilds.values()) {
                    if (!guild.getId().toString().startsWith(rest)) continue;
                    return guild;
                }
                return null;
            }
            if (rest.matches("\\d{4}")) {
                return GuildDataManager.getGuildByNumericCode(rest);
            }
            return null;
        }
        if (t.matches("\\d{1,4}")) {
            try {
                int v = Integer.parseInt(t);
                if (v >= 0 && v <= 9999) {
                    return GuildDataManager.getGuildByNumericCode(String.format("%04d", v));
                }
            }
            catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Guild getGuildByNumericCode(String fourDigits) {
        for (Guild guild : guilds.values()) {
            int n = guild.getNumericPublicId();
            if (n < 0 || n > 9999) continue;
            if (!Guild.formatPublicId(n).equals(fourDigits)) continue;
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
     * 解析指令中的公會識別：完整 UUID、四位數字編號（可省略前導零至 1 位）、或舊版 {@code guild_} 開頭的 8 位十六進制短 ID。
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
        return GuildDataManager.getGuildByShortId(t);
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
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
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
            obj.addProperty("uuid", guild.getId().toString());
            obj.addProperty("id", guild.getShortenedId());
            obj.addProperty("name", guild.getName());
            obj.addProperty("color", guild.getColor());
            obj.addProperty("prefix", guild.getPrefix());
            obj.addProperty("motd", guild.getMotd());
            obj.add("createdAt", context.serialize((Object)guild.getCreatedAt()));
            obj.addProperty("ownerId", guild.getOwnerId().toString());
            obj.addProperty("ownerName", guild.getOwnerName());
            obj.addProperty("friendlyFire", Boolean.valueOf(guild.isFriendlyFireEnabled()));
            obj.addProperty("numericPublicId", (Number)guild.getNumericPublicId());
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
            UUID id = obj.has("uuid") ? UUID.fromString(obj.get("uuid").getAsString()) : UUID.fromString(obj.get("id").getAsString().replace("guild_", "") + "-0000-0000-0000-000000000000");
            String name = obj.get("name").getAsString();
            String color = obj.get("color").getAsString();
            Instant createdAt = (Instant)context.deserialize(obj.get("createdAt"), Instant.class);
            UUID ownerId = UUID.fromString(obj.get("ownerId").getAsString());
            String ownerName = obj.get("ownerName").getAsString();
            boolean friendlyFire = obj.get("friendlyFire").getAsBoolean();
            Guild guild = new Guild(id, name, color, createdAt, ownerId, ownerName, friendlyFire);
            if (obj.has("numericPublicId")) {
                guild.setNumericPublicId(obj.get("numericPublicId").getAsInt());
            }
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

