package com.koshakmine.virtualinventory;

import cn.nukkit.Player;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.item.Item;

import java.util.*;

public class InventoryAdapter implements Inventory {

    private final VirtualInventory v;
    private InventoryHolder holder;

    public InventoryAdapter(VirtualInventory v) {
        this.v = v;
    }

    @Override
    public int getSize() {
        return v.getSize();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setMaxStackSize(int i) {
    }

    @Override
    public Item getItem(int index) {
        return v.getItem(index);
    }

    @Override
    public boolean setItem(int index, Item item) {
        return setItem(index, item, true);
    }

    @Override
    public boolean setItem(int index, Item item, boolean send) {
        v.setItem(index, item, send);
        return true;
    }

    @Override
    public Item[] addItem(Item... items) {
        List<Item> left = new ArrayList<>();
        if (items == null) return new Item[0];

        for (Item add : items) {
            if (add == null || add.isNull() || add.getId() == 0 || add.getCount() <= 0) continue;

            Item toAdd = add.clone();

            for (int i = 0; i < getSize(); i++) {
                Item cur = v.getItem(i);
                if (cur == null || cur.isNull() || cur.getId() == 0) continue;

                if (!cur.equals(toAdd, true, true)) continue; // same id/meta/nbt
                int max = Math.min(getMaxStackSize(), cur.getMaxStackSize());
                if (cur.getCount() >= max) continue;

                int can = max - cur.getCount();
                int move = Math.min(can, toAdd.getCount());
                if (move <= 0) continue;

                Item newCur = cur.clone();
                newCur.setCount(cur.getCount() + move);
                v.setItem(i, newCur, true);

                toAdd.setCount(toAdd.getCount() - move);
                if (toAdd.getCount() <= 0) break;
            }

            while (toAdd.getCount() > 0) {
                int empty = firstEmpty(toAdd);
                if (empty == -1) break;

                int max = Math.min(getMaxStackSize(), toAdd.getMaxStackSize());
                int move = Math.min(max, toAdd.getCount());

                Item placed = toAdd.clone();
                placed.setCount(move);
                v.setItem(empty, placed, true);

                toAdd.setCount(toAdd.getCount() - move);
            }

            if (toAdd.getCount() > 0) {
                left.add(toAdd);
            }
        }

        return left.toArray(Item[]::new);
    }

    @Override
    public boolean canAddItem(Item item) {
        if (item == null || item.isNull() || item.getId() == 0 || item.getCount() <= 0) return true;

        for (int i = 0; i < getSize(); i++) {
            Item cur = v.getItem(i);
            if (cur == null || cur.isNull() || cur.getId() == 0) continue;
            if (!cur.equals(item, true, true)) continue;

            int max = Math.min(getMaxStackSize(), cur.getMaxStackSize());
            if (cur.getCount() < max) return true;
        }

        return firstEmpty(item) != -1;
    }

    @Override
    public boolean allowedToAdd(Item item) {
        return true;
    }

    @Override
    public Item[] removeItem(Item... items) {
        List<Item> left = new ArrayList<>();
        if (items == null) return new Item[0];

        for (Item req : items) {
            if (req == null || req.isNull() || req.getId() == 0 || req.getCount() <= 0) continue;

            Item need = req.clone();

            for (int i = 0; i < getSize(); i++) {
                if (need.getCount() <= 0) break;

                Item cur = v.getItem(i);
                if (cur == null || cur.isNull() || cur.getId() == 0) continue;
                if (!cur.equals(need, true, true)) continue;

                int take = Math.min(cur.getCount(), need.getCount());
                int newCount = cur.getCount() - take;

                if (newCount <= 0) {
                    v.setItem(i, Item.AIR_ITEM, true);
                } else {
                    Item newCur = cur.clone();
                    newCur.setCount(newCount);
                    v.setItem(i, newCur, true);
                }

                need.setCount(need.getCount() - take);
            }

            if (need.getCount() > 0) left.add(need);
        }

        return left.toArray(Item[]::new);
    }

    @Override
    public Map<Integer, Item> getContents() {
        HashMap<Integer, Item> map = new HashMap<>();
        for (int i = 0; i < v.getSize(); i++) {
            Item it = v.getItem(i);
            if (it != null && !it.isNull() && it.getId() != 0) {
                map.put(i, it);
            }
        }
        return map;
    }

    @Override
    public void setContents(Map<Integer, Item> map) {
        for (int i = 0; i < getSize(); i++) {
            v.setItem(i, Item.AIR_ITEM, false);
        }
        if (map != null) {
            for (Map.Entry<Integer, Item> e : map.entrySet()) {
                int slot = e.getKey();
                if (slot < 0 || slot >= getSize()) continue;
                Item it = e.getValue();
                v.setItem(slot, it == null ? Item.AIR_ITEM : it, false);
            }
        }
        v.syncContents();
    }

    @Override
    public void sendContents(Player player) {
        v.syncContents();
    }

    @Override
    public void sendContents(Player... players) {
        v.syncContents();
    }

    @Override
    public void sendContents(Collection<Player> collection) {
        v.syncContents();
    }

    @Override
    public void sendSlot(int index, Player player) {
        v.syncSlot(index);
    }

    @Override
    public void sendSlot(int index, Player... players) {
        v.syncSlot(index);
    }

    @Override
    public void sendSlot(int index, Collection<Player> collection) {
        v.syncSlot(index);
    }

    @Override
    public boolean contains(Item item) {
        return first(item, true) != -1;
    }

    @Override
    public Map<Integer, Item> all(Item item) {
        HashMap<Integer, Item> found = new HashMap<>();
        if (item == null || item.isNull() || item.getId() == 0) return found;

        for (int i = 0; i < getSize(); i++) {
            Item cur = v.getItem(i);
            if (cur == null || cur.isNull() || cur.getId() == 0) continue;
            if (cur.equals(item, true, true)) {
                found.put(i, cur);
            }
        }
        return found;
    }

    @Override
    public int first(Item item, boolean exact) {
        if (item == null || item.isNull() || item.getId() == 0) return -1;
        for (int i = 0; i < getSize(); i++) {
            Item cur = v.getItem(i);
            if (cur == null || cur.isNull() || cur.getId() == 0) continue;

            if (exact) {
                if (cur.equals(item, true, true)) return i;
            } else {
                if (cur.getId() == item.getId()) return i;
            }
        }
        return -1;
    }

    @Override
    public int firstEmpty(Item item) {
        for (int i = 0; i < getSize(); i++) {
            Item cur = v.getItem(i);
            if (cur == null || cur.isNull() || cur.getId() == 0) return i;
        }
        return -1;
    }

    @Override
    public void decreaseCount(int slot) {
        Item cur = v.getItem(slot);
        if (cur == null || cur.isNull() || cur.getId() == 0) return;

        int c = cur.getCount() - 1;
        if (c <= 0) v.setItem(slot, Item.AIR_ITEM, true);
        else {
            Item n = cur.clone();
            n.setCount(c);
            v.setItem(slot, n, true);
        }
    }

    @Override
    public void remove(Item item) {
        if (item == null || item.isNull() || item.getId() == 0) return;
        for (int i = 0; i < getSize(); i++) {
            Item cur = v.getItem(i);
            if (cur == null || cur.isNull() || cur.getId() == 0) continue;
            if (cur.equals(item, true, true)) {
                v.setItem(i, Item.AIR_ITEM, true);
            }
        }
    }

    @Override
    public boolean clear(int slot, boolean send) {
        if (slot < 0 || slot >= getSize()) return false;
        Item cur = v.getItem(slot);
        boolean had = cur != null && !cur.isNull() && cur.getId() != 0;
        v.setItem(slot, Item.AIR_ITEM, send);
        return had;
    }

    @Override
    public void clearAll() {
        for (int i = 0; i < getSize(); i++) {
            v.setItem(i, Item.AIR_ITEM, false);
        }
        v.syncContents();
    }

    @Override
    public boolean isFull() {
        return firstEmpty(Item.AIR_ITEM) == -1;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getSize(); i++) {
            Item cur = v.getItem(i);
            if (cur != null && !cur.isNull() && cur.getId() != 0) return false;
        }
        return true;
    }

    @Override
    public InventoryType getType() {
        return v.getSize() > 27 ? InventoryType.DOUBLE_CHEST : InventoryType.CHEST;
    }

    @Override
    public String getName() {
        return v.getName();
    }

    @Override
    public String getTitle() {
        return v.getName();
    }

    @Override
    public InventoryHolder getHolder() {
        return holder;
    }


    @Override
    public Set<Player> getViewers() {
        return Set.copyOf(v.getViewers());
    }

    @Override
    public boolean open(Player who) {
        onOpen(who);
        return true;
    }

    @Override
    public void onOpen(Player player) {
    }

    @Override
    public void close(Player who) {
        onClose(who);
    }

    @Override
    public void onClose(Player player) {
    }

    @Override
    public void onSlotChange(int slot, Item oldItem, boolean send) {
        if (send) v.syncSlot(slot);
    }
}