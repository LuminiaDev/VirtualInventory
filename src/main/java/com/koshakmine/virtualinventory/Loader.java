package com.koshakmine.virtualinventory;

import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerJumpEvent;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import com.koshakmine.virtualinventory.impl.EntityInventory;

public class Loader extends PluginBase {

    @Override
    public void onLoad() {
        FakeEnderInventory inventory = new FakeEnderInventory(90, "test");
        inventory.setDefaultItemHandler(click -> {
            click.player().sendMessage("goida");
            return true;
        });

        inventory.setCloseHandler(player -> {
            player.sendMessage("ZOV");
            for (int slot = 0; slot < inventory.getContents().length; slot++) {
                Item item = inventory.getContents()[slot];

                if (item != null) {
                    player.getEnderChestInventory().setItem(slot, item);
                }
            }
        });

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
    }

}
