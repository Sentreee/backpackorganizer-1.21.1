package net.sentree.backpackorganizer.util;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.sentree.backpackorganizer.Backpackorganizer;
import net.sentree.backpackorganizer.item.custom.StorageManagerScreenHandler;

public final class ModScreenHandlers {
    // Encode the opening hand (MAIN_HAND / OFF_HAND)
    private static final PacketCodec<ByteBuf, Hand> HAND_CODEC =
            PacketCodecs.VAR_INT.xmap(i -> Hand.values()[i], Hand::ordinal);

    public static ScreenHandlerType<StorageManagerScreenHandler> STORAGEMANAGER;

    public static void register() {
        STORAGEMANAGER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(Backpackorganizer.MOD_ID, "storagemanager"),
                new ExtendedScreenHandlerType<>(StorageManagerScreenHandler::new, HAND_CODEC)
        );
    }

    private ModScreenHandlers() {}
}
