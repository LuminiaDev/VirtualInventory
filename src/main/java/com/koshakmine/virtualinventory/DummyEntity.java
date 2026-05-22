package com.koshakmine.virtualinventory;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.custom.CustomEntity;
import cn.nukkit.entity.custom.EntityDefinition;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

public class DummyEntity extends Entity implements CustomEntity {

    public static final EntityDefinition definition = new EntityDefinition(
            "kora:virtual_chest",
            null,
            false,
            "virtual_chest",
            DummyEntity.class
    );

    public DummyEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public EntityDefinition getEntityDefinition() {
        return definition;
    }

    @Override
    public int getNetworkId() {
        return 100;
    }
}
