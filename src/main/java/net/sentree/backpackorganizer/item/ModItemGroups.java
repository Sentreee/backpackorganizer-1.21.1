package net.sentree.backpackorganizer.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sentree.backpackorganizer.Backpackorganizer;

public class ModItemGroups {
    public static final ItemGroup BACKPACK_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(Backpackorganizer.MOD_ID, "backpack_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.STORAGEMANAGER))
                    .displayName(Text.translatable("itemgroup.backpackorganizer.backpack_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.STORAGEMANAGER);
                        entries.add(ModItems.STORAGEMANAGER_COPPER);

                    }).build());


    public static void registerItemGroups() {
        Backpackorganizer.LOGGER.info("Registering Item Groups for " + Backpackorganizer.MOD_ID);
    }
}
