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
import com.dahuzhou.dahuzhouguilds.util.GuildPermissionsMenu;
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

public class GuildRankPermissionsMenu {
    private static final List<String> SUPPORTED_PERMISSIONS = Arrays.asList("canInvite", "canKick", "canPromote", "canDemote", "canSetHome", "canUseHome", "canTogglePvP", "canDonate");
    private static final int[] PERMISSION_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7};

    public static void openForPlayer(ServerPlayerEntity player, String rankName) {
        int size = 54;
        SimpleInventory inventory = new SimpleInventory(size);
        Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
        if (guild == null) {
            player.sendMessage((Text)Text.literal((String)"\u00a7cYou must be in a guild to edit rank permissions."), false);
            return;
        }
        Map<String, Boolean> rankPermissions = guild.getRankPermissions(rankName);
        for (int i = 0; i < SUPPORTED_PERMISSIONS.size(); ++i) {
            String permission = SUPPORTED_PERMISSIONS.get(i);
            int slot = PERMISSION_SLOTS[i];
            boolean enabled = rankPermissions.getOrDefault(permission, false);
            inventory.setStack(slot, GuildRankPermissionsMenu.toggleItem(permission, enabled));
        }
        inventory.setStack(53, GuildPermissionsMenu.menuItem((Item)Items.ARROW, (String)"Back", null).icon.copy());
        player.openHandledScreen((NamedScreenHandlerFactory)new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerEntity) -> new GuildRankPermissionsHandler(syncId, playerInventory, inventory, rankName, guild), (Text)Text.literal((String)("Rank: " + rankName))));
    }

    private static ItemStack toggleItem(String permission, boolean enabled) {
        ItemStack stack = new ItemStack((ItemConvertible)(enabled ? Items.LIME_DYE : Items.GRAY_DYE));
        stack.setCustomName(Text.literal((enabled ? "\u00a7a" : "\u00a77") + GuildRankPermissionsMenu.getLabel(permission)));
        return stack;
    }

    private static String getLabel(String permissionKey) {
        return switch (permissionKey) {
            case "canInvite" -> "Can Invite";
            case "canKick" -> "Can Kick";
            case "canPromote" -> "Can Promote";
            case "canDemote" -> "Can Demote";
            case "canSetHome" -> "Can Set Home";
            case "canUseHome" -> "Can Use Home";
            case "canTogglePvP" -> "Can Toggle PvP";
            case "canDonate" -> "Can Donate";
            default -> permissionKey;
        };
    }

    public static void openEditor(ServerPlayerEntity player, String bankPermissions, List<String> canDeposit) {
    }

    public static class GuildRankPermissionsHandler
    extends GenericContainerScreenHandler {
        private final SimpleInventory menuInventory;
        private final String rankName;
        private final Guild guild;

        public GuildRankPermissionsHandler(int syncId, PlayerInventory playerInventory, SimpleInventory menuInventory, String rankName, Guild guild) {
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
                    GuildRankEditorMenu.openForPlayer(serverPlayer);
                    return;
                }
                for (int i = 0; i < PERMISSION_SLOTS.length; ++i) {
                    if (slot != PERMISSION_SLOTS[i]) continue;
                    String permissionKey = SUPPORTED_PERMISSIONS.get(i);
                    boolean currentlyEnabled = this.menuInventory.getStack(slot).getItem() == Items.LIME_DYE;
                    boolean nowEnabled = !currentlyEnabled;
                    this.menuInventory.setStack(slot, GuildRankPermissionsMenu.toggleItem(permissionKey, nowEnabled));
                    this.guild.addPermissionToRank(this.rankName, permissionKey, nowEnabled);
                    serverPlayer.sendMessage((Text)Text.literal((String)("\u00a77Set " + GuildRankPermissionsMenu.getLabel(permissionKey) + " for " + this.rankName + ": " + (nowEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"))), false);
                    GuildDataManager.saveGuild(Objects.requireNonNull(serverPlayer.getServer()), this.guild);
                    break;
                }
            }
        }
    }
}

