package com.songoda.core.nms;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

public class NBTObjectImpl {
    private final CompoundTag compound;
    private final String tag;

    public NBTObjectImpl(CompoundTag compound, String tag) {
        this.compound = compound;
        this.tag = tag;
    }

    public String asString() {
        return compound.getString(tag);
    }

    public boolean asBoolean() {
        return compound.getBoolean(tag);
    }

    public int asInt() {
        return compound.getInt(tag);
    }

    public double asDouble() {
        return compound.getDouble(tag);
    }

    public long asLong() {
        return compound.getLong(tag);
    }

    public short asShort() {
        return compound.getShort(tag);
    }

    public byte asByte() {
        return compound.getByte(tag);
    }

    public int[] asIntArray() {
        return compound.getIntArray(tag);
    }

    public byte[] asByteArray() {
        return compound.getByteArray(tag);
    }

    public NBTCompoundImpl asCompound() {
        return new NBTCompoundImpl(compound.getCompound(tag));
    }

    public Set<String> getKeys() {
        return compound.getAllKeys();
    }
}