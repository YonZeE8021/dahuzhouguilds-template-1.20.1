/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Formatting
 *  net.minecraft.Inventory
 *  net.minecraft.SimpleInventory
 *  net.minecraft.PlayerEntity
 *  net.minecraft.PlayerInventory
 *  net.minecraft.ScreenHandler
 *  net.minecraft.SlotActionType
 *  net.minecraft.Slot
 *  net.minecraft.ItemStack
 *  net.minecraft.Items
 *  net.minecraft.ItemConvertible
 *  net.minecraft.Text
 *  net.minecraft.ServerPlayerEntity
 *  net.minecraft.ScreenHandlerType
 *  net.minecraft.MutableText
 *  net.minecraft.DataComponentTypes
 *  net.minecraft.server.MinecraftServer
 */
package net.demoniconpc.guildsremasteredmod.util;

import java.util.List;
import net.demoniconpc.guildsremasteredmod.data.GuildBankManager;
import net.demoniconpc.guildsremasteredmod.data.GuildDataManager;
import net.demoniconpc.guildsremasteredmod.guild.Guild;
import net.demoniconpc.guildsremasteredmod.util.GuildBankInventory;
import net.minecraft.util.Formatting;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.MutableText;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.server.MinecraftServer;

public class GuildBankScreenHandler
extends ScreenHandler {
    private static final int BANK_SIZE = 54;
    private static final int LOCKED_START = 45;
    private static final int LOCKED_END = 53;
    private final List<GuildBankInventory> pages;
    private final PlayerInventory playerInventory;
    private int currentPage;
    private final Inventory pageSelectorInventory = new SimpleInventory(9);

    public GuildBankScreenHandler(int syncId, PlayerInventory playerInventory, List<GuildBankInventory> pages, int initialPage) {
        super(ScreenHandlerType.field_17327, syncId);
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
                    this.addSlot(new Slot(this, this.pageSelectorInventory, selectorSlot, x, y){

                        public boolean canInsert(ItemStack stack) {
                            return false;
                        }

                        public boolean canTakeItems(PlayerEntity player) {
                            return false;
                        }
                    });
                } else {
                    this.addSlot(new Slot((Inventory)currentInventory, slotIndex, x, y));
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
                this.addSlot(new Slot((Inventory)playerInventory, col + row * 9 + 9, x, y));
            }
        }
        for (int col = 0; col < 9; ++col) {
            int x = 8 + col * 18;
            int y = 198;
            this.addSlot(new Slot((Inventory)playerInventory, col, x, y));
        }
    }

    public boolean canUse(PlayerEntity player) {
        return this.pages.get(this.currentPage).canPlayerUse(player);
    }

    public ItemStack quickMove(PlayerEntity player, int index) {
        if (!(player instanceof ServerPlayerEntity)) {
            return ItemStack.EMPTY;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
        if (index >= 45 && index <= 53) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = (Slot)this.field_HangingSignBlockEntityRenderer.get(index);
        if (slot != null && slot.hasStack()) {
            boolean isPlayerSlot;
            int bankEnd;
            ItemStack slotStack = slot.getStack();
            stack = slotStack.copy();
            int playerInvStart = bankEnd = 54;
            int playerInvEnd = playerInvStart + 36;
            Guild guild = GuildDataManager.getGuildByPlayer(serverPlayer.getUuid());
            if (guild == null) {
                return ItemStack.EMPTY;
            }
            int page = this.currentPage + 1;
            boolean isBankSlot = index < bankEnd;
            boolean bl = isPlayerSlot = index >= bankEnd;
            if (isBankSlot && !guild.hasRankPermission(serverPlayer.getUuid(), "canWithdrawTab" + page)) {
                serverPlayer.sendMessage((Text)Text.literal((String)("\u00a7cYou cannot withdraw from Tab " + page + ".")), false);
                return ItemStack.EMPTY;
            }
            if (isPlayerSlot && !guild.hasRankPermission(serverPlayer.getUuid(), "canDepositTab" + page)) {
                serverPlayer.sendMessage((Text)Text.literal((String)("\u00a7cYou cannot deposit into Tab " + page + ".")), false);
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

    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        Slot clickedSlot;
        if (!(player instanceof ServerPlayerEntity)) {
            super.onSlotClick(slotIndex, button, actionType, player);
            return;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
        if (slotIndex >= 45 && slotIndex <= 53 && actionType == SlotActionType.PICKUP) {
            int newPage = slotIndex - 45;
            if (newPage >= 0 && newPage < this.pages.size() && newPage != this.currentPage) {
                this.savePageData(player, this.currentPage);
                String command = "guild bank " + (newPage + 1);
                try {
                    serverPlayer.getServer().getCommandManager().getDispatcher().execute(command, (Object)serverPlayer.getCommandSource());
                }
                catch (Exception e) {
                    System.err.println("[GuildBankScreenHandler] Error executing command: " + command);
                    e.printStackTrace();
                }
            }
            return;
        }
        Slot class_17352 = clickedSlot = slotIndex >= 0 && slotIndex < this.field_HangingSignBlockEntityRenderer.size() ? (Slot)this.field_HangingSignBlockEntityRenderer.get(slotIndex) : null;
        if (clickedSlot != null && clickedSlot.hasStack()) {
            boolean isBankSlot = slotIndex < 54;
            boolean isPlayerSlot = slotIndex >= 54;
            Guild guild = GuildDataManager.getGuildByPlayer(serverPlayer.getUuid());
            if (guild == null) {
                serverPlayer.sendMessage((Text)Text.literal((String)"\u00a7cYou are not in a guild."), false);
                return;
            }
            int page = this.currentPage + 1;
            if (isBankSlot && guild.getUnlockedBankTabs() < page) {
                serverPlayer.sendMessage((Text)Text.literal((String)("\u00a7cTab " + page + " is locked. You need to unlock it first by donating Netherite.")), false);
                return;
            }
            if (isPlayerSlot && !guild.hasRankPermission(serverPlayer.getUuid(), "canDepositTab" + page)) {
                serverPlayer.sendMessage((Text)Text.literal((String)("\u00a7cYou do not have permission to deposit into Tab " + page + ".")), false);
                return;
            }
            if (isBankSlot && !guild.hasRankPermission(serverPlayer.getUuid(), "canWithdrawTab" + page)) {
                serverPlayer.sendMessage((Text)Text.literal((String)("\u00a7cYou do not have permission to withdraw from Tab " + page + ".")), false);
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
            ItemStack item = ((Slot)this.field_HangingSignBlockEntityRenderer.get(i)).getStack().copy();
            inventory.setStack(i, item);
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            System.err.println("Error: Server could not be retrieved from player.");
            return;
        }
        String guildId = GuildDataManager.getGuildByPlayer(player.getUuid()).getShortenedId();
        if (guildId == null) {
            System.err.println("Error: Player does not belong to any guild.");
            return;
        }
        GuildBankManager.saveAllPages(server, guildId, this.pages);
    }

    private void refreshPageItems() {
        int i;
        GuildBankInventory currentInventory = this.pages.get(this.currentPage);
        for (i = 0; i < 45; ++i) {
            Slot slot = (Slot)this.field_HangingSignBlockEntityRenderer.get(i);
            slot.setStack(currentInventory.getStack(i));
        }
        for (i = 45; i <= 53; ++i) {
            int pageNum = i - 45 + 1;
            boolean isCurrent = pageNum == this.currentPage + 1;
            this.pageSelectorInventory.setStack(i - 45, this.createPagePane(pageNum, isCurrent));
        }
    }

    private ItemStack createPagePane(int pageNumber, boolean isCurrentPage) {
        ItemStack pane;
        ServerPlayerEntity player = (ServerPlayerEntity)this.playerInventory.player;
        Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
        if (guild == null) {
            return new ItemStack((ItemConvertible)Items.BARRIER);
        }
        int unlockedTabs = guild.getUnlockedBankTabs();
        if (isCurrentPage) {
            pane = new ItemStack((ItemConvertible)Items.GREEN_STAINED_GLASS_PANE);
            MutableText displayName = Text.literal((String)("Page " + pageNumber + " (Current)")).styled(s -> s.withColor(Formatting.field_1060).withBold(Boolean.valueOf(true)));
            pane.set(DataComponentTypes.field_49631, (Object)displayName);
        } else if (pageNumber <= unlockedTabs) {
            pane = new ItemStack((ItemConvertible)Items.BLACK_STAINED_GLASS_PANE);
            MutableText displayName = Text.literal((String)("Page " + pageNumber)).styled(s -> s.withColor(Formatting.field_1080));
            pane.set(DataComponentTypes.field_49631, (Object)displayName);
        } else {
            pane = new ItemStack((ItemConvertible)Items.RED_STAINED_GLASS_PANE);
            MutableText displayName = Text.literal((String)("Page " + pageNumber + " (LOCKED)")).styled(s -> s.withColor(Formatting.field_1061).withItalic(Boolean.valueOf(true)));
            pane.set(DataComponentTypes.field_49631, (Object)displayName);
        }
        return pane;
    }
}

