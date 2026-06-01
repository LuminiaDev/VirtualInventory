package com.koshakmine.virtualinventory;

import cn.nukkit.Player;

import java.util.HashMap;

public class InventoryManager {

    private static InventoryManager instance = new InventoryManager();
    private HashMap<Player, AbstractVirtualInventory> inventories = new HashMap<>();
    private HashMap<Player, InventoryDispatcher> dispatchers = new HashMap<>();

    public static InventoryManager getInstance() {
        return instance;
    }

    public AbstractVirtualInventory getInventory(Player player) {
        return inventories.get(player);
    }

    public void setInventory(Player player, AbstractVirtualInventory inventory) {
        inventories.put(player, inventory);
    }

    public void resetInventory(Player player) {
        if (!inventories.containsKey(player)) return;
        inventories.remove(player);
    }

    public InventoryDispatcher getDispatcher(Player player) {
        return dispatchers.get(player);
    }

    public void setDispatcher(Player player, InventoryDispatcher dispatcher) {
        InventoryDispatcher old = dispatchers.put(player, dispatcher);
        if (old != null) old.cancel();
    }


    public void resetDispatcher(Player player) {
        InventoryDispatcher d = dispatchers.remove(player);
        if (d != null) {
            d.cancel();
        }
    }
}
