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
 *  net.minecraft.Text
 *  net.minecraft.ServerPlayerEntity
 *  net.minecraft.NamedScreenHandlerFactory
 *  net.minecraft.ScreenHandlerType
 *  net.minecraft.SimpleNamedScreenHandlerFactory
 */
package net.demoniconpc.guildsremasteredmod.util;

import java.lang.invoke.StringConcatFactory;
import java.util.Objects;
import java.util.UUID;
import net.demoniconpc.guildsremasteredmod.data.GuildDataManager;
import net.demoniconpc.guildsremasteredmod.guild.Guild;
import net.demoniconpc.guildsremasteredmod.util.GuildMembersMenu;
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
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;

public class GuildMemberManageMenu {
    public static void openForPlayer(ServerPlayerEntity player, UUID memberId) {
        Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
        if (guild == null) {
            player.sendMessage((Text)Text.literal((String)"\u00a7cYou are not in a guild."), false);
            return;
        }
        String memberName = guild.getMembers().get((Object)memberId).name;
        int size = 27;
        SimpleInventory inventory = new SimpleInventory(size);
        ItemStack head = GuildMembersMenu.getPlayerHead(Objects.requireNonNull(player.getServer()), memberId, memberName);
        inventory.setStack(4, head);
        inventory.setStack(11, GuildPermissionsMenu.menuItem((Item)Items.field_8287, (String)"Promote", (String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"/guild promote \u0001"}, (String)memberName))).icon.copy());
        inventory.setStack(13, GuildPermissionsMenu.menuItem((Item)Items.field_8725, (String)"Kick", (String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"/guild kick \u0001"}, (String)memberName))).icon.copy());
        inventory.setStack(15, GuildPermissionsMenu.menuItem((Item)Items.BARRIER, (String)"Demote", (String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"/guild demote \u0001"}, (String)memberName))).icon.copy());
        inventory.setStack(26, GuildPermissionsMenu.menuItem((Item)Items.field_8107, (String)"Back", null).icon.copy());
        player.openHandledScreen((NamedScreenHandlerFactory)new SimpleNamedScreenHandlerFactory((syncId, playerInv, playerEntity) -> new GuildMemberManageMenuHandler(syncId, playerInv, inventory, memberId), (Text)Text.literal((String)("Manage: " + memberName))));
    }

    public static class GuildMemberManageMenuHandler
    extends GenericContainerScreenHandler {
        private final UUID memberId;

        public GuildMemberManageMenuHandler(int syncId, PlayerInventory playerInventory, SimpleInventory menuInventory, UUID memberId) {
            super(ScreenHandlerType.field_17326, syncId, playerInventory, (Inventory)menuInventory, 3);
            this.memberId = memberId;
        }

        public boolean canUse(PlayerEntity player) {
            return true;
        }

        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
            if (actionType == SlotActionType.PICKUP) {
                if (slot == 26) {
                    GuildMembersMenu.openForPlayer((ServerPlayerEntity)player);
                    return;
                }
                if (player instanceof ServerPlayerEntity) {
                    ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
                    Guild guild = GuildDataManager.getGuildByPlayer(serverPlayer.getUuid());
                    String memberName = guild != null && guild.getMembers().containsKey(this.memberId) ? guild.getMembers().get((Object)this.memberId).name : "";
                    String cmd = null;
                    if (slot == 11) {
                        cmd = "/guild promote " + memberName;
                    }
                    if (slot == 13) {
                        cmd = "/guild kick " + memberName;
                    }
                    if (slot == 15) {
                        cmd = "/guild demote " + memberName;
                    }
                    if (cmd != null) {
                        cmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                        serverPlayer.closeHandledScreen();
                        serverPlayer.getServer().getCommandManager().executeWithPrefix(serverPlayer.getCommandSource(), cmd);
                    }
                }
            }
        }
    }
}

