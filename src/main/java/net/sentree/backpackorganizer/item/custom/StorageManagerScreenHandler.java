package net.sentree.backpackorganizer.item.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.sentree.backpackorganizer.util.ModScreenHandlers;

public class StorageManagerScreenHandler extends ScreenHandler {
    private final Hand hand;
    private final StorageManagerInventory organizerInv;
    private final ItemStack managerStack;
    private final int managerSlots;

    /**
     * ✅ NEW: use this constructor for BOTH server+client.
     * The slots value MUST come from opening data (sent by server),
     * so the client never "guesses" size from its local held stack.
     */
    public StorageManagerScreenHandler(int syncId, PlayerInventory playerInv, Hand hand, int managerSlots) {
        super(ModScreenHandlers.STORAGEMANAGER, syncId);
        this.hand = hand;

        this.managerStack = playerInv.player.getStackInHand(hand);
        this.managerSlots = Math.max(1, managerSlots);

        // ✅ IMPORTANT: inventory size is forced from network-provided slot count
        this.organizerInv = new StorageManagerInventory(playerInv.player, hand, this.managerSlots);

        // --- Organizer slots (match your custom textures) ---
        addOrganizerSlots(this.managerSlots);

        // --- Player inventory + hotbar (standard 166px-tall GUI positions) ---
        int invY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }

        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
        }
    }

    /**
     * OPTIONAL: keep this ONLY if you still have old code calling (syncId, inv, hand).
     * It’s safe on the SERVER, but you should update your openHandledScreen factory
     * to pass slots so everything is consistent.
     */
    @Deprecated
    public StorageManagerScreenHandler(int syncId, PlayerInventory playerInv, Hand hand) {
        this(syncId, playerInv, hand, computeSlotsFromHeld(playerInv.player.getStackInHand(hand)));
    }

    private static int computeSlotsFromHeld(ItemStack held) {
        if (held.getItem() instanceof StorageManagerItem sm) return sm.getSlots();
        return 3;
    }

    private void addOrganizerSlots(int slots) {
        int y = 20;

        if (slots == 3) {
            int[] xs = {62, 80, 98};
            for (int i = 0; i < 3; i++) {
                final int slot = i;
                this.addSlot(new Slot(organizerInv, slot, xs[i], y) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return StorageManagerItem.isAllowedStoredItem(stack);
                    }
                });
            }
            return;
        }

        if (slots == 5) {
            int[] xs = {44, 62, 80, 98, 116};
            for (int i = 0; i < 5; i++) {
                final int slot = i;
                this.addSlot(new Slot(organizerInv, slot, xs[i], y) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return StorageManagerItem.isAllowedStoredItem(stack);
                    }
                });
            }
            return;
        }

        if (slots == 7) {
            int[] xs = {26, 44, 62, 80, 98, 116, 134};
            for (int i = 0; i < 7; i++) {
                final int slot = i;
                this.addSlot(new Slot(organizerInv, slot, xs[i], y) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return StorageManagerItem.isAllowedStoredItem(stack);
                    }
                });
            }
            return;
        }

        if (slots == 9) {
            int[] xs = {8, 26, 44, 62, 80, 98, 116, 134, 152};
            for (int i = 0; i < 9; i++) {
                final int slot = i;
                this.addSlot(new Slot(organizerInv, slot, xs[i], y) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return StorageManagerItem.isAllowedStoredItem(stack);
                    }
                });
            }
            return;
        }

        // Fallback: lay out in a row starting at x=8
        for (int i = 0; i < slots; i++) {
            int x = 8 + (i * 18);
            this.addSlot(new Slot(organizerInv, i, x, y) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return StorageManagerItem.isAllowedStoredItem(stack);
                }
            });
        }
    }

    // ---- Getters for client screens (tabs + texture selection) ----
    public Hand getHand() { return hand; }
    public StorageManagerInventory getOrganizerInv() { return organizerInv; }
    public ItemStack getManagerStack() { return managerStack; }
    public int getManagerSlots() { return managerSlots; }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getStackInHand(hand).getItem() instanceof StorageManagerItem;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        int managerSlots = this.managerSlots;
        int playerInvStart = managerSlots;
        int playerInvEnd = this.slots.size();

        if (slot != null && slot.hasStack()) {
            ItemStack original = slot.getStack();
            newStack = original.copy();

            if (index < managerSlots) {
                // Manager -> Player
                if (!this.insertItem(original, playerInvStart, playerInvEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player -> Manager
                if (!StorageManagerItem.isAllowedStoredItem(original)) {
                    return ItemStack.EMPTY;
                }
                if (!this.insertItem(original, 0, managerSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }

        return newStack;
    }
}
