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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.GuildBankRankEditorMenu;
import com.dahuzhou.dahuzhouguilds.util.GuildPermissionsMenu;
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

public class GuildBankRankPermissionsMenu {
    private static final List<String> SUPPORTED_PERMISSIONS = Arrays.asList("canUseBankTab1", "canDepositTab1", "canWithdrawTab1", "canUseBankTab2", "canDepositTab2", "canWithdrawTab2", "canUseBankTab3", "canDepositTab3", "canWithdrawTab3", "canUseBankTab4", "canDepositTab4", "canWithdrawTab4", "canUseBankTab5", "canDepositTab5", "canWithdrawTab5", "canUseBankTab6", "canDepositTab6", "canWithdrawTab6", "canUseBankTab7", "canDepositTab7", "canWithdrawTab7", "canUseBankTab8", "canDepositTab8", "canWithdrawTab8", "canUseBankTab9", "canDepositTab9", "canWithdrawTab9");
    private static final int[] PERMISSION_SLOTS = new int[]{0, 1, 2, 6, 7, 8, 9, 10, 11, 15, 16, 17, 18, 19, 20, 24, 25, 26, 27, 28, 29, 33, 34, 35, 36, 37, 38};

    public static void openForPlayer(ServerPlayerEntity player, String rankName) {
        int size = 54;
        SimpleInventory inventory = new SimpleInventory(size);
        Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
        if (guild == null) {
            player.sendMessage((Text)Text.literal((String)"\u00a7cYou must be in a guild to edit bank permissions."), false);
            return;
        }
        Map<String, Boolean> rankPermissions = guild.getRankPermissions(rankName);
        for (int i = 0; i < SUPPORTED_PERMISSIONS.size(); ++i) {
            String permission = SUPPORTED_PERMISSIONS.get(i);
            int slot = PERMISSION_SLOTS[i];
            boolean enabled = rankPermissions.getOrDefault(permission, false);
            inventory.setStack(slot, GuildBankRankPermissionsMenu.toggleItem(permission, enabled));
        }
        inventory.setStack(53, GuildPermissionsMenu.menuItem((Item)Items.ARROW, (String)"Back", null).icon.copy());
        player.openHandledScreen((NamedScreenHandlerFactory)new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerEntity) -> new GuildBankRankPermissionsHandler(syncId, playerInventory, inventory, rankName, guild), (Text)Text.literal((String)("Bank Permissions: " + rankName))));
    }

    private static ItemStack toggleItem(String permission, boolean enabled) {
        ItemStack stack = new ItemStack((ItemConvertible)(enabled ? Items.LIME_DYE : Items.GRAY_DYE));
        stack.setCustomName(Text.literal((enabled ? "\u00a7a" : "\u00a77") + GuildBankRankPermissionsMenu.getLabel(permission)));
        return stack;
    }

    private static String getLabel(String permissionKey) {
        return switch (permissionKey) {
            case "canUseBankTab1" -> "Can Use Bank Tab 1";
            case "canDepositTab1" -> "Can Deposit Tab 1";
            case "canWithdrawTab1" -> "Can Withdraw Tab 1";
            case "canUseBankTab2" -> "Can Use Bank Tab 2";
            case "canDepositTab2" -> "Can Deposit Tab 2";
            case "canWithdrawTab2" -> "Can Withdraw Tab 2";
            case "canUseBankTab3" -> "Can Use Bank Tab 3";
            case "canDepositTab3" -> "Can Deposit Tab 3";
            case "canWithdrawTab3" -> "Can Withdraw Tab 3";
            case "canUseBankTab4" -> "Can Use Bank Tab 4";
            case "canDepositTab4" -> "Can Deposit Tab 4";
            case "canWithdrawTab4" -> "Can Withdraw Tab 4";
            case "canUseBankTab5" -> "Can Use Bank Tab 5";
            case "canDepositTab5" -> "Can Deposit Tab 5";
            case "canWithdrawTab5" -> "Can Withdraw Tab 5";
            case "canUseBankTab6" -> "Can Use Bank Tab 6";
            case "canDepositTab6" -> "Can Deposit Tab 6";
            case "canWithdrawTab6" -> "Can Withdraw Tab 6";
            case "canUseBankTab7" -> "Can Use Bank Tab 7";
            case "canDepositTab7" -> "Can Deposit Tab 7";
            case "canWithdrawTab7" -> "Can Withdraw Tab 7";
            case "canUseBankTab8" -> "Can Use Bank Tab 8";
            case "canDepositTab8" -> "Can Deposit Tab 8";
            case "canWithdrawTab8" -> "Can Withdraw Tab 8";
            case "canUseBankTab9" -> "Can Use Bank Tab 9";
            case "canDepositTab9" -> "Can Deposit Tab 9";
            case "canWithdrawTab9" -> "Can Withdraw Tab 9";
            default -> permissionKey;
        };
    }

    public static class GuildBankRankPermissionsHandler
    extends GenericContainerScreenHandler {
        private final SimpleInventory menuInventory;
        private final String rankName;
        private final Guild guild;

        public GuildBankRankPermissionsHandler(int syncId, PlayerInventory playerInventory, SimpleInventory menuInventory, String rankName, Guild guild) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, (Inventory)menuInventory, 6);
            this.menuInventory = menuInventory;
            this.rankName = rankName;
            this.guild = guild;
        }

        public boolean canUse(PlayerEntity player) {
            return true;
        }

        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
            if (actionType == SlotActionType.PICKUP && player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
                if (slot == 53) {
                    GuildBankRankEditorMenu.openForPlayer(serverPlayer);
                    return;
                }
                for (int i = 0; i < PERMISSION_SLOTS.length; ++i) {
                    if (slot != PERMISSION_SLOTS[i]) continue;
                    String permissionKey = SUPPORTED_PERMISSIONS.get(i);
                    boolean currentlyEnabled = this.menuInventory.getStack(slot).getItem() == Items.LIME_DYE;
                    boolean nowEnabled = !currentlyEnabled;
                    this.menuInventory.setStack(slot, GuildBankRankPermissionsMenu.toggleItem(permissionKey, nowEnabled));
                    this.guild.addPermissionToRank(this.rankName, permissionKey, nowEnabled);
                    serverPlayer.sendMessage((Text)Text.literal((String)("\u00a77Set " + GuildBankRankPermissionsMenu.getLabel(permissionKey) + " for " + this.rankName + ": " + (nowEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"))), false);
                    GuildDataManager.saveGuild(Objects.requireNonNull(serverPlayer.getServer()), this.guild);
                    break;
                }
            }
        }
    }
}

