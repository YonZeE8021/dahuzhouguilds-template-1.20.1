/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package com.dahuzhou.dahuzhouguilds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

public class GuildsConfig {
    private static final File CONFIG_FILE = new File("config/guilds_remastered_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** 設定檔內僅供閱讀之說明鍵，載入時忽略。 */
    private static final String COMMENT_REQUIRE_OP_TO_CREATE_GUILD_KEY = "_comment_require_Op_To_Create_Guild";
    private static final String COMMENT_REQUIRE_OP_TO_CREATE_GUILD =
            "require_Op_To_Create_Guild：為 false 時任意玩家可使用 /guild create；為 true 時需權限等級 2／OP。"
                    + "若為 true 且管理員需代其他玩家建立公會，請使用：execute as <玩家> run guild create <名稱> <顏色>";
    private static final String COMMENT_REQUIRE_OP_TO_TRANSFER_GUILD_KEY = "_comment_require_Op_To_Transfer_Guild";
    private static final String COMMENT_REQUIRE_OP_TO_TRANSFER_GUILD =
            "require_Op_To_Transfer_Guild：為 false 時任意玩家（仍須為現任公會長）可使用 /guild transfer；為 true 時需權限等級 2／OP。"
                    + "若為 true，公會長本人無 OP 時可由管理員代執行：execute as <公會長> run guild transfer <新會長玩家名>";
    public static boolean enableEssentialsCommandGuildPrefix = false;
    public static boolean enableHungerBoundIntegration = false;
    /** 公会银行（/guild bank、捐献、解锁页、权限菜单中的银行项）。默认关闭。 */
    public static boolean enableGuildBank = false;
    /** 公会驻地（/guild sethome、/guild home 及菜单中的设家项）。默认关闭。 */
    public static boolean enableGuildHome = false;
    public static int guildHomeTeleportDelaySeconds = 6;
    public static boolean cancelTeleportOnMove = true;
    public static boolean cancelTeleportOnDamage = true;
    /**
     * 為 false 時任意玩家可使用 /guild create；為 true 時需權限等級 2（與原硬編碼行為一致）。缺鍵時預設 true，舊版設定檔相容。
     * 若為開（true）：需由管理員代為建立時，可使用 {@code execute as <玩家> run guild create <名稱> <顏色>}，以該玩家身分執行建立指令。
     */
    public static boolean requireOpToCreateGuild = true;
    /**
     * 為 false 時現任公會長可使用 /guild transfer（無需 OP）；為 true 時需權限等級 2。缺鍵時預設 true。
     */
    public static boolean requireOpToTransferGuild = true;

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            GuildsConfig.saveDefault();
            return;
        }
        JsonObject json = null;
        try (FileReader reader = new FileReader(CONFIG_FILE);){
            json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
        }
        catch (Exception e) {
            System.err.println("[GuildConfig] Failed to read config. Using defaults.");
            e.printStackTrace();
        }
        if (json == null) {
            System.err.println("[GuildConfig] Config JSON was empty or invalid. Using defaults.");
        } else {
            try {
                if (json.has("enable_Essentials_Command_Guild_Prefix")) {
                    enableEssentialsCommandGuildPrefix = json.get("enable_Essentials_Command_Guild_Prefix").getAsBoolean();
                }
                if (json.has("enable_HungerBound_Integration")) {
                    enableHungerBoundIntegration = json.get("enable_HungerBound_Integration").getAsBoolean();
                }
                if (json.has("enable_Guild_Bank")) {
                    enableGuildBank = json.get("enable_Guild_Bank").getAsBoolean();
                }
                if (json.has("enable_Guild_Home")) {
                    enableGuildHome = json.get("enable_Guild_Home").getAsBoolean();
                }
                if (json.has("guild_Home_Teleport_Delay_Seconds")) {
                    guildHomeTeleportDelaySeconds = json.get("guild_Home_Teleport_Delay_Seconds").getAsInt();
                }
                if (json.has("cancel_teleport_on_move")) {
                    cancelTeleportOnMove = json.get("cancel_teleport_on_move").getAsBoolean();
                }
                if (json.has("cancel_teleport_on_damage")) {
                    cancelTeleportOnDamage = json.get("cancel_teleport_on_damage").getAsBoolean();
                }
                if (json.has("require_Op_To_Create_Guild")) {
                    requireOpToCreateGuild = json.get("require_Op_To_Create_Guild").getAsBoolean();
                }
                if (json.has("require_Op_To_Transfer_Guild")) {
                    requireOpToTransferGuild = json.get("require_Op_To_Transfer_Guild").getAsBoolean();
                }
            }
            catch (Exception e) {
                System.err.println("[GuildConfig] Failed to parse config fields. Using defaults.");
                e.printStackTrace();
            }
            mergeMissingKeysIntoConfigFile(json);
        }
        System.out.println("[GuildConfig] Loaded enable_Essentials_Command_Guild_Prefix = " + enableEssentialsCommandGuildPrefix);
        System.out.println("[GuildConfig] Loaded enable_HungerBound_Integration = " + enableHungerBoundIntegration);
        System.out.println("[GuildConfig] Loaded enable_Guild_Bank = " + enableGuildBank);
        System.out.println("[GuildConfig] Loaded enable_Guild_Home = " + enableGuildHome);
        System.out.println("[GuildConfig] Loaded guild_Home_Teleport_Delay_Seconds = " + guildHomeTeleportDelaySeconds);
        System.out.println("[GuildConfig] Loaded require_Op_To_Create_Guild = " + requireOpToCreateGuild);
        System.out.println("[GuildConfig] Loaded require_Op_To_Transfer_Guild = " + requireOpToTransferGuild);
    }

    /**
     * 舊版設定檔缺少新鍵時寫回預設值，行為不變且方便服主發現選項。
     */
    private static void mergeMissingKeysIntoConfigFile(JsonObject json) {
        if (json == null) {
            return;
        }
        boolean changed = false;
        if (!json.has(COMMENT_REQUIRE_OP_TO_CREATE_GUILD_KEY)) {
            json.addProperty(COMMENT_REQUIRE_OP_TO_CREATE_GUILD_KEY, COMMENT_REQUIRE_OP_TO_CREATE_GUILD);
            changed = true;
        }
        if (!json.has("require_Op_To_Create_Guild")) {
            json.addProperty("require_Op_To_Create_Guild", requireOpToCreateGuild);
            changed = true;
        }
        if (!json.has(COMMENT_REQUIRE_OP_TO_TRANSFER_GUILD_KEY)) {
            json.addProperty(COMMENT_REQUIRE_OP_TO_TRANSFER_GUILD_KEY, COMMENT_REQUIRE_OP_TO_TRANSFER_GUILD);
            changed = true;
        }
        if (!json.has("require_Op_To_Transfer_Guild")) {
            json.addProperty("require_Op_To_Transfer_Guild", requireOpToTransferGuild);
            changed = true;
        }
        if (!changed) {
            return;
        }
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson((JsonElement)json, (Appendable)writer);
            System.out.println("[GuildConfig] Updated guilds_remastered_config.json with missing keys.");
        }
        catch (IOException e) {
            System.err.println("[GuildConfig] Failed to merge missing keys into config.");
            e.printStackTrace();
        }
    }

    private static void saveDefault() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            JsonObject json = new JsonObject();
            json.addProperty("enable_Essentials_Command_Guild_Prefix", Boolean.valueOf(enableEssentialsCommandGuildPrefix));
            json.addProperty("enable_HungerBound_Integration", Boolean.valueOf(enableHungerBoundIntegration));
            json.addProperty("enable_Guild_Bank", Boolean.valueOf(enableGuildBank));
            json.addProperty("enable_Guild_Home", Boolean.valueOf(enableGuildHome));
            json.addProperty("guild_Home_Teleport_Delay_Seconds", (Number)guildHomeTeleportDelaySeconds);
            json.addProperty("cancel_teleport_on_move", Boolean.valueOf(cancelTeleportOnMove));
            json.addProperty("cancel_teleport_on_damage", Boolean.valueOf(cancelTeleportOnDamage));
            json.addProperty(COMMENT_REQUIRE_OP_TO_CREATE_GUILD_KEY, COMMENT_REQUIRE_OP_TO_CREATE_GUILD);
            json.addProperty("require_Op_To_Create_Guild", Boolean.valueOf(requireOpToCreateGuild));
            json.addProperty(COMMENT_REQUIRE_OP_TO_TRANSFER_GUILD_KEY, COMMENT_REQUIRE_OP_TO_TRANSFER_GUILD);
            json.addProperty("require_Op_To_Transfer_Guild", Boolean.valueOf(requireOpToTransferGuild));
            try (FileWriter writer = new FileWriter(CONFIG_FILE);){
                GSON.toJson((JsonElement)json, (Appendable)writer);
            }
        }
        catch (IOException e) {
            System.err.println("[GuildConfig] Failed to write default config.");
            e.printStackTrace();
        }
    }
}

