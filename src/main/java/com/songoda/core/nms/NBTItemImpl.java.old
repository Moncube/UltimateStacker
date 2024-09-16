package com.songoda.core.nms;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;

public class NBTItemImpl extends NBTCompoundImpl {
    private final ItemStack nmsItem;

    public NBTItemImpl(ItemStack nmsItem) {
        super(nmsItem != null && nmsItem.hasTag() ? nmsItem.getTag() : new CompoundTag());

        this.nmsItem = nmsItem;
    }

    public org.bukkit.inventory.ItemStack finish() {
        if (nmsItem == null) {
            return CraftItemStack.asBukkitCopy(ItemStack.of(compound));
        }

        return CraftItemStack.asBukkitCopy(nmsItem);
    }
}