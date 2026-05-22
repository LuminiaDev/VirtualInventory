package com.koshakmine.virtualinventory;

import cn.nukkit.Player;
import cn.nukkit.item.Item;

public record InventoryClick(VirtualInventory inventory, Player player, int slot, Item item) {
}
