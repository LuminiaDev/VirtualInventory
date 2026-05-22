package com.koshakmine.virtualinventory;

import cn.nukkit.Player;
import cn.nukkit.item.Item;

import java.util.HashMap;

public class InventoryManager {

    private static InventoryManager instance = new InventoryManager();
    private HashMap<Player, VirtualInventory> inventories = new HashMap<>();
    private HashMap<Player, InventoryDispatcher> dispatchers = new HashMap<>();

    public static InventoryManager getInstance() {
        return instance;
    }

    public VirtualInventory getInventory(Player player) {
        return inventories.get(player);
    }

    public void setInventory(Player player, VirtualInventory inventory) {
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
