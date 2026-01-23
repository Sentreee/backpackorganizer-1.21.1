package net.sentree.backpackorganizer.util;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.sentree.backpackorganizer.Backpackorganizer;
import net.sentree.backpackorganizer.item.custom.StorageManagerCodecs;
import net.sentree.backpackorganizer.item.custom.StorageManagerContainerScreenHandler;
import net.sentree.backpackorganizer.item.custom.StorageManagerScreenHandler;

public final class ModScreenHandlers {
    private ModScreenHandlers() {}

    // ✅ Manager open data MUST include slots so client/server match
    public record ManagerOpenData(Hand hand, int managerSlots) {}

    // Container open data includes containerSlots so client can pick the correct GUI/layout
    public record ContainerOpenData(Hand hand, int slotIndex, int containerSlots) {}

    public static ScreenHandlerType<StorageManagerScreenHandler> STORAGEMANAGER;
    public static ScreenHandlerType<StorageManagerContainerScreenHandler> STORAGEMANAGER_CONTAINER;

    public static void register() {
        STORAGEMANAGER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(Backpackorganizer.MOD_ID, "storagemanager"),
                new ExtendedScreenHandlerType<>(
                        (syncId, playerInv, data) ->
                                new StorageManagerScreenHandler(syncId, playerInv, data.hand(), data.managerSlots()),
                        StorageManagerCodecs.MANAGER_OPEN_CODEC
                )
        );

        STORAGEMANAGER_CONTAINER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(Backpackorganizer.MOD_ID, "storagemanager_container"),
                new ExtendedScreenHandlerType<>(
                        (syncId, playerInv, data) ->
                                new StorageManagerContainerScreenHandler(syncId, playerInv, data.hand(), data.slotIndex(), data.containerSlots()),
                        StorageManagerCodecs.CONTAINER_OPEN_CODEC
                )
        );
    }
}
