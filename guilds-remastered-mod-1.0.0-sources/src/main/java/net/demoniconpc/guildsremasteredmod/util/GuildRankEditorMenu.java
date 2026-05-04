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
import net.demoniconpc.guildsremasteredmod.util.GuildPermissionsMenu;
import net.demoniconpc.guildsremasteredmod.util.GuildRankPermissionsMenu;
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

public class GuildRankEditorMenu {
    public static Map<String, Boolean> getDefaultPermissions(String rankName) {
        HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();
        if (rankName.equalsIgnoreCase("Guild Master")) {
            permissions.put("canManageMembers", true);
            permissions.put("canPromote", true);
            permissions.put("canDemote", true);
            permissions.put("canKick", true);
            permissions.put("canEditPermissions", true);
            permissions.put("canSetHome", true);
            permissions.put("canDisband", true);
        } else {
            permissions.put("canManageMembers", false);
            permissions.put("canPromote", false);
            permissions.put("canDemote", false);
            permissions.put("canKick", false);
            permissions.put("canEditPermissions", false);
            permissions.put("canSetHome", false);
            permissions.put("canDisband", false);
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
        inventory.setStack(10, GuildRankEditorMenu.rankItem("Guild Master", Items.field_8137));
        inventory.setStack(12, GuildRankEditorMenu.rankItem("Officer", Items.field_8802));
        inventory.setStack(14, GuildRankEditorMenu.rankItem("Member", Items.field_8371));
        inventory.setStack(16, GuildRankEditorMenu.rankItem("Initiate", Items.field_8091));
        inventory.setStack(26, GuildPermissionsMenu.menuItem((Item)Items.field_8107, (String)"Back", null).icon.copy());
        player.openHandledScreen((NamedScreenHandlerFactory)new SimpleNamedScreenHandlerFactory((syncId, playerInv, playerEntity) -> new GuildRankEditorMenuHandler(syncId, playerInv, inventory), (Text)Text.literal((String)"Guild Rank Editor")));
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

    public static class GuildRankEditorMenuHandler
    extends GenericContainerScreenHandler {
        private final SimpleInventory menuInventory;

        public GuildRankEditorMenuHandler(int syncId, PlayerInventory playerInventory, SimpleInventory menuInventory) {
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
                    GuildRankPermissionsMenu.openForPlayer(serverPlayer, rankName);
                }
            }
        }
    }
}

