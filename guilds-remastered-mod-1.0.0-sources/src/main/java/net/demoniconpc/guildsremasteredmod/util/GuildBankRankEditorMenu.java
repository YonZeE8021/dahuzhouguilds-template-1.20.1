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
package net.demoniconpc.guildsremasteredmod.util;

import java.util.HashMap;
import java.util.Map;
import net.demoniconpc.guildsremasteredmod.util.GuildBankRankPermissionsMenu;
import net.demoniconpc.guildsremasteredmod.util.GuildPermissionsMenu;
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
import net.minecraft.component.DataComponentTypes;

public class GuildBankRankEditorMenu {
    public static Map<String, Boolean> getDefaultPermissions(String rankName) {
        HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();
        if (rankName.equalsIgnoreCase("Guild Master")) {
            permissions.put("canUseBankTab1", true);
            permissions.put("canDepositTab1", true);
            permissions.put("canWithdrawTab1", true);
            permissions.put("canUseBankTab2", true);
            permissions.put("canDepositTab2", true);
            permissions.put("canWithdrawTab2", true);
            permissions.put("canUseBankTab3", true);
            permissions.put("canDepositTab3", true);
            permissions.put("canWithdrawTab3", true);
            permissions.put("canUseBankTab4", true);
            permissions.put("canDepositTab4", true);
            permissions.put("canWithdrawTab4", true);
            permissions.put("canUseBankTab5", true);
            permissions.put("canDepositTab5", true);
            permissions.put("canWithdrawTab5", true);
            permissions.put("canUseBankTab6", true);
            permissions.put("canDepositTab6", true);
            permissions.put("canWithdrawTab6", true);
            permissions.put("canUseBankTab7", true);
            permissions.put("canDepositTab7", true);
            permissions.put("canWithdrawTab7", true);
            permissions.put("canUseBankTab8", true);
            permissions.put("canDepositTab8", true);
            permissions.put("canWithdrawTab8", true);
            permissions.put("canUseBankTab9", true);
            permissions.put("canDepositTab9", true);
            permissions.put("canWithdrawTab9", true);
        } else {
            permissions.put("canUseBankTab1", false);
            permissions.put("canDepositTab1", false);
            permissions.put("canWithdrawTab1", false);
            permissions.put("canUseBankTab2", false);
            permissions.put("canDepositTab2", false);
            permissions.put("canWithdrawTab2", false);
            permissions.put("canUseBankTab3", false);
            permissions.put("canDepositTab3", false);
            permissions.put("canWithdrawTab3", false);
            permissions.put("canUseBankTab4", false);
            permissions.put("canDepositTab4", false);
            permissions.put("canWithdrawTab4", false);
            permissions.put("canUseBankTab5", false);
            permissions.put("canDepositTab5", false);
            permissions.put("canWithdrawTab5", false);
            permissions.put("canUseBankTab6", false);
            permissions.put("canDepositTab6", false);
            permissions.put("canWithdrawTab6", false);
            permissions.put("canUseBankTab7", false);
            permissions.put("canDepositTab7", false);
            permissions.put("canWithdrawTab7", false);
            permissions.put("canUseBankTab8", false);
            permissions.put("canDepositTab8", false);
            permissions.put("canWithdrawTab8", false);
            permissions.put("canUseBankTab9", false);
            permissions.put("canDepositTab9", false);
            permissions.put("canWithdrawTab9", false);
        }
        return permissions;
    }

    public static MenuItem menuItem(Item item, String displayName, String command) {
        ItemStack stack = new ItemStack((ItemConvertible)item);
        stack.set(DataComponentTypes.field_49631, (Object)Text.literal((String)displayName));
        return new MenuItem(stack, command);
    }

    public static void openForPlayer(ServerPlayerEntity player) {
        int size = 27;
        SimpleInventory inventory = new SimpleInventory(size);
        inventory.setStack(10, GuildBankRankEditorMenu.rankItem("Guild Master", Items.field_8137));
        inventory.setStack(12, GuildBankRankEditorMenu.rankItem("Officer", Items.field_8802));
        inventory.setStack(14, GuildBankRankEditorMenu.rankItem("Member", Items.field_8371));
        inventory.setStack(16, GuildBankRankEditorMenu.rankItem("Initiate", Items.field_8091));
        inventory.setStack(26, GuildPermissionsMenu.menuItem((Item)Items.field_8107, (String)"Back", null).icon.copy());
        player.openHandledScreen((NamedScreenHandlerFactory)new SimpleNamedScreenHandlerFactory((syncId, playerInv, playerEntity) -> new GuildBankRankEditorMenuHandler(syncId, playerInv, inventory), (Text)Text.literal((String)"Guild Bank Rank Editor")));
    }

    public static ItemStack rankItem(String rankName, Item icon) {
        ItemStack stack = new ItemStack((ItemConvertible)icon);
        stack.set(DataComponentTypes.field_49631, (Object)Text.literal((String)("\u00a7a" + rankName)));
        return stack;
    }

    public static class MenuItem {
        public final ItemStack icon;
        public final String command;

        public MenuItem(ItemStack icon, String command) {
            this.icon = icon;
            this.command = command;
        }
    }

    public static class GuildBankRankEditorMenuHandler
    extends GenericContainerScreenHandler {
        private final SimpleInventory menuInventory;

        public GuildBankRankEditorMenuHandler(int syncId, PlayerInventory playerInventory, SimpleInventory menuInventory) {
            super(ScreenHandlerType.field_17326, syncId, playerInventory, (Inventory)menuInventory, 3);
            this.menuInventory = menuInventory;
        }

        public boolean canUse(PlayerEntity player) {
            return true;
        }

        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
            if (actionType == SlotActionType.PICKUP && player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
                if (slot == 26) {
                    GuildPermissionsMenu.openForPlayer(serverPlayer);
                    return;
                }
                String rankName = null;
                if (slot == 10) {
                    rankName = "Guild Master";
                }
                if (slot == 12) {
                    rankName = "Officer";
                }
                if (slot == 14) {
                    rankName = "Member";
                }
                if (slot == 16) {
                    rankName = "Initiate";
                }
                if (rankName != null) {
                    if (rankName.equalsIgnoreCase("Guild Master")) {
                        serverPlayer.sendMessage((Text)Text.literal((String)"You cannot modify the Guild Master's permissions."), false);
                        return;
                    }
                    GuildBankRankPermissionsMenu.openForPlayer(serverPlayer, rankName);
                }
            }
        }
    }
}

