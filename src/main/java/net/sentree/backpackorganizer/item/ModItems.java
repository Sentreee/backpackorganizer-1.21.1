package net.sentree.backpackorganizer.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.sentree.backpackorganizer.Backpackorganizer;
import net.sentree.backpackorganizer.item.custom.StorageManagerItem;

public class ModItems{
    public static final Item STORAGEMANAGER = registerItem("storagemanager", new StorageManagerItem(new Item.Settings().maxCount(1)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Backpackorganizer.MOD_ID, name), item);
    }

    public static void registerModItems() {
        Backpackorganizer.LOGGER.info("Registering Mod Items for " + Backpackorganizer.MOD_ID);
    }
}
