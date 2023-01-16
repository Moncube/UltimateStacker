package com.songoda.core.lootables.gui;

import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.lootables.loot.LootManager;
import com.songoda.core.lootables.loot.Lootable;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiEditor extends Gui {
    private final LootManager lootManager;

    public GuiEditor(LootManager lootManager) {
        super(6);

        this.lootManager = lootManager;

        setDefaultItem(null);
        setTitle("Lootables Overview");

        paint();
    }

    private void paint() {
        if (inventory != null) {
            inventory.clear();
        }

        setActionForRange(0, 0, 5, 9, null);

        List<Lootable> lootables = new ArrayList<>(lootManager.getRegisteredLootables().values());

        double itemCount = lootables.size();
        this.pages = (int) Math.max(1, Math.ceil(itemCount / 36));

        if (page != 1) {
            setButton(5, 2, GuiUtils.createButtonItem(Material.ARROW, "Back"),
                    (event) -> {
                        page--;
                        paint();
                    });
        }

        if (page != pages) {
            setButton(5, 6, GuiUtils.createButtonItem(Material.ARROW, "Next"),
                    (event) -> {
                        page++;
                        paint();
                    });
        }

        for (int i = 9; i < 45; i++) {
            int current = ((page - 1) * 36) - 9;
            if (current + i >= lootables.size()) {
                setItem(i, null);
                continue;
            }

            Lootable lootable = lootables.get(current + i);
            if (lootable == null) {
                continue;
            }

            setButton(i, getIcon(lootable.getKey()),
                    (event) -> guiManager.showGUI(event.player, new GuiLootableEditor(lootManager, lootable, this)));
        }
    }

    public ItemStack getIcon(String key) {
        ItemStack stack = null;
        EntityType type = EntityType.fromName(key);

        if (type != null) {
            Material material = Material.getMaterial(type+"_SPAWN_EGG");

            if (material != null) {
                stack = new ItemStack(material);
            }
        }

        if (stack == null) {
            stack = new ItemStack(Material.GHAST_SPAWN_EGG);
        }

        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(key);
        stack.setItemMeta(meta);

        return stack;
    }
}