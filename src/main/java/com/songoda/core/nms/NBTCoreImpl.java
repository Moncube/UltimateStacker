package com.songoda.core.nms;


import net.minecraft.nbt.CompoundTag;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public class NBTCoreImpl {
    /*@Deprecated
    public static NBTItemImpl of(ItemStack item) {
        return new NBTItemImpl(CraftItemStack.asNMSCopy(item));
    }

    @Deprecated
    public NBTItemImpl newItem() {
        return new NBTItemImpl(null);
    }*/

    public static NBTEntityImpl of(Entity entity) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();

        CompoundTag nbt = new CompoundTag();
        nmsEntity.saveWithoutId(nbt);

        return new NBTEntityImpl(nbt, nmsEntity);
    }

    public static NBTEntityImpl newEntity() {
        return new NBTEntityImpl(new CompoundTag(), null);
    }
}