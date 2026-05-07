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
    public static boolean enableEssentialsCommandGuildPrefix = false;
    public static boolean enableHungerBoundIntegration = false;
    /** 公会银行（/guild bank、捐献、解锁页、权限菜单中的银行项）。默认关闭。 */
    public static boolean enableGuildBank = false;
    /** 公会驻地（/guild sethome、/guild home 及菜单中的设家项）。默认关闭。 */
    public static boolean enableGuildHome = false;
    public static int guildHomeTeleportDelaySeconds = 6;
    public static boolean cancelTeleportOnMove = true;
    public static boolean cancelTeleportOnDamage = true;

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            GuildsConfig.saveDefault();
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE);){
            JsonObject json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
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
        }
        catch (Exception e) {
            System.err.println("[GuildConfig] Failed to read config. Using defaults.");
            e.printStackTrace();
        }
        System.out.println("[GuildConfig] Loaded enable_Essentials_Command_Guild_Prefix = " + enableEssentialsCommandGuildPrefix);
        System.out.println("[GuildConfig] Loaded enable_HungerBound_Integration = " + enableHungerBoundIntegration);
        System.out.println("[GuildConfig] Loaded enable_Guild_Bank = " + enableGuildBank);
        System.out.println("[GuildConfig] Loaded enable_Guild_Home = " + enableGuildHome);
        System.out.println("[GuildConfig] Loaded guild_Home_Teleport_Delay_Seconds = " + guildHomeTeleportDelaySeconds);
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

