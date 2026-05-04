/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Inventory
 *  net.minecraft.PlayerEntity
 *  net.minecraft.ItemStack
 */
package com.dahuzhou.dahuzhouguilds.util;

import java.util.Arrays;
import net.minecraft.inventory.Inventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class GuildBankInventory
implements Inventory {
    public static final int SIZE = 54;
    private final ItemStack[] items;
    private Runnable onChanged = null;
    public static final int LOCKED_START = 45;
    public static final int LOCKED_END = 53;
    private int currentPage = 1;

    public GuildBankInventory() {
        this.items = new ItemStack[54];
        Arrays.fill(this.items, ItemStack.EMPTY);
    }

    public GuildBankInventory(ItemStack[] items) {
        this.items = Arrays.copyOf(items, 54);
        for (int i = 0; i < 54; ++i) {
            if (this.items[i] != null) continue;
            this.items[i] = ItemStack.EMPTY;
        }
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    public int size() {
        return 54;
    }

    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (stack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= 54) {
            return ItemStack.EMPTY;
        }
        return this.items[slot];
    }

    public ItemStack removeStack(int slot, int amount) {
        if (slot < 0 || slot >= 54) {
            return ItemStack.EMPTY;
        }
        if (slot >= 45 && slot <= 53) {
            return ItemStack.EMPTY;
        }
        if (this.items[slot].isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = this.items[slot].split(amount);
        if (this.items[slot].isEmpty()) {
            this.items[slot] = ItemStack.EMPTY;
        }
        this.markDirty();
        return result;
    }

    public ItemStack removeStack(int slot) {
        if (slot < 0 || slot >= 54) {
            return ItemStack.EMPTY;
        }
        if (slot >= 45 && slot <= 53) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items[slot];
        this.items[slot] = ItemStack.EMPTY;
        this.markDirty();
        return removed;
    }

    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= 54) {
            return;
        }
        if (slot >= 45 && slot <= 53) {
            return;
        }
        this.items[slot] = stack;
        this.markDirty();
    }

    public void markDirty() {
        if (this.onChanged != null) {
            this.onChanged.run();
        }
    }

    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    public void clear() {
        Arrays.fill(this.items, ItemStack.EMPTY);
        this.markDirty();
    }
}

