package com.koshakmine.virtualinventory;

import cn.nukkit.Player;import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerJumpEvent;import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.plugin.PluginBase;import com.koshakmine.virtualinventory.impl.EntityInventory;

import java.util.function.Consumer;

public class Loader extends PluginBase {

    @Override
    public void onLoad() {
        FakeEnderInventory inventory = new FakeEnderInventory(9, "test");
        inventory.setMode(VirtualInventory.Mode.STORAGE);
        inventory.onClick = inventoryClick -> {
            return true;
        };
        this.getServer().getPluginManager().subscribeEvent(PlayerJumpEvent.class, event -> {
            inventory.open(event.getPlayer());
        }, this);
    }

    @Override
    public void onEnable() {
        new InventoryHooker();
        InventoryHooker.register(this);
    }

    public static class FakeEnderInventory extends EntityInventory {

        public FakeEnderInventory(int size, String name) {
            super(size, name);
        }

        @Override
        public FakeEnderInventory setMode(VirtualInventory.Mode mode) {
            this.mode = mode == null ? Mode.MENU : mode;
            return this;
        }

        @Override
        public void onClose(Player player) {
            for (int slot = 0; slot < contents.length; slot++) {
                Item item = contents[slot];

                if (item != null) {
                    player.getEnderChestInventory().setItem(slot, item);
                }
            }
            super.onClose(player);
        }
    }

}
