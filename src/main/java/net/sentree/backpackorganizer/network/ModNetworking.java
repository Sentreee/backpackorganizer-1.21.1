package net.sentree.backpackorganizer.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.sentree.backpackorganizer.item.custom.StorageManagerInventory;
import net.sentree.backpackorganizer.item.custom.StorageManagerItem;
import net.sentree.backpackorganizer.item.custom.StorageManagerScreens;
import net.sentree.backpackorganizer.item.custom.StorageManagerContainerScreenHandler;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(OpenStorageTabPayload.ID, OpenStorageTabPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetContainerScrollPayload.ID, SetContainerScrollPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetContainerScrollPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof StorageManagerContainerScreenHandler h) {
                    h.setScrollRow(payload.scrollRow());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(OpenStorageTabPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                PlayerEntity player = context.player();
                Hand hand = payload.hand();

                ItemStack held = player.getStackInHand(hand);

                if (!(held.getItem() instanceof StorageManagerItem)) return;

                StorageManagerInventory inv = new StorageManagerInventory(player, hand);
                int tab = payload.tabIndex();

                if (tab < 0) { StorageManagerScreens.openManager(player, hand); return; }
                if (tab >= inv.size()) return;

                ItemStack target = inv.getStack(tab);
                if (target.isEmpty()) return;
                if (!StorageManagerScreens.isEditableContainer(target)) return;

                StorageManagerScreens.openContainer(player, hand, tab, target);

            });
        });
    }
}
