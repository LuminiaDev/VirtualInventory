package com.koshakmine.virtualinventory;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.custom.EntityDefinition;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.network.protocol.ContainerClosePacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.InventoryTransactionPacket;
import cn.nukkit.network.protocol.types.NetworkInventoryAction;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.registry.Registries;

public class InventoryHooker {
    public InventoryHooker(Plugin plugin) {
        EntityDefinition definition = Registries.ENTITY.getCustomEntityDefinition(DummyEntity.definition.getIdentifier());
        if (definition == null) {
            Registries.ENTITY.registerCustomEntityDefinition(DummyEntity.definition);
        }

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        pluginManager.subscribeEvent(PlayerQuitEvent.class, this::onQuit, plugin);
        pluginManager.subscribeEvent(DataPacketReceiveEvent.class, this::onDataPacketReceive, plugin);
    }

    private void onQuit(PlayerQuitEvent ev) {
        Player player = ev.getPlayer();
        InventoryManager manager = InventoryManager.getInstance();
        AbstractVirtualInventory inventory = manager.getInventory(player);
        if (inventory != null) {
            inventory.close(player);
        }
    }

    private void onDataPacketReceive(DataPacketReceiveEvent ev) {
        DataPacket pk = ev.getPacket();
        Player player = ev.getPlayer();
        InventoryManager manager = InventoryManager.getInstance();
        if (pk instanceof ContainerClosePacket closePacket) {
            AbstractVirtualInventory inv = InventoryManager.getInstance().getInventory(player);
            if (inv != null && inv.isViewer(player)) {
                int winId = inv.getWindowId(player);

                if (closePacket.windowId == Byte.MIN_VALUE || (winId > 0 && closePacket.windowId == (byte) winId)) {
                    inv.onClose(player);
                    player.directDataPacket(pk);
                    ev.setCancelled(true);
                }
            }
            return;
        }
        if ((pk instanceof InventoryTransactionPacket transactionPacket)) {
            if (transactionPacket.transactionType != InventoryTransactionPacket.TYPE_NORMAL) return;
            if (transactionPacket.actions.length > 100) {
                player.kick("disconnectionScreen.badPacket");
                return;
            }

            AbstractVirtualInventory inventory = manager.getInventory(player);
            if (inventory != null) {
                if (inventory.isViewer(player)) {
                    boolean cancelled = processTransaction(player, transactionPacket.actions);
                    ev.setCancelled(cancelled);

                        Server.getInstance().getScheduler().scheduleDelayedTask(() -> {
                            player.getCursorInventory().sendSlot(0, player);
                            player.getUIInventory().sendSlot(0, player);
                        }, 1);

                } else {
                    ev.setCancelled();
                }
            }
        }
    }

    private boolean processTransaction(Player player, NetworkInventoryAction[] actions) {
        InventoryManager manager = InventoryManager.getInstance();
        AbstractVirtualInventory inventory = manager.getInventory(player);

        if (inventory == null || !inventory.isViewer(player)) return true;

        int winId = inventory.getWindowId(player);

        boolean cancelled = false;
        for (NetworkInventoryAction action : actions) {
            if(action.sourceType == NetworkInventoryAction.SOURCE_CREATIVE) {
                return true;
            }
            if (winId > 0 && action.windowId == (byte) winId) {
                if(!inventory.handleTransaction(player, action.inventorySlot, action.newItem)) {
                    cancelled = true;
                }
            } else {
                player.getWindowById(action.windowId).sendSlot(action.inventorySlot, player);
            }
        }
        inventory.syncContents();
        return cancelled;
    }
}