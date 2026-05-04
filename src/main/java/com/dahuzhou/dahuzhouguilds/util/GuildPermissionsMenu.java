/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Inventory
 *  net.minecraft.SimpleInventory
 *  net.minecraft.PlayerEntity
 *  net.minecraft.PlayerInventory
 *  net.minecraft.GenericContainerScreenHandler
 *  net.minecraft.SlotActionType
 *  net.minecraft.Item
 *  net.minecraft.ItemStack
 *  net.minecraft.Items
 *  net.minecraft.ItemConvertible
 *  net.minecraft.Text
 *  net.minecraft.ServerPlayerEntity
 *  net.minecraft.NamedScreenHandlerFactory
 *  net.minecraft.ScreenHandlerType
 *  net.minecraft.SimpleNamedScreenHandlerFactory
 *  net.minecraft.DataComponentTypes
 */
package com.dahuzhou.dahuzhouguilds.util;

import java.util.List;
import com.dahuzhou.dahuzhouguilds.util.GuildBankRankEditorMenu;
import com.dahuzhou.dahuzhouguilds.util.GuildMembersMenu;
import com.dahuzhou.dahuzhouguilds.util.GuildRankEditorMenu;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;

public class GuildPermissionsMenu {
    public static final List<PositionedMenuItem> MENU_ITEMS = List.of(new PositionedMenuItem(10, GuildPermissionsMenu.menuItem(Items.BOOK, "\u00a7eGuild Info", "/guild info")), new PositionedMenuItem(12, GuildPermissionsMenu.menuItem(Items.CHEST, "\u00a7bGuild Bank", "/guild bank 1")), new PositionedMenuItem(14, GuildPermissionsMenu.menuItem(Items.WHITE_BED, "\u00a7dSet Guild Home", "/guild sethome")), new PositionedMenuItem(16, GuildPermissionsMenu.menuItem(Items.ORANGE_DYE, "\u00a76Toggle Friendly Fire", "/guild toggle_friendlyfire")), new PositionedMenuItem(28, GuildPermissionsMenu.menuItem(Items.PLAYER_HEAD, "\u00a7aManage Members", null)), new PositionedMenuItem(30, GuildPermissionsMenu.menuItem(Items.WRITABLE_BOOK, "\u00a7bEdit Ranks", null)), new PositionedMenuItem(32, GuildPermissionsMenu.menuItem(Items.ENDER_CHEST, "\u00a79Bank Permissions", null)), new PositionedMenuItem(34, GuildPermissionsMenu.menuItem(Items.BARRIER, "\u00a7cDisband Guild", "/guild disband")));

    public static MenuItem menuItem(Item item, String displayName, String command) {
        ItemStack stack = new ItemStack((ItemConvertible)item);
        stack.setCustomName(Text.literal(displayName));
        return new MenuItem(stack, command);
    }

    public static void openForPlayer(ServerPlayerEntity player) {
        int size = 54;
        SimpleInventory inventory = new SimpleInventory(size);
        for (PositionedMenuItem pm : MENU_ITEMS) {
            if (pm.slot < 0 || pm.slot >= size) continue;
            inventory.setStack(pm.slot, pm.item.icon.copy());
        }
        player.openHandledScreen((NamedScreenHandlerFactory)new SimpleNamedScreenHandlerFactory((syncId, playerInv, playerEntity) -> new GuildPermissionsMenuHandler(syncId, playerInv, inventory), (Text)Text.literal((String)"Guild Menu")));
    }

    public static class MenuItem {
        public final ItemStack icon;
        public final String command;

        public MenuItem(ItemStack icon, String command) {
            this.icon = icon;
            this.command = command;
        }
    }

    public static class PositionedMenuItem {
        public final int slot;
        public final MenuItem item;

        public PositionedMenuItem(int slot, MenuItem item) {
            this.slot = slot;
            this.item = item;
        }
    }

    public static class GuildPermissionsMenuHandler
    extends GenericContainerScreenHandler {
        private final SimpleInventory menuInventory;

        public GuildPermissionsMenuHandler(int syncId, PlayerInventory playerInventory, SimpleInventory menuInventory) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, (Inventory)menuInventory, 6);
            this.menuInventory = menuInventory;
        }

        public boolean canUse(PlayerEntity player) {
            return true;
        }

        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
            if (actionType == SlotActionType.PICKUP) {
                for (PositionedMenuItem pm : MENU_ITEMS) {
                    if (pm.slot != slot || !(player instanceof ServerPlayerEntity)) continue;
                    ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
                    serverPlayer.closeHandledScreen();
                    String label = pm.item.icon.getName().getString();
                    if (pm.item.command == null) {
                        if (label.contains("Manage Members")) {
                            GuildMembersMenu.openForPlayer(serverPlayer);
                        } else if (label.contains("Edit Ranks")) {
                            GuildRankEditorMenu.openForPlayer(serverPlayer);
                        } else if (label.contains("Bank Permissions")) {
                            GuildBankRankEditorMenu.openForPlayer(serverPlayer);
                        }
                    } else {
                        String cmd = pm.item.command.startsWith("/") ? pm.item.command.substring(1) : pm.item.command;
                        serverPlayer.getServer().getCommandManager().executeWithPrefix(serverPlayer.getCommandSource(), cmd);
                    }
                    return;
                }
            }
        }
    }
}

