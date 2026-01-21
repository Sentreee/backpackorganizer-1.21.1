package net.sentree.backpackorganizer.item.basic;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sentree.backpackorganizer.util.ModTags;

public class StorageManagerItem extends Item {
    private final int slots;

    public StorageManagerItem(Settings settings, int slots) {
        super(settings);
        this.slots = Math.max(1, slots);
    }

    public int getSlots() {
        return slots;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            sp.openHandledScreen(new ExtendedScreenHandlerFactory<Hand>() {
                @Override
                public Hand getScreenOpeningData(ServerPlayerEntity player) {
                    return hand;
                }

                @Override
                public Text getDisplayName() {
                    // Title becomes the item name (ex: "Copper Storage Manager")
                    return sp.getStackInHand(hand).getName();
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                    return new StorageManagerScreenHandler(syncId, inv, hand);
                }
            });
        }

        return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
    }

    public static boolean isAllowedStoredItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Never allow putting any Storage Manager tier inside itself
        if (stack.getItem() instanceof StorageManagerItem) return false;

        // Tag allowlist
        if (stack.isIn(ModTags.Items.NBT_CONTAINER_ITEMS)) return true;

        // Runtime support for Sophisticated Backpacks variants without hard dependency
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if ("sophisticatedbackpacks".equals(id.getNamespace()) && id.getPath().endsWith("backpack")) {
            return true;
        }

        // Heuristics for vanilla-like containers
        return stack.contains(DataComponentTypes.CONTAINER)
                || stack.contains(DataComponentTypes.BUNDLE_CONTENTS)
                || stack.contains(DataComponentTypes.BLOCK_ENTITY_DATA);
    }
}
