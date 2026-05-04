package com.dahuzhou.dahuzhouguilds.util;

import com.dahuzhou.dahuzhouguilds.data.GuildBankManager;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GuildBankScreenHandler extends ScreenHandler {
	private static final int BANK_END = 54;
	private final List<GuildBankInventory> pages;
	private final PlayerInventory playerInventory;
	private int currentPage;
	private final Inventory pageSelectorInventory = new SimpleInventory(9);

	public GuildBankScreenHandler(int syncId, PlayerInventory playerInventory, List<GuildBankInventory> pages, int initialPage) {
		super(ScreenHandlerType.GENERIC_9X6, syncId);
		if (pages == null || pages.isEmpty()) {
			throw new IllegalArgumentException("Guild bank pages cannot be null or empty");
		}
		this.pages = pages;
		this.playerInventory = playerInventory;
		this.currentPage = initialPage;
		this.addSlotsForPage(this.currentPage);
		this.addPlayerSlots(playerInventory);
		this.refreshPageItems();
	}

	private void addSlotsForPage(int pageIndex) {
		GuildBankInventory currentInventory = this.pages.get(pageIndex);
		int slotIndex = 0;
		for (int row = 0; row < 6; ++row) {
			for (int col = 0; col < 9; ++col) {
				int x = 8 + col * 18;
				int y = 18 + row * 18;
				if (slotIndex >= 45 && slotIndex <= 53) {
					int selectorSlot = slotIndex - 45;
					this.addSlot(new Slot(this.pageSelectorInventory, selectorSlot, x, y) {
						@Override
						public boolean canInsert(ItemStack stack) {
							return false;
						}

						@Override
						public boolean canTakeItems(PlayerEntity player) {
							return false;
						}
					});
				} else {
					this.addSlot(new Slot(currentInventory, slotIndex, x, y));
				}
				++slotIndex;
			}
		}
	}

	private void addPlayerSlots(PlayerInventory playerInventory) {
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				int x = 8 + col * 18;
				int y = 140 + row * 18;
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
			}
		}
		for (int col = 0; col < 9; ++col) {
			int x = 8 + col * 18;
			int y = 198;
			this.addSlot(new Slot(playerInventory, col, x, y));
		}
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return this.pages.get(this.currentPage).canPlayerUse(player);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return ItemStack.EMPTY;
		}
		if (index >= 45 && index <= 53) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasStack()) {
			ItemStack slotStack = slot.getStack();
			stack = slotStack.copy();
			int bankEnd = BANK_END;
			int playerInvStart = bankEnd;
			int playerInvEnd = playerInvStart + 36;
			Guild guild = GuildDataManager.getGuildByPlayer(serverPlayer.getUuid());
			if (guild == null) {
				return ItemStack.EMPTY;
			}
			int page = this.currentPage + 1;
			boolean isBankSlot = index < bankEnd;
			boolean isPlayerSlot = index >= bankEnd;
			if (isBankSlot && !guild.hasRankPermission(serverPlayer.getUuid(), "canWithdrawTab" + page)) {
				serverPlayer.sendMessage(Text.literal("\u00a7cYou cannot withdraw from Tab " + page + "."), false);
				return ItemStack.EMPTY;
			}
			if (isPlayerSlot && !guild.hasRankPermission(serverPlayer.getUuid(), "canDepositTab" + page)) {
				serverPlayer.sendMessage(Text.literal("\u00a7cYou cannot deposit into Tab " + page + "."), false);
				return ItemStack.EMPTY;
			}
			if (isBankSlot ? !this.insertItem(slotStack, playerInvStart, playerInvEnd, true) : !this.insertItem(slotStack, 0, bankEnd, false)) {
				return ItemStack.EMPTY;
			}
			if (slotStack.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}
		}
		return stack;
	}

	private static void executeAsPlayer(ServerPlayerEntity player, String command) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		String cmd = command.startsWith("/") ? command.substring(1) : command;
		try {
			ParseResults<ServerCommandSource> parse = server.getCommandManager().getDispatcher().parse(new StringReader(cmd), player.getCommandSource());
			server.getCommandManager().getDispatcher().execute(parse);
		} catch (Exception e) {
			System.err.println("[GuildBankScreenHandler] Error executing command: " + cmd);
			e.printStackTrace();
		}
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			super.onSlotClick(slotIndex, button, actionType, player);
			return;
		}
		if (slotIndex >= 45 && slotIndex <= 53 && actionType == SlotActionType.PICKUP) {
			int newPage = slotIndex - 45;
			if (newPage >= 0 && newPage < this.pages.size() && newPage != this.currentPage) {
				this.savePageData(player, this.currentPage);
				executeAsPlayer(serverPlayer, "guild bank " + (newPage + 1));
			}
			return;
		}
		Slot clickedSlot = slotIndex >= 0 && slotIndex < this.slots.size() ? this.slots.get(slotIndex) : null;
		if (clickedSlot != null && clickedSlot.hasStack()) {
			boolean isBankSlot = slotIndex < 54;
			boolean isPlayerSlot = slotIndex >= 54;
			Guild guild = GuildDataManager.getGuildByPlayer(serverPlayer.getUuid());
			if (guild == null) {
				serverPlayer.sendMessage(Text.literal("\u00a7cYou are not in a guild."), false);
				return;
			}
			int page = this.currentPage + 1;
			if (isBankSlot && guild.getUnlockedBankTabs() < page) {
				serverPlayer.sendMessage(Text.literal("\u00a7cTab " + page + " is locked. You need to unlock it first by donating Netherite."), false);
				return;
			}
			if (isPlayerSlot && !guild.hasRankPermission(serverPlayer.getUuid(), "canDepositTab" + page)) {
				serverPlayer.sendMessage(Text.literal("\u00a7cYou do not have permission to deposit into Tab " + page + "."), false);
				return;
			}
			if (isBankSlot && !guild.hasRankPermission(serverPlayer.getUuid(), "canWithdrawTab" + page)) {
				serverPlayer.sendMessage(Text.literal("\u00a7cYou do not have permission to withdraw from Tab " + page + "."), false);
				return;
			}
		}
		super.onSlotClick(slotIndex, button, actionType, player);
	}

	private void savePageData(PlayerEntity player, int pageIndex) {
		if (pageIndex < 0 || pageIndex >= this.pages.size()) {
			return;
		}
		GuildBankInventory inventory = this.pages.get(pageIndex);
		for (int i = 0; i < 45; ++i) {
			ItemStack item = this.slots.get(i).getStack().copy();
			inventory.setStack(i, item);
		}
		MinecraftServer server = player.getServer();
		if (server == null) {
			System.err.println("Error: Server could not be retrieved from player.");
			return;
		}
		Guild g = GuildDataManager.getGuildByPlayer(player.getUuid());
		if (g == null) {
			System.err.println("Error: Player does not belong to any guild.");
			return;
		}
		GuildBankManager.saveAllPages(server, g.getShortenedId(), this.pages);
	}

	private void refreshPageItems() {
		GuildBankInventory currentInventory = this.pages.get(this.currentPage);
		for (int i = 0; i < 45; ++i) {
			Slot slot = this.slots.get(i);
			slot.setStack(currentInventory.getStack(i));
		}
		for (int i = 45; i <= 53; ++i) {
			int pageNum = i - 45 + 1;
			boolean isCurrent = pageNum == this.currentPage + 1;
			this.pageSelectorInventory.setStack(i - 45, this.createPagePane(pageNum, isCurrent));
		}
	}

	private ItemStack createPagePane(int pageNumber, boolean isCurrentPage) {
		ServerPlayerEntity player = (ServerPlayerEntity) this.playerInventory.player;
		Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
		if (guild == null) {
			return new ItemStack(Items.BARRIER);
		}
		int unlockedTabs = guild.getUnlockedBankTabs();
		ItemStack pane;
		if (isCurrentPage) {
			pane = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
			MutableText displayName = Text.literal("Page " + pageNumber + " (Current)").styled(s -> s.withColor(Formatting.GREEN).withBold(true));
			pane.setCustomName(displayName);
		} else if (pageNumber <= unlockedTabs) {
			pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
			MutableText displayName = Text.literal("Page " + pageNumber).styled(s -> s.withColor(Formatting.GRAY));
			pane.setCustomName(displayName);
		} else {
			pane = new ItemStack(Items.RED_STAINED_GLASS_PANE);
			MutableText displayName = Text.literal("Page " + pageNumber + " (LOCKED)").styled(s -> s.withColor(Formatting.RED).withItalic(true));
			pane.setCustomName(displayName);
		}
		return pane;
	}
}
