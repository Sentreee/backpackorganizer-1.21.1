package net.sentree.backpackorganizer.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

public class StorageManagerInventory implements Inventory {
    private final PlayerEntity player;
    private final Hand hand;
    private final DefaultedList<ItemStack> stacks;

    public StorageManagerInventory(PlayerEntity player, Hand hand) {
        this.player = player;
        this.hand = hand;

        ItemStack host = getHostStack();

        int size = 3;
        if (host.getItem() instanceof StorageManagerItem sm) {
            size = Math.max(1, sm.getSlots());
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
        ItemStack host = getHostStack();

        // Write the component
        host.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));

        // ✅ Explicitly write back to hand to ensure it persists
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
        ItemStack taken = net.minecraft.inventory.Inventories.splitStack(stacks, slot, amount);
        if (!taken.isEmpty()) markDirty();
        return taken;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack taken = net.minecraft.inventory.Inventories.removeStack(stacks, slot);
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
