package net.sentree.backpackorganizer.item.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.sentree.backpackorganizer.util.ModScreenHandlers;

public final class StorageManagerScreens {
    private StorageManagerScreens() {}

    // Allows anon factory to implement both interfaces
    private interface NoCloseFactory<D> extends ExtendedScreenHandlerFactory<D>, FabricScreenHandlerFactory {}

    /** Can we open an "edit contents" screen for this stack? */
    public static boolean isEditableContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Shulkers + BE-style container items
        if (stack.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) return true;

        // Some containers/backpacks use CONTAINER component
        if (stack.contains(DataComponentTypes.CONTAINER)) return true;

        // Bundles use BUNDLE_CONTENTS (you can decide later if you actually implement bundle editing)
        return stack.contains(DataComponentTypes.BUNDLE_CONTENTS);
    }

    public static void openManager(PlayerEntity player, Hand hand) {
        player.openHandledScreen(new NoCloseFactory<Hand>() {
            @Override
            public boolean shouldCloseCurrentScreen() {
                return false; // prevents cursor recenter when switching tabs
            }

            @Override
            public Hand getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
                return hand;
            }

            @Override
            public Text getDisplayName() {
                // Show the name of the held manager item (Basic/Copper/etc)
                return player.getStackInHand(hand).getName();
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                return new StorageManagerScreenHandler(syncId, inv, hand);
            }
        });
    }

    public static void openContainer(PlayerEntity player, Hand hand, int slotIndex, ItemStack target) {
        int rows = computeRowsFor(target);

        player.openHandledScreen(new NoCloseFactory<ModScreenHandlers.ContainerOpenData>() {
            @Override
            public boolean shouldCloseCurrentScreen() {
                return false; // prevents cursor recenter
            }

            @Override
            public ModScreenHandlers.ContainerOpenData getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
                return new ModScreenHandlers.ContainerOpenData(hand, slotIndex, rows);
            }

            @Override
            public Text getDisplayName() {
                // Show the opened container's item name (e.g. "Red Shulker Box")
                return target.getName();
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                return new StorageManagerContainerScreenHandler(syncId, inv, hand, slotIndex, rows);
            }
        });
    }

    private static int computeRowsFor(ItemStack stack) {
        // Shulker boxes (and most BE-style container items) should always show full size.
        // Shulkers are 27 slots = 3 rows.
        if (stack.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) {
            return 3;
        }

        // Default: 3 rows
        int rows = 3;

        // For component-based containers, try best-effort sizing,
        // but never smaller than 3 (you requested full display, not "only used rows")
        if (stack.contains(DataComponentTypes.CONTAINER)) {
            ContainerComponent cc = stack.get(DataComponentTypes.CONTAINER);
            int represented = (cc == null) ? 0 : (int) cc.stream().count();

            int slots = Math.max(27, roundUpTo9(represented)); // at least 27 slots (3 rows)
            rows = clamp(slots / 9, 3, 6);                    // 3..6 rows
        }

        return rows;
    }

    private static int roundUpTo9(int v) { return ((v + 8) / 9) * 9; }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
