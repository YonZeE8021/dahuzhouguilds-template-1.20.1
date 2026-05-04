/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.serialization.DynamicOps
 *  net.minecraft.ItemStack
 *  net.minecraft.NbtCompound
 *  net.minecraft.NbtOps
 *  net.minecraft.StringNbtReader
 *  net.minecraft.WorldSavePath
 *  net.minecraft.server.MinecraftServer
 */
package net.demoniconpc.guildsremasteredmod.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.demoniconpc.guildsremasteredmod.util.GuildBankInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.MinecraftServer;

public class GuildBankManager {
    private static final int BANK_SIZE = 54;
    private static final Map<UUID, Integer> playerCurrentPageMap = new HashMap<UUID, Integer>();
    public static final Map<String, List<GuildBankInventory>> BANK_CACHE = new HashMap<String, List<GuildBankInventory>>();

    public static Path getBankFilePath(MinecraftServer server, String shortId) {
        if (shortId.startsWith("guild_")) {
            shortId = shortId.substring("guild_".length());
        }
        return server.getSavePath(WorldSavePath.ROOT).resolve("guilds").resolve("guild_" + shortId + "_bank.json");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static List<GuildBankInventory> getOrLoadBankPages(MinecraftServer server, String shortId) {
        Map<String, List<GuildBankInventory>> map = BANK_CACHE;
        synchronized (map) {
            if (!BANK_CACHE.containsKey(shortId)) {
                List<GuildBankInventory> pages = GuildBankManager.loadBankPages(server, shortId);
                System.out.println("Loading bank pages for guild " + shortId + ". Number of pages: " + pages.size());
                pages.forEach(page -> page.setOnChanged(() -> GuildBankManager.saveAllPages(server, shortId, pages)));
                BANK_CACHE.put(shortId, pages);
            }
            System.out.println("Returning bank pages for guild " + shortId + ". Number of pages: " + BANK_CACHE.get(shortId).size());
            return BANK_CACHE.get(shortId);
        }
    }

    public static int getPlayerCurrentPage(UUID playerUUID) {
        return playerCurrentPageMap.getOrDefault(playerUUID, 0);
    }

    public static void setPlayerCurrentPage(UUID playerUUID, int currentPage) {
        playerCurrentPageMap.put(playerUUID, currentPage);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static List<GuildBankInventory> createBank(MinecraftServer server, String shortId) {
        Map<String, List<GuildBankInventory>> map = BANK_CACHE;
        synchronized (map) {
            ArrayList<GuildBankInventory> emptyPages = new ArrayList<GuildBankInventory>();
            for (int i = 0; i < 9; ++i) {
                GuildBankInventory emptyBank = new GuildBankInventory();
                emptyPages.add(emptyBank);
            }
            emptyPages.forEach(page -> page.setOnChanged(() -> {
                try {
                    GuildBankManager.saveAllPages(server, shortId, emptyPages);
                }
                catch (Exception e) {
                    System.err.println("[GuildBankManager] Failed to save bank on change for " + shortId);
                    e.printStackTrace();
                }
            }));
            BANK_CACHE.put(shortId, emptyPages);
            GuildBankManager.saveAllPages(server, shortId, emptyPages);
            return emptyPages;
        }
    }

    public static void saveAllPages(MinecraftServer server, String shortId, List<GuildBankInventory> pages) {
        Path file = GuildBankManager.getBankFilePath(server, shortId);
        JsonArray pagesArray = new JsonArray();
        for (int pageIndex = 0; pageIndex < pages.size(); ++pageIndex) {
            GuildBankInventory inventory = pages.get(pageIndex);
            JsonObject pageObject = new JsonObject();
            pageObject.addProperty("page", (Number)(pageIndex + 1));
            JsonArray jsonItems = new JsonArray();
            for (int slot = 0; slot < 54; ++slot) {
                ItemStack stack;
                if (slot >= 45 || (stack = inventory.getStack(slot)) == null || stack.isEmpty()) continue;
                NbtCompound nbt = ItemStack.CODEC.encodeStart((DynamicOps)NbtOps.INSTANCE, (Object)stack).resultOrPartial(System.err::println).orElse(new NbtCompound());
                JsonObject entry = new JsonObject();
                entry.addProperty("slot", (Number)slot);
                entry.addProperty("item", nbt.toString());
                jsonItems.add((JsonElement)entry);
            }
            pageObject.add("items", (JsonElement)jsonItems);
            pagesArray.add((JsonElement)pageObject);
        }
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            try (FileWriter writer = new FileWriter(file.toFile());){
                JsonObject root = new JsonObject();
                root.add("pages", (JsonElement)pagesArray);
                new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement)root, (Appendable)writer);
            }
        }
        catch (IOException e) {
            System.err.println("[GuildBankManager] Failed to save all pages for " + shortId);
            e.printStackTrace();
        }
    }

    public static List<GuildBankInventory> loadBankPages(MinecraftServer server, String shortId) {
        ArrayList<GuildBankInventory> pages = new ArrayList<GuildBankInventory>();
        Path file = GuildBankManager.getBankFilePath(server, shortId);
        if (!Files.exists(file, new LinkOption[0])) {
            for (int i = 0; i < 9; ++i) {
                pages.add(new GuildBankInventory());
            }
            return pages;
        }
        try (FileReader reader = new FileReader(file.toFile());){
            JsonObject root = JsonParser.parseReader((Reader)reader).getAsJsonObject();
            JsonArray pagesArray = root.getAsJsonArray("pages");
            for (int i = 0; i < 9; ++i) {
                pages.add(new GuildBankInventory());
            }
            System.out.println("[GuildBankManager] Loading pages for guild " + shortId + ", total pages in file: " + pagesArray.size());
            for (JsonElement pageElem : pagesArray) {
                JsonObject pageObj = pageElem.getAsJsonObject();
                int pageNum = pageObj.get("page").getAsInt() - 1;
                if (pageNum < 0 || pageNum >= 9) continue;
                JsonArray jsonItems = pageObj.getAsJsonArray("items");
                Object[] items = new ItemStack[54];
                Arrays.fill(items, ItemStack.EMPTY);
                for (JsonElement el : jsonItems) {
                    JsonObject obj = el.getAsJsonObject();
                    int slot = obj.get("slot").getAsInt();
                    if (slot < 0 || slot >= 54) continue;
                    String snbt = obj.get("item").getAsString();
                    NbtCompound nbt = StringNbtReader.readCompound((String)snbt);
                    ItemStack stack = ItemStack.CODEC.parse((DynamicOps)NbtOps.INSTANCE, (Object)nbt).resultOrPartial(System.err::println).orElse(ItemStack.EMPTY);
                    items[slot] = stack;
                }
                pages.set(pageNum, new GuildBankInventory((ItemStack[])items));
            }
        }
        catch (Exception e) {
            System.err.println("[GuildBankManager] Failed to load bank pages for " + shortId);
            e.printStackTrace();
            pages.clear();
            for (int i = 0; i < 9; ++i) {
                pages.add(new GuildBankInventory());
            }
        }
        return pages;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void saveAll(MinecraftServer server) {
        Map<String, List<GuildBankInventory>> map = BANK_CACHE;
        synchronized (map) {
            for (Map.Entry<String, List<GuildBankInventory>> entry : BANK_CACHE.entrySet()) {
                GuildBankManager.saveAllPages(server, entry.getKey(), entry.getValue());
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void deleteBank(MinecraftServer server, String shortId) {
        Map<String, List<GuildBankInventory>> map = BANK_CACHE;
        synchronized (map) {
            BANK_CACHE.remove(shortId);
            Path file = GuildBankManager.getBankFilePath(server, shortId);
            try {
                Files.deleteIfExists(file);
            }
            catch (IOException e) {
                System.err.println("[GuildBankManager] Failed to delete bank file for " + shortId);
                e.printStackTrace();
            }
        }
    }
}

