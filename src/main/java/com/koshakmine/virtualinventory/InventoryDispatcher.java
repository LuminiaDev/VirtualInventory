package com.koshakmine.virtualinventory;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.scheduler.TaskHandler;

final public class InventoryDispatcher {

    private final TaskHandler taskHandler;

    private final Player player;
    private final DataPacket[] packets;
    private final Runnable runnable;
    private long timemout = 20L;

    public InventoryDispatcher(Player player, DataPacket[] packets, Runnable runnable) {
        this.player = player;
        this.packets = packets;
        this.runnable = runnable;

        this.taskHandler = Server.getInstance().getScheduler().scheduleRepeatingTask(this::run, 1);
    }

    public void run() {
        if (!this.player.isConnected()) {
            this.taskHandler.cancel();
            return;
        }

        AbstractVirtualInventory inv = InventoryManager.getInstance().getInventory(this.player);
        if (inv == null || !inv.isViewer(this.player)) {
            this.taskHandler.cancel();
            return;
        }

        --this.timemout;

        if (timemout <= 0) {
            process();
            return;
        }

        if ((timemout & 1) == 0) {
            for (DataPacket packet : packets) {
                player.directDataPacket(packet);
            }
        }
    }

    public void process() {
        runnable.run();
        this.taskHandler.cancel();
    }

    public void cancel() {
        this.taskHandler.cancel();
    }
}
