package com.dahuzhou.dahuzhouguilds.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.dahuzhou.dahuzhouguilds.util.GuildBankInventory;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public class GuildBankManager {
	private static final Map<UUID, Integer> playerCurrentPageMap = new HashMap<>();
	public static final Map<String, List<GuildBankInventory>> BANK_CACHE = new HashMap<>();

	public static Path getBankFilePath(MinecraftServer server, String shortId) {
		if (shortId.startsWith("guild_")) {
			shortId = shortId.substring("guild_".length());
		}
		return server.getSavePath(WorldSavePath.ROOT).resolve("guilds").resolve("guild_" + shortId + "_bank.json");
	}

	public static List<GuildBankInventory> getOrLoadBankPages(MinecraftServer server, String shortId) {
		synchronized (BANK_CACHE) {
			if (!BANK_CACHE.containsKey(shortId)) {
				List<GuildBankInventory> pages = GuildBankManager.loadBankPages(server, shortId);
				pages.forEach(page -> page.setOnChanged(() -> GuildBankManager.saveAllPages(server, shortId, pages)));
				BANK_CACHE.put(shortId, pages);
			}
			return BANK_CACHE.get(shortId);
		}
	}

	public static int getPlayerCurrentPage(UUID playerUUID) {
		return playerCurrentPageMap.getOrDefault(playerUUID, 0);
	}

	public static void setPlayerCurrentPage(UUID playerUUID, int currentPage) {
		playerCurrentPageMap.put(playerUUID, currentPage);
	}

	public static List<GuildBankInventory> createBank(MinecraftServer server, String shortId) {
		synchronized (BANK_CACHE) {
			ArrayList<GuildBankInventory> emptyPages = new ArrayList<>();
			for (int i = 0; i < 9; ++i) {
				emptyPages.add(new GuildBankInventory());
			}
			emptyPages.forEach(page -> page.setOnChanged(() -> {
				try {
					GuildBankManager.saveAllPages(server, shortId, emptyPages);
				} catch (Exception e) {
					System.err.println("[GuildBankManager] Failed to save bank on change for " + shortId);
					e.printStackTrace();
				}
			}));
			BANK_CACHE.put(shortId, emptyPages);
			GuildBankManager.saveAllPages(server, shortId, emptyPages);
			return emptyPages;
		}
	}

	private static NbtCompound stackToNbt(ItemStack stack) {
		NbtCompound nbt = new NbtCompound();
		stack.writeNbt(nbt);
		return nbt;
	}

	private static ItemStack stackFromNbt(NbtCompound nbt) {
		try {
			return ItemStack.fromNbt(nbt);
		} catch (RuntimeException e) {
			System.err.println("[GuildBankManager] Failed to parse item NBT");
			e.printStackTrace();
			return ItemStack.EMPTY;
		}
	}

	public static void saveAllPages(MinecraftServer server, String shortId, List<GuildBankInventory> pages) {
		Path file = GuildBankManager.getBankFilePath(server, shortId);
		JsonArray pagesArray = new JsonArray();
		for (int pageIndex = 0; pageIndex < pages.size(); ++pageIndex) {
			GuildBankInventory inventory = pages.get(pageIndex);
			JsonObject pageObject = new JsonObject();
			pageObject.addProperty("page", pageIndex + 1);
			JsonArray jsonItems = new JsonArray();
			for (int slot = 0; slot < 54; ++slot) {
				if (slot >= 45) {
					continue;
				}
				ItemStack stack = inventory.getStack(slot);
				if (stack == null || stack.isEmpty()) {
					continue;
				}
				NbtCompound nbt = stackToNbt(stack);
				JsonObject entry = new JsonObject();
				entry.addProperty("slot", slot);
				entry.addProperty("item", nbt.toString());
				jsonItems.add(entry);
			}
			pageObject.add("items", jsonItems);
			pagesArray.add(pageObject);
		}
		try {
			Files.createDirectories(file.getParent());
			try (FileWriter writer = new FileWriter(file.toFile())) {
				JsonObject root = new JsonObject();
				root.add("pages", pagesArray);
				new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
			}
		} catch (IOException e) {
			System.err.println("[GuildBankManager] Failed to save all pages for " + shortId);
			e.printStackTrace();
		}
	}

	public static List<GuildBankInventory> loadBankPages(MinecraftServer server, String shortId) {
		ArrayList<GuildBankInventory> pages = new ArrayList<>();
		Path file = GuildBankManager.getBankFilePath(server, shortId);
		if (!Files.exists(file)) {
			for (int i = 0; i < 9; ++i) {
				pages.add(new GuildBankInventory());
			}
			return pages;
		}
		try (FileReader reader = new FileReader(file.toFile())) {
			JsonObject root = JsonParser.parseReader((Reader) reader).getAsJsonObject();
			JsonArray pagesArray = root.getAsJsonArray("pages");
			for (int i = 0; i < 9; ++i) {
				pages.add(new GuildBankInventory());
			}
			for (JsonElement pageElem : pagesArray) {
				JsonObject pageObj = pageElem.getAsJsonObject();
				int pageNum = pageObj.get("page").getAsInt() - 1;
				if (pageNum < 0 || pageNum >= 9) {
					continue;
				}
				JsonArray jsonItems = pageObj.getAsJsonArray("items");
				ItemStack[] items = new ItemStack[54];
				Arrays.fill(items, ItemStack.EMPTY);
				for (JsonElement el : jsonItems) {
					JsonObject obj = el.getAsJsonObject();
					int slot = obj.get("slot").getAsInt();
					if (slot < 0 || slot >= 54) {
						continue;
					}
					String snbt = obj.get("item").getAsString();
					NbtCompound nbt = StringNbtReader.parse(snbt);
					items[slot] = stackFromNbt(nbt);
				}
				pages.set(pageNum, new GuildBankInventory(items));
			}
		} catch (Exception e) {
			System.err.println("[GuildBankManager] Failed to load bank pages for " + shortId);
			e.printStackTrace();
			pages.clear();
			for (int i = 0; i < 9; ++i) {
				pages.add(new GuildBankInventory());
			}
		}
		return pages;
	}

	public static void saveAll(MinecraftServer server) {
		synchronized (BANK_CACHE) {
			for (Map.Entry<String, List<GuildBankInventory>> entry : BANK_CACHE.entrySet()) {
				GuildBankManager.saveAllPages(server, entry.getKey(), entry.getValue());
			}
		}
	}

	public static void deleteBank(MinecraftServer server, String shortId) {
		synchronized (BANK_CACHE) {
			BANK_CACHE.remove(shortId);
			Path file = GuildBankManager.getBankFilePath(server, shortId);
			try {
				Files.deleteIfExists(file);
			} catch (IOException e) {
				System.err.println("[GuildBankManager] Failed to delete bank file for " + shortId);
				e.printStackTrace();
			}
		}
	}
}
