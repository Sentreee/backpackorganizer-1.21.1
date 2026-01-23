package net.sentree.backpackorganizer.item.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.sentree.backpackorganizer.util.ModTags;

import java.util.List;

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
        if (!world.isClient) {
            StorageManagerScreens.openManager(user, hand);
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

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        // Your storage manager saves its internal inventory here
        if (!stack.contains(DataComponentTypes.CONTAINER)) return;

        int slotCount = this.slots; // your field (3/5/7/etc per tier)
        DefaultedList<ItemStack> stored = DefaultedList.ofSize(slotCount, ItemStack.EMPTY);

        ContainerComponent cc = stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        cc.copyTo(stored);

        int totalNonEmpty = 0;
        for (ItemStack s : stored) if (!s.isEmpty()) totalNonEmpty++;
        if (totalNonEmpty == 0) return;

        int shown = 0;
        for (ItemStack s : stored) {
            if (s.isEmpty()) continue;
            if (shown >= 5) break;

            // Uses the same vanilla translation key shulker tooltips use: "%s x%s"
            tooltip.add(Text.translatable("container.shulkerBox.itemCount", s.getName(), s.getCount())
                    .formatted(Formatting.GRAY));
            shown++;
        }

        int remaining = totalNonEmpty - shown;
        if (remaining > 0) {
            tooltip.add(Text.translatable("container.shulkerBox.more", remaining)
                    .formatted(Formatting.GRAY, Formatting.ITALIC));
        }
    }
}
