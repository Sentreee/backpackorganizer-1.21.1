package net.sentree.backpackorganizer.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

public class StoredItemContainerInventory implements Inventory {
    private final StorageManagerInventory managerInv;
    private final int managerSlot;
    private final DefaultedList<ItemStack> stacks;
    private final RegistryWrapper.WrapperLookup registries;
    private final boolean isClient;
    private final Mode mode;

    private enum Mode { BLOCK_ENTITY_DATA, COMPONENT_CONTAINER, NONE }

    public StoredItemContainerInventory(PlayerEntity player,
                                        StorageManagerInventory managerInv,
                                        int managerSlot,
                                        int rows,
                                        boolean isClient) {
        this.managerInv = managerInv;
        this.isClient = isClient;

        // WrapperLookup required for Inventories.readNbt/writeNbt in 1.21.x
        this.registries = player.getWorld().getRegistryManager();

        this.stacks = DefaultedList.ofSize(rows * 9, ItemStack.EMPTY);

        // Clamp slot for safety
        this.managerSlot = clamp(managerSlot, 0, Math.max(0, managerInv.size() - 1));

        ItemStack target = managerInv.getStack(this.managerSlot);
        if (target.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) {
            this.mode = Mode.BLOCK_ENTITY_DATA;
            loadFromBlockEntityData(target);
        } else if (target.contains(DataComponentTypes.CONTAINER)) {
            this.mode = Mode.COMPONENT_CONTAINER;
            loadFromContainerComponent(target);
        } else {
            this.mode = Mode.NONE;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void loadFromBlockEntityData(ItemStack target) {
        NbtComponent comp = target.getOrDefault(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = comp.copyNbt();

        // 3-arg signature in 1.21.x
        Inventories.readNbt(nbt, stacks, registries);
    }

    private void saveToBlockEntityData(ItemStack target) {
        NbtComponent comp = target.getOrDefault(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = comp.copyNbt();

        // Replace stored items cleanly
        nbt.remove("Items");

        // 3-arg signature in 1.21.x
        Inventories.writeNbt(nbt, stacks, registries);

        target.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(nbt));
    }

    private void loadFromContainerComponent(ItemStack target) {
        ContainerComponent cc = target.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        cc.copyTo(stacks);
    }

    private void saveToContainerComponent(ItemStack target) {
        target.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
    }

    @Override public int size() { return stacks.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : stacks) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

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
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        markDirty();
    }

    @Override
    public void clear() {
        // IMPORTANT: don't shrink the list; set all to empty
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        markDirty();
    }

    @Override
    public void markDirty() {
        if (isClient || mode == Mode.NONE) return;

        ItemStack target = managerInv.getStack(managerSlot);
        if (target.isEmpty()) return;

        if (mode == Mode.BLOCK_ENTITY_DATA) {
            saveToBlockEntityData(target);
        } else if (mode == Mode.COMPONENT_CONTAINER) {
            saveToContainerComponent(target);
        }

        // Write back into manager slot; StorageManagerInventory.setStack() persists to the manager item
        managerInv.setStack(managerSlot, target);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }
}
