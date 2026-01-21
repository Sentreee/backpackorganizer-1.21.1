package net.sentree.backpackorganizer.item.basic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.sentree.backpackorganizer.util.ModScreenHandlers;

public class StorageManagerContainerScreenHandler extends ScreenHandler {
    private final Hand hand;
    private final int managerSlot;
    private final int rows;

    private final StorageManagerInventory managerInv;
    private final StoredItemContainerInventory containerInv;

    public StorageManagerContainerScreenHandler(int syncId, PlayerInventory playerInv, Hand hand, int managerSlot, int rows) {
        super(ModScreenHandlers.STORAGEMANAGER_CONTAINER, syncId);
        this.hand = hand;
        this.managerSlot = managerSlot;
        this.rows = rows;

        this.managerInv = new StorageManagerInventory(playerInv.player, hand);
        this.containerInv = new StoredItemContainerInventory(
                playerInv.player,
                managerInv,
                managerSlot,
                rows,
                playerInv.player.getWorld().isClient()
        );

        // --- Container slots (vanilla chest layout) ---
        int slotIndex = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(containerInv, slotIndex++, 8 + c * 18, 18 + r * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        // Vanilla-like "no nesting" for portable containers
                        return stack.getItem().canBeNested();
                    }
                });
            }
        }

        // --- Player inventory ---
        int invY = 18 + rows * 18 + 14;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, invY + r * 18));
            }
        }

        // --- Hotbar ---
        int hotbarY = invY + 58;
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(playerInv, c, 8 + c * 18, hotbarY));
        }
    }

    // ---- Getters used by the client screen tabs ----
    public Hand getHand() { return hand; }
    public int getManagerSlot() { return managerSlot; }
    public int getRows() { return rows; }
    public StorageManagerInventory getManagerInv() { return managerInv; }

    @Override
    public boolean canUse(PlayerEntity player) {
        // Allow all tiers (basic/copper/etc)
        return player.getStackInHand(hand).getItem() instanceof StorageManagerItem;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        containerInv.markDirty(); // final save
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        int containerSize = rows * 9;

        if (slot != null && slot.hasStack()) {
            ItemStack original = slot.getStack();
            newStack = original.copy();

            if (index < containerSize) {
                // container -> player
                if (!this.insertItem(original, containerSize, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // player -> container (respect canBeNested)
                if (!original.getItem().canBeNested()) return ItemStack.EMPTY;
                if (!this.insertItem(original, 0, containerSize, false)) return ItemStack.EMPTY;
            }

            if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }

        return newStack;
    }
}
