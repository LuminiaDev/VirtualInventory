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
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
@Setter
abstract public class AbstractVirtualInventory {

    @Setter(AccessLevel.NONE)
    private final ArrayList<Player> viewers = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private final Map<Player, Integer> windowIds = new HashMap<>();

    @Setter(AccessLevel.NONE)
    private String name;

    private String prefix = "";

    @Setter(AccessLevel.NONE)
    private int size;

    @Setter(AccessLevel.NONE)
    private Item[] contents;

    @Getter(AccessLevel.NONE)
    private Predicate<ItemHandler> defaultItemHandler = null;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Int2ObjectOpenHashMap<Predicate<ItemHandler>> slotItemHandlers = new Int2ObjectOpenHashMap<>();

    @Getter(AccessLevel.NONE)
    private Consumer<Player> closeHandler = null;

    @Getter(AccessLevel.NONE)
    private Consumer<Player> openHandler = null;

    @Getter(AccessLevel.NONE)
    private final Map<Player, InventoryAdapter> adapters = new WeakHashMap<>();

    public AbstractVirtualInventory(int size) {
        this(size, "Chest");
    }

    public AbstractVirtualInventory(int size, String name) {
        this.size = size;
        this.name = name;

        contents = new Item[size];
    }

    public int getWindowId(Player p) {
        return windowIds.getOrDefault(p, (int) Byte.MIN_VALUE);
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

    public boolean setItem(int slot, Item item, Predicate<ItemHandler> handler) {
        return setItem(slot, item, handler, true);
    }

    public boolean setItem(int slot, Item item, Predicate<ItemHandler> handler, boolean sync) {
        boolean success = this.setItem(slot, item, sync);
        if (success) this.addItemHandler(slot, handler);
        return success;
    }

    public void addItemHandler(int slot, Predicate<ItemHandler> handler) {
        slotItemHandlers.put(slot, handler);
    }

    public void open(Player player) {
        open(player, name);
    }

    public void open(Player player, String name) {
        if (viewers.contains(player)) return;

        byte windowId = Byte.MIN_VALUE;

        InventoryAdapter adapter = adapters.computeIfAbsent(player, k -> new InventoryAdapter(this));
        player.addWindow(adapter);

        int winId = player.getWindowId(adapter);
        if (winId > 0) {
            windowIds.put(player, winId);
            windowId = (byte) winId;
        } else {
            windowIds.put(player, (int) Byte.MIN_VALUE);
        }

        DataPacket[] packets = sendInventory(player, name, windowId);

        InventoryManager invManager = InventoryManager.getInstance();
        invManager.setDispatcher(player, new InventoryDispatcher(player, packets, this::syncContents));

        for (DataPacket packet : packets) player.directDataPacket(packet);

        viewers.add(player);
        invManager.setInventory(player, this);

        if (openHandler != null) openHandler.accept(player);

        syncContents();
    }

    protected void close(Player player) {
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

    protected void onClose(Player player) {
        if (!viewers.contains(player)) return;
        if (closeHandler != null) closeHandler.accept(player);
        viewers.remove(player);

        int winId = getWindowId(player);
        if (winId > 0) {
            InventoryAdapter adapter = adapters.get(player);
            if (adapter != null) {
                try {
                    player.removeWindow(adapter);
                } catch (Throwable ignored) {
                }
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

    public boolean handleTransaction(Player player, int slot, Item newItem) {
        if (slot < 0 || slot >= size) return false;
        if (!viewers.contains(player)) return false;

        var event = new ItemHandler(this, player, slot, getItem(slot), newItem);

        if(slotItemHandlers.containsKey(slot)) return slotItemHandlers.get(slot).test(event);

        if (defaultItemHandler != null) return defaultItemHandler.test(event);
        return false;
    }

    abstract protected ContainerSlotType getSlotType();

    abstract protected DataPacket[] sendInventory(Player player, String name, byte windowId);

    abstract protected DataPacket[] removeInventory(Player player);
}
