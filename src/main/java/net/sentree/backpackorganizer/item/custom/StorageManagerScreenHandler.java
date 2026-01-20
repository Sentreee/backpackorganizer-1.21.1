package net.sentree.backpackorganizer.item.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.sentree.backpackorganizer.util.ModScreenHandlers;

public class StorageManagerScreenHandler extends ScreenHandler {
    private static final int ORGANIZER_SLOTS = StorageManagerInventory.SIZE; // should be 3

    private final Hand hand;
    private final StorageManagerInventory organizerInv;

    // Vanilla chest layout constants (176x166 GUI)
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y     = 142;

    // Where the top “container” row would be in a chest GUI
    private static final int CONTAINER_ROW_Y = 18;

    public StorageManagerScreenHandler(int syncId, PlayerInventory playerInv, Hand hand) {
        super(ModScreenHandlers.STORAGEMANAGER, syncId);

        this.hand = hand;
        this.organizerInv = new StorageManagerInventory(playerInv.player, hand);

        // --- Top inventory (your 3 slots) ---
        // Make it look like a vanilla chest row:
        // Place them on the same Y as chest row (18) and center horizontally.
        // If you ever change ORGANIZER_SLOTS, this will still center properly.
        int totalWidth = ORGANIZER_SLOTS * 18;
        int startX = (176 - totalWidth) / 2; // 176 is vanilla GUI width

        for (int i = 0; i < ORGANIZER_SLOTS; i++) {
            this.addSlot(new Slot(organizerInv, i, startX + i * 18, CONTAINER_ROW_Y) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return StorageManagerItem.isAllowedStoredItem(stack);
                }
            });
        }

        // --- Player inventory (3 rows, vanilla coords) ---
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                int x = PLAYER_INV_X + col * 18;
                int y = PLAYER_INV_Y + row * 18;
                this.addSlot(new Slot(playerInv, index, x, y));
            }
        }

        // --- Hotbar (vanilla coords) ---
        for (int col = 0; col < 9; col++) {
            int x = PLAYER_INV_X + col * 18;
            this.addSlot(new Slot(playerInv, col, x, HOTBAR_Y));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // Keep your existing rule if you have one; this is the common “still holding item” check:
        return player.getStackInHand(hand).getItem() instanceof StorageManagerItem;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        ItemStack inSlot = slot.getStack();
        ItemStack copy = inSlot.copy();

        if (index < ORGANIZER_SLOTS) {
            // storage -> player inventory
            if (!this.insertItem(inSlot, ORGANIZER_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // player -> storage (only allowed items)
            if (!StorageManagerItem.isAllowedStoredItem(inSlot)) return ItemStack.EMPTY;
            if (!this.insertItem(inSlot, 0, ORGANIZER_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (inSlot.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        return copy;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        organizerInv.markDirty();
    }
}
