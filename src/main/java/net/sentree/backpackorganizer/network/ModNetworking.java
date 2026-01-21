package net.sentree.backpackorganizer.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.sentree.backpackorganizer.item.basic.StorageManagerInventory;
import net.sentree.backpackorganizer.item.basic.StorageManagerItem;
import net.sentree.backpackorganizer.item.basic.StorageManagerScreens;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register() {
        // Register payload type (runs on both sides)
        PayloadTypeRegistry.playC2S().register(OpenStorageTabPayload.ID, OpenStorageTabPayload.CODEC);

        // Server receiver
        ServerPlayNetworking.registerGlobalReceiver(OpenStorageTabPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                PlayerEntity player = context.player();
                Hand hand = payload.hand();

                ItemStack held = player.getStackInHand(hand);
                if (!(held.getItem() instanceof StorageManagerItem)) return;

                int tab = payload.tabIndex();

                // tab -1 = manager
                if (tab < 0) {
                    StorageManagerScreens.openManager(player, hand);
                    return;
                }

                // Validate tab against current manager size
                StorageManagerInventory inv = new StorageManagerInventory(player, hand);
                if (tab >= inv.size()) return;

                ItemStack target = inv.getStack(tab);
                if (target.isEmpty()) return;

                if (!StorageManagerScreens.isEditableContainer(target)) return;

                StorageManagerScreens.openContainer(player, hand, tab, target);
            });
        });
    }
}
