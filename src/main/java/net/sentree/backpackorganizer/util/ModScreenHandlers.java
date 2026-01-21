package net.sentree.backpackorganizer.util;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
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

    // Opening data for the container editor screen
    public record ContainerOpenData(Hand hand, int slotIndex, int rows) {}

    private static final Hand[] HANDS = Hand.values();

    // Manager screen opening data is just the Hand
    private static final PacketCodec<? super RegistryByteBuf, Hand> HAND_CODEC =
            PacketCodecs.VAR_INT.xmap(
                    i -> HANDS[Math.max(0, Math.min(HANDS.length - 1, i))],
                    Hand::ordinal
            );

    public static ScreenHandlerType<StorageManagerScreenHandler> STORAGEMANAGER;
    public static ScreenHandlerType<StorageManagerContainerScreenHandler> STORAGEMANAGER_CONTAINER;

    public static void register() {
        STORAGEMANAGER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(Backpackorganizer.MOD_ID, "storagemanager"),
                new ExtendedScreenHandlerType<>(
                        (syncId, playerInv, hand) -> new StorageManagerScreenHandler(syncId, playerInv, hand),
                        HAND_CODEC
                )
        );

        STORAGEMANAGER_CONTAINER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(Backpackorganizer.MOD_ID, "storagemanager_container"),
                new ExtendedScreenHandlerType<>(
                        (syncId, playerInv, data) -> new StorageManagerContainerScreenHandler(
                                syncId, playerInv, data.hand(), data.slotIndex(), data.rows()
                        ),
                        StorageManagerCodecs.CONTAINER_OPEN_CODEC
                )
        );
    }
}
