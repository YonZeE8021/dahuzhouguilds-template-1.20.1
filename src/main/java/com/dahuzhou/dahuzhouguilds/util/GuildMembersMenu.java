/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
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
 *  net.minecraft.ProfileComponent
 *  net.minecraft.DataComponentTypes
 *  net.minecraft.server.MinecraftServer
 */
package com.dahuzhou.dahuzhouguilds.util;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.GuildMemberManageMenu;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;

public class GuildMembersMenu {
    private static final int[] MEMBER_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42};

    public static ItemStack getPlayerHead(MinecraftServer server, UUID uuid, String playerName) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        GameProfile profile = server.getUserCache().getByUuid(uuid).orElse(new GameProfile(uuid, playerName));
        stack.setCustomName(Text.literal("\u00a7f" + playerName));
        NbtCompound skullOwner = new NbtCompound();
        NbtHelper.writeGameProfile(skullOwner, profile);
        stack.setSubNbt("SkullOwner", skullOwner);
        return stack;
    }

    public static void openForPlayer(ServerPlayerEntity player) {
        GuildMembersMenu.openForPlayer(player, 0);
    }

    public static void openForPlayer(ServerPlayerEntity player, int page) {
        Guild guild = GuildDataManager.getGuildByPlayer(player.getUuid());
        if (guild == null) {
            player.sendMessage((Text)Text.literal((String)"\u00a7cYou are not in a guild."), false);
            return;
        }
        ArrayList<UUID> memberIds = new ArrayList<UUID>(guild.getMembers().keySet());
        int totalPages = (int)Math.ceil((double)memberIds.size() / (double)MEMBER_SLOTS.length);
        if (page < 0) {
            page = 0;
        }
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        int currentPage = page;
        int start = currentPage * MEMBER_SLOTS.length;
        int end = Math.min(start + MEMBER_SLOTS.length, memberIds.size());
        List currentPageIds = memberIds.subList(start, end);
        SimpleInventory inventory = new SimpleInventory(54);
        for (int i = 0; i < currentPageIds.size(); ++i) {
            UUID memberId = (UUID)currentPageIds.get(i);
            String name = guild.getMembers().get((Object)memberId).name;
            inventory.setStack(MEMBER_SLOTS[i], GuildMembersMenu.getPlayerHead(Objects.requireNonNull(player.getServer()), memberId, name));
        }
        inventory.setStack(45, GuildPermissionsMenu.menuItem((Item)Items.ARROW, (String)(currentPage > 0 ? "\u00a7a\u2190 Prev Page" : "\u00a77\u2190 Prev Page"), null).icon.copy());
        inventory.setStack(49, GuildPermissionsMenu.menuItem((Item)Items.BARRIER, (String)"\u00a7cBack", null).icon.copy());
        inventory.setStack(53, GuildPermissionsMenu.menuItem((Item)Items.ARROW, (String)(currentPage + 1 < totalPages ? "\u00a7aNext Page \u2192" : "\u00a77Next Page \u2192"), null).icon.copy());
        player.openHandledScreen((NamedScreenHandlerFactory)new SimpleNamedScreenHandlerFactory((syncId, playerInv, playerEntity) -> new GuildMembersMenuHandler(syncId, playerInv, inventory, memberIds, currentPage), (Text)Text.literal((String)("Guild Members (Page " + (currentPage + 1) + "/" + Math.max(totalPages, 1) + ")"))));
    }

    public static class GuildMembersMenuHandler
    extends GenericContainerScreenHandler {
        private final List<UUID> memberIds;
        private final int page;

        public GuildMembersMenuHandler(int syncId, PlayerInventory playerInventory, SimpleInventory menuInventory, List<UUID> memberIds, int page) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, (Inventory)menuInventory, 6);
            this.memberIds = memberIds;
            this.page = page;
        }

        public boolean canUse(PlayerEntity player) {
            return true;
        }

        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
            if (actionType != SlotActionType.PICKUP) {
                return;
            }
            int start = this.page * MEMBER_SLOTS.length;
            int end = Math.min(start + MEMBER_SLOTS.length, this.memberIds.size());
            List<UUID> currentPageIds = this.memberIds.subList(start, end);
            if (slot == 45 && this.page > 0) {
                GuildMembersMenu.openForPlayer((ServerPlayerEntity)player, this.page - 1);
                return;
            }
            if (slot == 53 && (this.page + 1) * MEMBER_SLOTS.length < this.memberIds.size()) {
                GuildMembersMenu.openForPlayer((ServerPlayerEntity)player, this.page + 1);
                return;
            }
            if (slot == 49) {
                GuildPermissionsMenu.openForPlayer((ServerPlayerEntity)player);
                return;
            }
            for (int i = 0; i < currentPageIds.size(); ++i) {
                if (slot != MEMBER_SLOTS[i]) continue;
                GuildMemberManageMenu.openForPlayer((ServerPlayerEntity)player, currentPageIds.get(i));
                return;
            }
        }
    }
}

