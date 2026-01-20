package net.sentree.backpackorganizer.util;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.sentree.backpackorganizer.Backpackorganizer;

// ModItemTags.java
public final class ModTags {
    public static class Items {
        public static final TagKey<Item> NBT_CONTAINER_ITEMS = createTag("nbt_container_items");

        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, Identifier.of(Backpackorganizer.MOD_ID, name));
        }
    }
}
