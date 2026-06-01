package com.koshakmine.virtualinventory;

import cn.nukkit.plugin.PluginBase;

public class Bootstrap extends PluginBase {

    @Override
    public void onLoad() {
        new InventoryHooker(this);
    }
}
