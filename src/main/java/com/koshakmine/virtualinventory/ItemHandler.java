package com.koshakmine.virtualinventory;

import cn.nukkit.Player;
import cn.nukkit.item.Item;

public record ItemHandler(AbstractVirtualInventory inventory, Player player, int slot, Item oldItem, Item newItem) {
}
