# VirtualInventory

FakeInventories is a simple library plugin for Lumi that will help you to create your custom virtual inventories with ease.

```java
VirtualInventory inventory = new VirtualInventory(9, "Example");
virtual.setPrefix("§F§l§a§g§r§T§i§t§l§e§r"); //Prefix to define how your inventory will look according to resourcepack

inventory.setDefaultItemHandler(handler -> {
    handler.player().sendMessage("This is default item handler");
    return true;
});

inventory.setItem(0, Item.get(ItemNamespaceId.DIAMOND), handler -> {
    handler.player().sendMessage("This is custom item handler");
    return true;
});

inventory.setOpenHandler(player -> player.sendMessage("You opened inventory"));
inventory.setCloseHandler(player -> player.sendMessage("You closed inventory"));

inventory.open(player);
```

## Gradle
Adding repo:
```kts
maven {
    name = "lumiRepositoryReleases"
    url = uri("https://repo.lumi.su/releases")
}
```

Adding dependency:
```kts
compileOnly("com.koshakmine.virtualinventory:VirtualInventory:1.0.0")
```

## Maven
Adding repo:
```xml
<repository>
    <id>lumi-repository-releases</id>
    <url>https://repo.lumi.su/releases</url>
</repository>
```

Adding dependency:
```xml
<dependency>
    <groupId>com.koshakmine.virtualinventory</groupId>
    <artifactId>VirtualInventory</artifactId>
    <version>1.0.0</version>
</dependency>
```