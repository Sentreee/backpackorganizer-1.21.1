package net.sentree.backpackorganizer.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.sentree.backpackorganizer.item.ModItems;

public class StorageManagerInventory implements Inventory {
    public static final int SIZE = 3;

    private final PlayerEntity player;
    private final Hand hand;
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);

    public StorageManagerInventory(PlayerEntity player, Hand hand) {
        this.player = player;
        this.hand = hand;

        ItemStack host = hostStack();
        ContainerComponent cc = host.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        cc.copyTo(stacks);
    }

    private ItemStack hostStack() {
        return player.getStackInHand(hand);
    }

    private void save() {
        ItemStack host = hostStack();
        if (!host.isOf(ModItems.STORAGEMANAGER)) return;
        host.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
    }

    @Override public int size() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return stacks.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(stacks, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(stacks, slot);
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        markDirty();
    }

    @Override
    public void markDirty() {
        save();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return player.getStackInHand(hand).isOf(ModItems.STORAGEMANAGER);
    }

    @Override
    public void clear() {
        stacks.clear();
        markDirty();
    }
}
