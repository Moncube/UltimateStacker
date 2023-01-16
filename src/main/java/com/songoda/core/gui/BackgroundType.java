package com.songoda.core.gui;

public enum BackgroundType {
    ADVENTURE, END, HUSBANDRY, NETHER, STONE;
    final String key;

    BackgroundType() {
        this.key = "minecraft:textures/gui/advancements/backgrounds/" + name().toLowerCase() + ".png";
    }
}