package com.songoda.core.nms;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.UUID;

public class NBTCompoundImpl {
    protected CompoundTag compound;

    protected NBTCompoundImpl(CompoundTag compound) {
        this.compound = compound;
    }

    public NBTCompoundImpl() {
        this.compound = new CompoundTag();
    }

    public NBTCompoundImpl set(String tag, String s) {
        compound.putString(tag, s);
        return this;
    }

    public NBTCompoundImpl set(String tag, boolean b) {
        compound.putBoolean(tag, b);
        return this;
    }

    public NBTCompoundImpl set(String tag, int i) {
        compound.putInt(tag, i);
        return this;
    }

    public NBTCompoundImpl set(String tag, double i) {
        compound.putDouble(tag, i);
        return this;
    }

    public NBTCompoundImpl set(String tag, long l) {
        compound.putLong(tag, l);
        return this;
    }

    public NBTCompoundImpl set(String tag, short s) {
        compound.putShort(tag, s);
        return this;
    }

    public NBTCompoundImpl set(String tag, byte b) {
        compound.putByte(tag, b);
        return this;
    }

    public NBTCompoundImpl set(String tag, int[] i) {
        compound.putIntArray(tag, i);
        return this;
    }

    public NBTCompoundImpl set(String tag, byte[] b) {
        compound.putByteArray(tag, b);
        return this;
    }

    public NBTCompoundImpl set(String tag, UUID u) {
        compound.putUUID(tag, u);
        return this;
    }

    public NBTCompoundImpl remove(String tag) {
        compound.remove(tag);
        return this;
    }

    public boolean has(String tag) {
        return compound.contains(tag);
    }

    public NBTObjectImpl getNBTObject(String tag) {
        return new NBTObjectImpl(compound, tag);
    }

    public String getString(String tag) {
        return getNBTObject(tag).asString();
    }

    public boolean getBoolean(String tag) {
        return getNBTObject(tag).asBoolean();
    }

    public int getInt(String tag) {
        return getNBTObject(tag).asInt();
    }

    public double getDouble(String tag) {
        return getNBTObject(tag).asDouble();
    }

    public long getLong(String tag) {
        return getNBTObject(tag).asLong();
    }

    public short getShort(String tag) {
        return getNBTObject(tag).asShort();
    }

    public byte getByte(String tag) {
        return getNBTObject(tag).asByte();
    }

    public int[] getIntArray(String tag) {
        return getNBTObject(tag).asIntArray();
    }

    public byte[] getByteArray(String tag) {
        return getNBTObject(tag).asByteArray();
    }

    public NBTCompoundImpl getCompound(String tag) {
        if (has(tag)) {
            return getNBTObject(tag).asCompound();
        }

        CompoundTag newCompound = new CompoundTag();
        compound.put(tag, newCompound);
        return new NBTCompoundImpl(newCompound);
    }

    public Set<String> getKeys() {
        return compound.getAllKeys();
    }

    public Set<String> getKeys(String tag) {
        return compound.getCompound(tag).getAllKeys();
    }

    public byte[] serialize(String... exclusions) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream)) {
            addExtras();
            CompoundTag compound = this.compound.copy();

            for (String exclusion : exclusions) {
                compound.remove(exclusion);
            }

            NbtIo.writeCompressed(compound, dataOutput);

            return outputStream.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public void deSerialize(byte[] serialized) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(serialized);
             ObjectInputStream dataInput = new ObjectInputStream(inputStream)) {
            compound = NbtIo.readCompressed(dataInput);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addExtras() {
        // None
    }

    @Override
    public String toString() {
        return compound.toString();
    }
}