package com.koshakmine.virtualinventory;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.network.protocol.ContainerClosePacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.InventoryContentPacket;
import cn.nukkit.network.protocol.InventorySlotPacket;
import cn.nukkit.network.protocol.types.inventory.ContainerSlotType;
import cn.nukkit.network.protocol.types.inventory.ContainerType;
import cn.nukkit.network.protocol.types.inventory.FullContainerName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

abstract public class VirtualInventory {

    protected final ArrayList<Player> viewers = new ArrayList<>();
    protected final Map<Player, Integer> windowIds = new HashMap<>();
    protected String name;
    protected String prefix;
    protected int size;
    protected Item[] contents;
    protected Predicate<InventoryClick> onClick = null;
    protected Consumer<Player> onClose = null;
    protected Mode mode = Mode.MENU;
    protected final Map<Player, InventoryAdapter> adapters = new WeakHashMap<>();

    public enum Mode {MENU, STORAGE}

    public VirtualInventory(int size) {
        this(size, "Chest");
    }

    public VirtualInventory(int size, String name) {
        this.size = size;
        this.name = name;
        int rows = size / 9 + (size % 9 == 0 ? 0 : 1);
        int scroll = rows > 6 ? 1 : 0;
        int length = Math.min(rows, 6);
        this.prefix = "§" + length + "§" + scroll + "§r§r§r§r§r§r§r§r§r§r";

        contents = new Item[size];
    }

    public int getWindowId(Player p) {
        return windowIds.getOrDefault(p, (int) Byte.MIN_VALUE);
    }

    public Mode getMode() {
        return mode;
    }

    public VirtualInventory setMode(Mode mode) {
        this.mode = (mode == null ? Mode.MENU : mode);
        return this;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public Item[] getContents() {
        return contents;
    }

    public Item getItem(int slot) {
        Item item = contents[slot];
        if (item == null) {
            return Item.AIR_ITEM;
        }

        return item;
    }

    public boolean setItem(int slot, Item item) {
        return setItem(slot, item, true);
    }

    public boolean setItem(int slot, Item item, boolean sync) {
        if (slot < 0 || slot >= size) {
            return false;
        }

        if (item == null) {
            item = Item.AIR_ITEM;
        }

        contents[slot] = item;
        if (sync) syncSlot(slot);
        return true;
    }

    public void open(Player player) {
        open(player, name);
    }

    public void open(Player player, String name) {
        if (viewers.contains(player)) return;

        byte windowId = Byte.MIN_VALUE;

        if (mode == Mode.STORAGE) {
            InventoryAdapter adapter = adapters.computeIfAbsent(player, k -> new InventoryAdapter(this));
            player.addWindow(adapter);

            int winId = player.getWindowId(adapter);
            if (winId > 0) {
                windowIds.put(player, winId);
                windowId = (byte) winId;
            } else {
                windowIds.put(player, (int) Byte.MIN_VALUE);
            }
        } else {
            windowIds.put(player, (int) Byte.MIN_VALUE);
        }

        DataPacket[] packets = sendInventory(player, name, windowId);

        InventoryManager invManager = InventoryManager.getInstance();
        invManager.setDispatcher(player, new InventoryDispatcher(player, packets, this::syncContents));

        for (DataPacket packet : packets) player.directDataPacket(packet);

        viewers.add(player);
        invManager.setInventory(player, this);

        syncContents();
    }

    public void close(Player player) {
        if (!viewers.contains(player)) return;

        InventoryManager.getInstance().resetDispatcher(player);

        ContainerClosePacket closePacket = new ContainerClosePacket();
        closePacket.windowId = Byte.MAX_VALUE;
        closePacket.type = ContainerType.CONTAINER;
        closePacket.wasServerInitiated = true;
        player.directDataPacket(closePacket);

        ContainerClosePacket closePacket2 = new ContainerClosePacket();
        closePacket2.windowId = Byte.MIN_VALUE;
        closePacket2.type = ContainerType.CONTAINER;
        closePacket2.wasServerInitiated = true;
        player.directDataPacket(closePacket2);

        onClose(player);
    }

    public void onClose(Player player) {
        if (!viewers.contains(player)) return;
        if (onClose != null) onClose.accept(player);
        viewers.remove(player);

        int winId = getWindowId(player);
        if (winId > 0) {
            InventoryAdapter adapter = adapters.get(player);
            if (adapter != null) {
                try {
                    player.removeWindow(adapter);
                } catch (Throwable ignored) {}
            }
        }

        for (DataPacket packet : removeInventory(player)) {
            player.directDataPacket(packet);
        }

        try {
            var field = Player.class.getDeclaredField("inventoryOpen");
            field.setAccessible(true);
            field.set(player, false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        InventoryManager.getInstance().resetInventory(player);
        windowIds.remove(player);
        adapters.remove(player);
    }

    public void syncSlot(int slot) {
        var pk = new InventorySlotPacket();
        pk.slot = slot;

        pk.containerNameData = new FullContainerName(getSlotType(), null);
        pk.item = getItem(slot);

        for (Player viewer : new ArrayList<>(viewers)) {
            if (!viewer.isConnected()) {
                viewers.remove(viewer);
                continue;
            }

            int winId = getWindowId(viewer);
            pk.inventoryId = (byte) winId;

            viewer.directDataPacket(pk);
        }
    }

    public void syncContents() {
        var pk = new InventoryContentPacket();
        pk.containerNameData = new FullContainerName(getSlotType(), null);
        pk.storageItem = Item.AIR_ITEM;
        pk.slots = contents;

        for (Player viewer : new ArrayList<>(viewers)) {
            if (!viewer.isConnected()) {
                viewers.remove(viewer);
                continue;
            }

            int winId = getWindowId(viewer);
            pk.inventoryId = (byte) winId;

            viewer.directDataPacket(pk);
        }
    }

    public boolean isViewer(Player player) {
        return viewers.contains(player);
    }

    public boolean onClick(Player player, int slot) {
        if (slot < 0 || slot >= size) return false;
        if (!viewers.contains(player)) return false;

        var event = new InventoryClick(this, player, slot, getItem(slot));

        if (onClick != null) return onClick.test(event);
        return false;
    }

    abstract protected ContainerSlotType getSlotType();

    abstract protected DataPacket[] sendInventory(Player player, String name, byte windowId);

    abstract protected DataPacket[] removeInventory(Player player);
}
