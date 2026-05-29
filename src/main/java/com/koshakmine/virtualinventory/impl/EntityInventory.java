package com.koshakmine.virtualinventory.impl;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.ContainerOpenPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import cn.nukkit.network.protocol.types.EntityLink;
import cn.nukkit.network.protocol.types.inventory.ContainerSlotType;
import cn.nukkit.network.protocol.types.inventory.ContainerType;
import com.koshakmine.virtualinventory.VirtualInventory;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class EntityInventory extends VirtualInventory {

    public long eId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE - 1000L, Long.MAX_VALUE);

    public EntityInventory(int size, String name) {
        super(size, name);
    }

    @Override
    protected ContainerSlotType getSlotType() {
        return ContainerSlotType.LEVEL_ENTITY;
    }

    @Override
    protected DataPacket[] sendInventory(Player player, String name, byte windowId){
        ArrayList<DataPacket> packets = new ArrayList<>();

        EntityMetadata metadata = new EntityMetadata();
        metadata.putString(Entity.DATA_NAMETAG, this.getPrefix() + name);
        metadata.putByte(Entity.DATA_CONTAINER_TYPE, 0);
        metadata.putInt(Entity.DATA_CONTAINER_BASE_SIZE, getSize());

        AddEntityPacket addEntity = new AddEntityPacket();
        addEntity.entityUniqueId = eId;
        addEntity.entityRuntimeId = eId;
        addEntity.id = "kora:virtual_chest";
        addEntity.type = 0;
        addEntity.x = (float) player.x;
        addEntity.y = (float) player.y;
        addEntity.z = (float) player.z;
        addEntity.speedX = 0;
        addEntity.speedY = 0;
        addEntity.speedZ = 0;
        addEntity.yaw = 0;
        addEntity.pitch = 0;
        addEntity.headYaw = 0;
        addEntity.bodyYaw = 0;
        addEntity.metadata = metadata;
        addEntity.links = new EntityLink[]{new EntityLink(player.getId(), eId, EntityLink.TYPE_RIDER, true, true, 0)};

        var containerOpen = new ContainerOpenPacket();
        containerOpen.windowId = windowId;
        containerOpen.type = ContainerType.CONTAINER.getId();
        containerOpen.entityId = eId;
        containerOpen.x = 0;
        containerOpen.y = 0;
        containerOpen.z = 0;

        try {
            var field = Player.class.getDeclaredField("inventoryOpen");
            field.setAccessible(true);
            field.set(player, true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        packets.add(addEntity);
        packets.add(containerOpen);

        return packets.toArray(DataPacket[]::new);
    }

    @Override
    protected DataPacket[] removeInventory(Player player) {
        RemoveEntityPacket removeEntity = new RemoveEntityPacket();
        removeEntity.eid = eId;

        try {
            var field = Player.class.getDeclaredField("inventoryOpen");
            field.setAccessible(true);
            field.set(player, false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return new DataPacket[]{removeEntity};
    }
}
