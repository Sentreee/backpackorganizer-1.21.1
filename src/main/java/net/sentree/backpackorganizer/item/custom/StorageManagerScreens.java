package net.sentree.backpackorganizer.item.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
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

    // lets an anonymous class "implement both" (Java only allows 1 interface in anon class)
    private interface NoCloseFactory<D> extends ExtendedScreenHandlerFactory<D>, FabricScreenHandlerFactory {}

    public static boolean isEditableContainer(ItemStack stack) {
        // Vanilla-ish
        if (stack.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) return true;
        if (stack.contains(DataComponentTypes.CONTAINER)) return true;

        // Fabric Transfer “Item Storage” (Sophisticated Backpacks uses this in the port)
        Storage<ItemVariant> api = ContainerItemContext.withConstant(stack).find(ItemStorage.ITEM);
        return api instanceof SlottedStorage<ItemVariant>;
    }

    public static void openManager(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);

        int computedSlots = 3;
        if (held.getItem() instanceof StorageManagerItem sm) {
            computedSlots = sm.getSlots();
        }
        final int slots = computedSlots; // capture-safe for the anon class

        player.openHandledScreen(new NoCloseFactory<ModScreenHandlers.ManagerOpenData>() {
            @Override public boolean shouldCloseCurrentScreen() { return false; }

            @Override
            public ModScreenHandlers.ManagerOpenData getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
                return new ModScreenHandlers.ManagerOpenData(hand, slots);
            }

            @Override
            public Text getDisplayName() {
                return held.getName();
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                return new StorageManagerScreenHandler(syncId, inv, hand, slots);
            }
        });
    }

    public static void openContainer(PlayerEntity player, Hand hand, int slotIndex, ItemStack target) {
        int containerSlots = computeSlotsFor(target);

        player.openHandledScreen(new NoCloseFactory<ModScreenHandlers.ContainerOpenData>() {
            @Override public boolean shouldCloseCurrentScreen() { return false; }

            @Override
            public ModScreenHandlers.ContainerOpenData getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
                return new ModScreenHandlers.ContainerOpenData(hand, slotIndex, containerSlots);
            }

            @Override
            public Text getDisplayName() {
                return target.getName();
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                return new StorageManagerContainerScreenHandler(syncId, inv, hand, slotIndex, containerSlots);
            }
        });
    }

    /**
     * Computes the total slot count for the target container.
     *
     * This is sent to the client so it can pick the correct GUI/layout (gold/diamond/netherite),
     * and so the server/client never disagree on the container size.
     */
    private static int computeSlotsFor(ItemStack stack) {
        // Shulker-like always 27 slots
        if (stack.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) return 27;

        // Transfer API-backed containers (Sophisticated Backpacks, etc.)
        Storage<ItemVariant> api = ContainerItemContext.withConstant(stack).find(ItemStorage.ITEM);
        if (api instanceof SlottedStorage<ItemVariant> slotted) {
            return Math.max(1, slotted.getSlots().size());
        }

        // Component containers (best-effort) -> infer rows from current content count, clamp to 6 rows
        if (stack.contains(DataComponentTypes.CONTAINER)) {
            ContainerComponent cc = stack.get(DataComponentTypes.CONTAINER);
            int represented = (cc == null) ? 0 : (int) cc.stream().count();
            int rows9 = ceilDiv(Math.max(9, represented), 9);
            rows9 = clamp(rows9, 1, 6);
            return rows9 * 9;
        }

        // Fallback
        return 27;
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
