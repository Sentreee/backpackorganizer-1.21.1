package net.sentree.backpackorganizer.item.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.sentree.backpackorganizer.util.ModScreenHandlers;

public class StorageManagerContainerScreenHandler extends ScreenHandler {
    private final Hand hand;
    private final int managerSlot;

    private final int containerSlots;

    private final int cols;
    private final int totalRows;
    private final int visibleRows;

    private final boolean customTexture;

    // Slot anchors (in GUI space, not screen space)
    private final int gridX0;
    private final int gridY0;
    private final int playerX0;
    private final int playerInvY0;
    private final int hotbarY0;

    private int scrollRow = 0;

    private final boolean isClient;

    private final StorageManagerInventory organizerInv;
    private final StoredItemContainerInventory containerInv;
    private final ItemStack managerStack;

    /** A fixed-size view of the currently visible window of the container grid. */
    private final WindowInventory windowInv;

    public enum LayoutKind {
        VANILLA_9COL,
        GOLD_9x9,
        DIAMOND_12x9,
        NETHERITE_12x10
    }

    private final LayoutKind layoutKind;

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    /**
     * A fixed-size inventory used for the visible grid window.
     *
     * Why this exists:
     * - Vanilla networking applies S2C slot updates via Slot#setStackNoCallbacks(), which writes
     *   into the Slot's backing Inventory at its fixed index.
     * - If we create Slots with a dummy inventory index (like 0) and try to remap in Slot#getStack,
     *   the client will receive updates and write them into the wrong place ("jank" items).
     *
     * By giving each visible Slot a stable (0..visible-1) index into this WindowInventory, both
     * server and client update the correct visible slot. On the server, this inventory delegates
     * to the real backing container using the current scroll row.
     */
    private static final class WindowInventory implements Inventory {
        private final StoredItemContainerInventory backing;
        private final int visible;
        private final java.util.function.IntSupplier baseIndex;
        private final boolean isClient;
        private final DefaultedList<ItemStack> clientStacks;

        private WindowInventory(StoredItemContainerInventory backing,
                                int visible,
                                java.util.function.IntSupplier baseIndex,
                                boolean isClient) {
            this.backing = backing;
            this.visible = Math.max(1, visible);
            this.baseIndex = baseIndex;
            this.isClient = isClient;
            this.clientStacks = isClient ? DefaultedList.ofSize(this.visible, ItemStack.EMPTY)
                                         : DefaultedList.ofSize(0, ItemStack.EMPTY);
        }

        private int realIndex(int local) {
            return baseIndex.getAsInt() + local;
        }

        @Override
        public int size() {
            return visible;
        }

        @Override
        public boolean isEmpty() {
            if (isClient) {
                return clientStacks.stream().allMatch(ItemStack::isEmpty);
            }
            for (int i = 0; i < visible; i++) {
                if (!getStack(i).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getStack(int slot) {
            if (slot < 0 || slot >= visible) return ItemStack.EMPTY;
            if (isClient) return clientStacks.get(slot);

            int real = realIndex(slot);
            if (real < 0 || real >= backing.size()) return ItemStack.EMPTY;
            return backing.getStack(real);
        }

        @Override
        public ItemStack removeStack(int slot, int amount) {
            if (slot < 0 || slot >= visible) return ItemStack.EMPTY;
            if (isClient) {
                return Inventories.splitStack(clientStacks, slot, amount);
            }
            int real = realIndex(slot);
            if (real < 0 || real >= backing.size()) return ItemStack.EMPTY;
            return backing.removeStack(real, amount);
        }

        @Override
        public ItemStack removeStack(int slot) {
            if (slot < 0 || slot >= visible) return ItemStack.EMPTY;
            if (isClient) {
                return Inventories.removeStack(clientStacks, slot);
            }
            int real = realIndex(slot);
            if (real < 0 || real >= backing.size()) return ItemStack.EMPTY;
            return backing.removeStack(real);
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            if (slot < 0 || slot >= visible) return;
            if (isClient) {
                clientStacks.set(slot, stack);
                return;
            }
            int real = realIndex(slot);
            if (real < 0 || real >= backing.size()) return;
            backing.setStack(real, stack);
        }

        @Override
        public void markDirty() {
            if (!isClient) backing.markDirty();
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return true;
        }

        @Override
        public void clear() {
            if (isClient) {
                for (int i = 0; i < clientStacks.size(); i++) clientStacks.set(i, ItemStack.EMPTY);
                return;
            }
            for (int i = 0; i < visible; i++) setStack(i, ItemStack.EMPTY);
        }
    }

    public StorageManagerContainerScreenHandler(int syncId, PlayerInventory playerInv, Hand hand, int managerSlot, int containerSlots) {
        super(ModScreenHandlers.STORAGEMANAGER_CONTAINER, syncId);
        this.hand = hand;
        this.managerSlot = managerSlot;

        this.isClient = playerInv.player.getWorld().isClient();

        this.containerSlots = Math.max(1, containerSlots);

        // Pick layout strictly by slot count (requested)
        if (this.containerSlots == 81) {
            this.layoutKind = LayoutKind.GOLD_9x9;
            this.cols = 9;
            this.totalRows = 9;
            this.visibleRows = 9;
            this.customTexture = true;

            this.gridX0 = 8;
            this.gridY0 = 20;

            this.playerX0 = 8;
            this.playerInvY0 = 194;
            this.hotbarY0 = 252;

        } else if (this.containerSlots == 108) {
            this.layoutKind = LayoutKind.DIAMOND_12x9;
            this.cols = 12;
            this.totalRows = 9;
            this.visibleRows = 9;
            this.customTexture = true;

            this.gridX0 = 7;
            this.gridY0 = 19;

            this.playerX0 = 35;
            this.playerInvY0 = 200;
            this.hotbarY0 = 258;

        } else if (this.containerSlots == 120) {
            this.layoutKind = LayoutKind.NETHERITE_12x10;
            this.cols = 12;
            this.totalRows = 10;
            this.visibleRows = 10;
            this.customTexture = true;

            this.gridX0 = 7;
            this.gridY0 = 19;

            this.playerX0 = 35;
            this.playerInvY0 = 218;
            this.hotbarY0 = 276;

        } else if (this.containerSlots >= 120) {
            // Generic large container: use the netherite (12x10) layout with row scrolling
            this.layoutKind = LayoutKind.NETHERITE_12x10;
            this.cols = 12;
            this.totalRows = Math.max(1, ceilDiv(this.containerSlots, 12));
            this.visibleRows = Math.min(10, this.totalRows);
            this.customTexture = true;

            this.gridX0 = 7;
            this.gridY0 = 19;

            this.playerX0 = 35;
            this.playerInvY0 = 218;
            this.hotbarY0 = 276;

        } else {
            // Default: vanilla 9-col UI with scrolling (generic_54 style)
            this.layoutKind = LayoutKind.VANILLA_9COL;
            this.cols = 9;
            this.totalRows = Math.max(1, ceilDiv(this.containerSlots, 9));
            this.visibleRows = Math.min(6, this.totalRows);
            this.customTexture = false;

            this.gridX0 = 8;
            this.gridY0 = 18;

            this.playerX0 = 8;
            this.playerInvY0 = this.gridY0 + (this.visibleRows * 18) + 14;
            this.hotbarY0 = this.playerInvY0 + 58;
        }

        this.managerStack = playerInv.player.getStackInHand(hand);
        this.organizerInv = new StorageManagerInventory(playerInv.player, hand);

        this.containerInv = new StoredItemContainerInventory(
                playerInv.player,
                organizerInv,
                managerSlot,
                this.containerSlots,
                this.isClient
        );

        // Visible grid window (scrolls through containerInv)
        int visibleSlots = this.visibleRows * this.cols;
        this.windowInv = new WindowInventory(containerInv, visibleSlots, () -> scrollRow * this.cols, this.isClient);
        for (int i = 0; i < visibleSlots; i++) {
            final int local = i; // capture for inner class

            int col = local % this.cols;
            int row = local / this.cols;

            int sx = this.gridX0 + col * 18;
            int sy = this.gridY0 + row * 18;

            this.addSlot(new Slot(windowInv, local, sx, sy) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    int real = (scrollRow * StorageManagerContainerScreenHandler.this.cols) + local;
                    if (real < 0 || real >= containerInv.size()) return false;
                    return stack.getItem().canBeNested();
                }
            });
        }

        // Player inventory + hotbar
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(playerInv, c + r * 9 + 9, playerX0 + c * 18, playerInvY0 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(playerInv, c, playerX0 + c * 18, hotbarY0));
        }
    }

    public void setScrollRow(int newRow) {
        int max = getMaxScrollRow();
        this.scrollRow = Math.max(0, Math.min(max, newRow));
        sendContentUpdates();
    }

    /** Client-side helper so Slot.canInsert/etc matches the current scroll position immediately. */
    public void setClientScrollRow(int newRow) {
        int max = getMaxScrollRow();
        this.scrollRow = Math.max(0, Math.min(max, newRow));
    }

    @Override
    public void sendContentUpdates() {
        // Keep component/BE-data containers in sync if another screen edits the same item.
        containerInv.refreshFromTarget();
        super.sendContentUpdates();
    }

    public int getMaxScrollRow() { return Math.max(0, totalRows - visibleRows); }
    public int getVisibleRows() { return visibleRows; }
    public int getTotalRows() { return totalRows; }
    public int getCols() { return cols; }

    public boolean usesCustomTexture() { return customTexture; }

    /** Slot-grid top Y used by the scrollbar (border is 1px above slot icon area). */
    public int getGridTopBorderY() { return gridY0 - 1; }

    public Hand getHand() { return hand; }
    public int getManagerSlot() { return managerSlot; }
    public StorageManagerInventory getOrganizerInv() { return organizerInv; }
    public ItemStack getManagerStack() { return managerStack; }

    public LayoutKind getLayoutKind() { return layoutKind; }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getStackInHand(hand).getItem() instanceof StorageManagerItem;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        containerInv.markDirty();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        int containerWindowSlots = this.visibleRows * this.cols;
        int playerInvStart = containerWindowSlots;
        int playerInvEnd = this.slots.size();

        if (slot != null && slot.hasStack()) {
            ItemStack original = slot.getStack();
            newStack = original.copy();

            if (index < containerWindowSlots) {
                // Container -> Player
                if (!this.insertItem(original, playerInvStart, playerInvEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player -> Container
                if (!original.getItem().canBeNested()) {
                    return ItemStack.EMPTY;
                }
                if (!this.insertItem(original, 0, containerWindowSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }

        return newStack;
    }
}
