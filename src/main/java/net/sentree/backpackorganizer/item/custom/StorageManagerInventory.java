package net.sentree.backpackorganizer.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

/**
 * Inventory stored inside the Storage Manager item.
 *
 * NOTE: client-side screen handlers will also construct this inventory for prediction/rendering.
 * We MUST NOT write changes back into the held ItemStack on the client, or you get ghost items/desync.
 */
public class StorageManagerInventory implements Inventory {
    private final PlayerEntity player;
    private final Hand hand;
    private final DefaultedList<ItemStack> stacks;
    private final boolean isClient;

    /**
     * Uses the held stack's tier to determine size (server-safe).
     */
    public StorageManagerInventory(PlayerEntity player, Hand hand) {
        this(player, hand, -1);
    }

    /**
     * Uses a forced size (used by screen opening data so client/server never disagree).
     * If forcedSize <= 0, falls back to reading the size from the held stack.
     */
    public StorageManagerInventory(PlayerEntity player, Hand hand, int forcedSize) {
        this.player = player;
        this.hand = hand;
        this.isClient = player.getWorld().isClient();

        ItemStack host = getHostStack();

        int size;
        if (forcedSize > 0) {
            size = Math.max(1, forcedSize);
        } else {
            size = 3;
            if (host.getItem() instanceof StorageManagerItem sm) {
                size = Math.max(1, sm.getSlots());
            }
        }

        this.stacks = DefaultedList.ofSize(size, ItemStack.EMPTY);

        // Load from the manager item's CONTAINER component
        ContainerComponent cc = host.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        // copyTo copies as much as fits in the list size
        cc.copyTo(stacks);
    }

    private ItemStack getHostStack() {
        return player.getStackInHand(hand);
    }

    private void saveToHost() {
        // Never write on the client (prevents ghost items / dupe-like desync)
        if (isClient) return;

        ItemStack host = getHostStack();
        if (!(host.getItem() instanceof StorageManagerItem)) return;

        // Write the component
        host.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));

        // Explicitly write back to hand so the server syncs the updated stack to the client
        player.setStackInHand(hand, host);
    }

    @Override
    public int size() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : stacks) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack taken = Inventories.splitStack(stacks, slot, amount);
        if (!taken.isEmpty()) markDirty();
        return taken;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack taken = Inventories.removeStack(stacks, slot);
        if (!taken.isEmpty()) markDirty();
        return taken;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        markDirty();
    }

    @Override
    public void markDirty() {
        saveToHost();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        markDirty();
    }
}
