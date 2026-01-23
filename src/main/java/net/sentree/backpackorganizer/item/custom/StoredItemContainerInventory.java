package net.sentree.backpackorganizer.item.custom;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class StoredItemContainerInventory implements Inventory {
    private final StorageManagerInventory managerInv;
    private final int managerSlot;
    private final RegistryWrapper.WrapperLookup registries;
    private final boolean isClient;

    /** Slot count the screen handler expects (authoritative from opening data). */
    private final int expectedSlots;

    private enum Mode { BLOCK_ENTITY_DATA, COMPONENT_CONTAINER, CUSTOM_DATA_ITEMS, TRANSFER_ITEM_STORAGE, NONE }
    private final Mode mode;

    /** If mode == CUSTOM_DATA_ITEMS: where the item list lives inside the item's custom NBT. */
    private final String[] customItemsPath;

    // For vanilla-ish modes:
    private final DefaultedList<ItemStack> stacks;

    // For transfer mode:
    private final List<SingleSlotStorage<ItemVariant>> transferSlots;

    public StoredItemContainerInventory(PlayerEntity player,
                                        StorageManagerInventory managerInv,
                                        int managerSlot,
                                        int slotCount,
                                        boolean isClient) {
        this.managerInv = managerInv;
        this.managerSlot = managerSlot;
        this.isClient = isClient;
        this.registries = player.getWorld().getRegistryManager();

        this.expectedSlots = Math.max(1, slotCount);
        int safeSlotCount = this.expectedSlots;

        ItemStack target = managerInv.getStack(managerSlot);

        // 1) Shulker / BE-data
        if (target.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) {
            this.mode = Mode.BLOCK_ENTITY_DATA;
            this.transferSlots = List.of();
            this.stacks = DefaultedList.ofSize(safeSlotCount, ItemStack.EMPTY);
            this.customItemsPath = null;
            loadFromBlockEntityData(target);
            return;
        }

        // 2) Component container
        if (target.contains(DataComponentTypes.CONTAINER)) {
            this.mode = Mode.COMPONENT_CONTAINER;
            this.transferSlots = List.of();
            this.stacks = DefaultedList.ofSize(safeSlotCount, ItemStack.EMPTY);
            this.customItemsPath = null;
            loadFromContainerComponent(target);
            return;
        }

        // 3) Custom data item list (many mod backpacks store their inventory here)
        // We use this BEFORE the Transfer API because the Transfer API often behaves like
        // "automation insertion" (fills first slots) rather than true slot-addressable placement.
        if (hasCustomData(target)) {
            String[] path = findItemListPath(getCustomDataNbt(target));
            if (path != null) {
                this.mode = Mode.CUSTOM_DATA_ITEMS;
                this.transferSlots = List.of();
                this.customItemsPath = path;
                this.stacks = DefaultedList.ofSize(safeSlotCount, ItemStack.EMPTY);
                loadFromCustomData(target);
                return;
            }
        }

        // 4) Transfer API (Sophisticated Backpacks port exposes this)
        // Wrap your manager slot as a transfer slot so changes write back into the manager item.
        InventoryStorage invStorage = InventoryStorage.of(managerInv, null);
        SingleSlotStorage<ItemVariant> mainSlot = invStorage.getSlots().get(managerSlot);

        ContainerItemContext ctx = ContainerItemContext.ofPlayerSlot(player, mainSlot);
        Storage<ItemVariant> api = ctx.find(ItemStorage.ITEM);

        if (api instanceof SlottedStorage<ItemVariant> slotted) {
            this.mode = Mode.TRANSFER_ITEM_STORAGE;
            this.transferSlots = slotted.getSlots();
            this.customItemsPath = null;
            // IMPORTANT:
            // On the client, many container-items (e.g. Sophisticated Backpacks) do NOT have their
            // contents synced into the ItemStack immediately after login.
            // If we read live from the Transfer API on the client, the GUI will appear empty until
            // another action (opening the backpack normally, shift-tooltip, etc.) triggers a sync.
            //
            // Vanilla screenhandlers already sync slot contents from server -> client, so we keep a
            // local mirror list on the client and let incoming slot updates populate it.
            // On the client, do NOT trust the Transfer API slot list size; always use the
            // screen-opening data (expectedSlots) so the handler's slots and this inventory agree.
            // The server will sync the visible slot contents via packets.
            this.stacks = isClient
                    ? DefaultedList.ofSize(safeSlotCount, ItemStack.EMPTY)
                    : DefaultedList.ofSize(0, ItemStack.EMPTY);
            return;
        }

        // 5) Unsupported
        this.mode = Mode.NONE;
        this.transferSlots = List.of();
        this.stacks = DefaultedList.ofSize(safeSlotCount, ItemStack.EMPTY);
        this.customItemsPath = null;
    }

    private static boolean hasCustomData(ItemStack stack) {
        // Yarn 1.21+ uses Custom Data as an NbtComponent in DataComponentTypes.
        // Not all items will have it.
        try {
            return stack.contains(DataComponentTypes.CUSTOM_DATA);
        } catch (Throwable t) {
            return false;
        }
    }

    private static NbtCompound getCustomDataNbt(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }

    private void setCustomDataNbt(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    /** Returns a path (compound keys) that leads to an NbtList of item stack entries, or null. */
    private static String[] findItemListPath(NbtCompound root) {
        // Common case: top-level "Items"
        if (looksLikeItemList(root, "Items")) {
            return new String[]{"Items"};
        }

        // Otherwise: scan a couple levels deep for the first list that looks like an inventory.
        // (Keeps this fast and avoids surprising edits to totally unrelated NBT.)
        for (String k1 : root.getKeys()) {
            if (root.get(k1) instanceof NbtCompound c1) {
                if (looksLikeItemList(c1, "Items")) return new String[]{k1, "Items"};

                for (String k2 : c1.getKeys()) {
                    if (c1.get(k2) instanceof NbtCompound c2) {
                        if (looksLikeItemList(c2, "Items")) return new String[]{k1, k2, "Items"};
                    }
                }
            }

            // Some mods store under a non-"Items" key; check any top-level list.
            if (root.contains(k1, NbtElement.LIST_TYPE) && looksLikeItemList(root, k1)) {
                return new String[]{k1};
            }
        }

        return null;
    }

    private static boolean looksLikeItemList(NbtCompound c, String key) {
        if (!c.contains(key, NbtElement.LIST_TYPE)) return false;
        NbtList list;
        try {
            list = c.getList(key, NbtElement.COMPOUND_TYPE);
        } catch (Throwable t) {
            return false;
        }
        if (list.isEmpty()) return true; // empty inventory is valid

        // Heuristic: at least one entry with Slot + (id/item) fields.
        int samples = Math.min(5, list.size());
        for (int i = 0; i < samples; i++) {
            if (!(list.get(i) instanceof NbtCompound e)) continue;
            boolean hasSlot = e.contains("Slot", NbtElement.BYTE_TYPE) || e.contains("Slot", NbtElement.INT_TYPE);
            boolean hasId = e.contains("id", NbtElement.STRING_TYPE) || e.contains("item", NbtElement.STRING_TYPE);
            boolean hasCount = e.contains("Count", NbtElement.BYTE_TYPE) || e.contains("count", NbtElement.INT_TYPE);
            if (hasSlot && hasId && hasCount) return true;
        }
        return false;
    }

    private NbtCompound getCompoundAtPath(NbtCompound root, String[] path, boolean create) {
        NbtCompound cur = root;
        for (int i = 0; i < path.length - 1; i++) {
            String k = path[i];
            if (cur.get(k) instanceof NbtCompound next) {
                cur = next;
            } else if (create) {
                NbtCompound next = new NbtCompound();
                cur.put(k, next);
                cur = next;
            } else {
                return null;
            }
        }
        return cur;
    }

    private NbtList getItemListFromCustomData(NbtCompound root) {
        if (customItemsPath == null || customItemsPath.length == 0) return new NbtList();
        NbtCompound parent = getCompoundAtPath(root, customItemsPath, false);
        if (parent == null) return new NbtList();
        String last = customItemsPath[customItemsPath.length - 1];
        if (!parent.contains(last, NbtElement.LIST_TYPE)) return new NbtList();
        return parent.getList(last, NbtElement.COMPOUND_TYPE);
    }

    private void setItemListIntoCustomData(NbtCompound root, NbtList list) {
        if (customItemsPath == null || customItemsPath.length == 0) return;
        NbtCompound parent = getCompoundAtPath(root, customItemsPath, true);
        if (parent == null) return;
        String last = customItemsPath[customItemsPath.length - 1];
        parent.put(last, list);
    }

    private void loadFromBlockEntityData(ItemStack target) {
        NbtComponent comp = target.getOrDefault(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = comp.copyNbt();
        Inventories.readNbt(nbt, stacks, registries);
    }

    private void saveToBlockEntityData(ItemStack target) {
        NbtComponent comp = target.getOrDefault(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = comp.copyNbt();
        nbt.remove("Items");
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

    private void loadFromCustomData(ItemStack target) {
        NbtCompound root = getCustomDataNbt(target);
        NbtList list = getItemListFromCustomData(root);

        // Inventories.readNbt expects the list under the key "Items".
        NbtCompound wrapper = new NbtCompound();
        wrapper.put("Items", list.copy());
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
        Inventories.readNbt(wrapper, stacks, registries);
    }

    private void saveToCustomData(ItemStack target) {
        NbtCompound root = getCustomDataNbt(target);
        NbtCompound wrapper = new NbtCompound();
        Inventories.writeNbt(wrapper, stacks, registries);
        NbtList items = wrapper.getList("Items", NbtElement.COMPOUND_TYPE);
        setItemListIntoCustomData(root, items);
        setCustomDataNbt(target, root);
    }

    @Override
    public int size() {
        return switch (mode) {
            case TRANSFER_ITEM_STORAGE -> (isClient ? stacks.size() : Math.min(expectedSlots, transferSlots.size()));
            default -> stacks.size();
        };
    }

    @Override
    public boolean isEmpty() {
        if (mode == Mode.TRANSFER_ITEM_STORAGE) {
            if (isClient) {
                return stacks.stream().allMatch(ItemStack::isEmpty);
            }
            for (var s : transferSlots) {
                if (!s.isResourceBlank() && s.getAmount() > 0) return false;
            }
            return true;
        }
        return stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        if (mode == Mode.TRANSFER_ITEM_STORAGE) {
            if (isClient) {
                return stacks.get(slot);
            }
            if (slot < 0 || slot >= size()) return ItemStack.EMPTY;
            var s = transferSlots.get(slot);
            if (s.isResourceBlank() || s.getAmount() <= 0) return ItemStack.EMPTY;

            ItemVariant v = s.getResource();
            int max = v.getItem().getMaxCount();
            int count = (int) Math.min((long) max, s.getAmount());
            return v.toStack(count);
        }
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (mode == Mode.TRANSFER_ITEM_STORAGE) {
            if (isClient) {
                // Client prediction only; server will resync.
                return Inventories.splitStack(stacks, slot, amount);
            }

            if (slot < 0 || slot >= size()) return ItemStack.EMPTY;

            var s = transferSlots.get(slot);
            if (s.isResourceBlank() || s.getAmount() <= 0) return ItemStack.EMPTY;

            ItemVariant v = s.getResource();
            long toExtract = Math.min(amount, s.getAmount());

            try (Transaction tx = Transaction.openOuter()) {
                long extracted = s.extract(v, toExtract, tx);
                tx.commit();
                managerInv.markDirty();
                return extracted <= 0 ? ItemStack.EMPTY : v.toStack((int) extracted);
            }
        }

        ItemStack taken = Inventories.splitStack(stacks, slot, amount);
        if (!taken.isEmpty()) markDirty();
        return taken;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (mode == Mode.TRANSFER_ITEM_STORAGE) {
            if (isClient) {
                // Client prediction only; server will resync.
                return Inventories.removeStack(stacks, slot);
            }

            if (slot < 0 || slot >= size()) return ItemStack.EMPTY;

            var s = transferSlots.get(slot);
            if (s.isResourceBlank() || s.getAmount() <= 0) return ItemStack.EMPTY;

            ItemVariant v = s.getResource();
            long toExtract = s.getAmount();

            try (Transaction tx = Transaction.openOuter()) {
                long extracted = s.extract(v, toExtract, tx);
                tx.commit();
                managerInv.markDirty();
                return extracted <= 0 ? ItemStack.EMPTY : v.toStack((int) extracted);
            }
        }

        ItemStack taken = Inventories.removeStack(stacks, slot);
        if (!taken.isEmpty()) markDirty();
        return taken;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (mode == Mode.TRANSFER_ITEM_STORAGE) {
            if (isClient) {
                if (slot < 0 || slot >= stacks.size()) return;
                stacks.set(slot, stack);
                return;
            }

            if (slot < 0 || slot >= size()) return;

            var s = transferSlots.get(slot);

            try (Transaction tx = Transaction.openOuter()) {
                if (!s.isResourceBlank() && s.getAmount() > 0) {
                    s.extract(s.getResource(), s.getAmount(), tx);
                }
                if (!stack.isEmpty()) {
                    s.insert(ItemVariant.of(stack), stack.getCount(), tx);
                }
                tx.commit();
            }
            managerInv.markDirty();
            return;
        }

        stacks.set(slot, stack);
        markDirty();
    }

    @Override
    public void markDirty() {
        if (isClient || mode == Mode.NONE || mode == Mode.TRANSFER_ITEM_STORAGE) return;

        ItemStack target = managerInv.getStack(managerSlot);
        if (target.isEmpty()) return;

        if (mode == Mode.BLOCK_ENTITY_DATA) saveToBlockEntityData(target);
        if (mode == Mode.COMPONENT_CONTAINER) saveToContainerComponent(target);
        if (mode == Mode.CUSTOM_DATA_ITEMS) saveToCustomData(target);

        managerInv.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        if (mode == Mode.TRANSFER_ITEM_STORAGE) {
            if (isClient) {
                for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
                return;
            }
            for (int i = 0; i < size(); i++) setStack(i, ItemStack.EMPTY);
            return;
        }
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        markDirty();
    }

    /**
     * For component/BE-data containers: pull the latest contents from the target ItemStack.
     *
     * This keeps the Storage Manager view in sync when the same backpack is modified by some
     * other UI while this screen is open.
     */
    public void refreshFromTarget() {
        if (isClient) return;
        ItemStack target = managerInv.getStack(managerSlot);
        if (target.isEmpty()) return;

        if (mode == Mode.BLOCK_ENTITY_DATA) {
            // Re-read into our list; no writes.
            for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
            loadFromBlockEntityData(target);
        } else if (mode == Mode.COMPONENT_CONTAINER) {
            for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
            loadFromContainerComponent(target);
        } else if (mode == Mode.CUSTOM_DATA_ITEMS) {
            for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
            loadFromCustomData(target);
        }
    }
}
