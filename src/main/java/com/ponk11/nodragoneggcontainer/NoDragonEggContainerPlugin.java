package com.ponk11.nodragoneggcontainer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class NoDragonEggContainerPlugin extends JavaPlugin implements Listener {
    private static final String BLOCKED_MESSAGE = "You cannot store or place the dragon egg. You can only drop it.";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        boolean puttingCursorIntoTop = clickedInventory == topInventory && isDragonEgg(event.getCursor());
        boolean shiftClickingEggIntoTop = clickedInventory != topInventory
                && event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY
                && isDragonEgg(event.getCurrentItem());
        boolean swappingHotbarEggIntoTop = clickedInventory == topInventory
                && event.getHotbarButton() >= 0
                && isDragonEgg(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
        boolean storingEggInBundle = (isBundle(event.getCurrentItem()) && isDragonEgg(event.getCursor()))
            || (isBundle(event.getCursor()) && isDragonEgg(event.getCurrentItem()));

        if (puttingCursorIntoTop || shiftClickingEggIntoTop || swappingHotbarEggIntoTop || storingEggInBundle) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(BLOCKED_MESSAGE);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isDragonEgg(event.getOldCursor())) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        boolean targetsTopInventory = event.getRawSlots().stream()
                .anyMatch(rawSlot -> rawSlot < topInventory.getSize());
        if (targetsTopInventory) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(BLOCKED_MESSAGE);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isDragonEgg(event.getItemInHand())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(BLOCKED_MESSAGE);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemFrameInteract(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (clicked instanceof ItemFrame && isDragonEgg(event.getPlayer().getInventory().getItem(event.getHand()))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(BLOCKED_MESSAGE);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isDragonEgg(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        Item item = event.getItem();
        if (isDragonEgg(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getKeepInventory()) {
            return;
        }

        List<ItemStack> dragonEggs = new ArrayList<>();
        event.getDrops().removeIf(stack -> {
            if (!isDragonEgg(stack)) {
                return false;
            }
            dragonEggs.add(stack.clone());
            return true;
        });

        PlayerInventory inventory = event.getEntity().getInventory();
        collectEggs(inventory.getStorageContents(), inventory::setStorageContents, dragonEggs);
        collectEggs(inventory.getArmorContents(), inventory::setArmorContents, dragonEggs);
        collectEggs(inventory.getExtraContents(), inventory::setExtraContents, dragonEggs);
        event.getDrops().addAll(dragonEggs);
    }

    private static void collectEggs(ItemStack[] contents, java.util.function.Consumer<ItemStack[]> setter,
            List<ItemStack> dragonEggs) {
        boolean changed = false;
        for (int slot = 0; slot < contents.length; slot++) {
            if (isDragonEgg(contents[slot])) {
                dragonEggs.add(contents[slot].clone());
                contents[slot] = null;
                changed = true;
            }
        }
        if (changed) {
            setter.accept(contents);
        }
    }

    private static boolean isDragonEgg(ItemStack item) {
        return item != null && item.getType() == Material.DRAGON_EGG;
    }

    private static boolean isBundle(ItemStack item) {
        return item != null && item.getType() == Material.BUNDLE;
    }
}