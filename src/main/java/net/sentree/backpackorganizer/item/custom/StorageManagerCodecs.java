package net.sentree.backpackorganizer.item.custom;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Hand;
import net.sentree.backpackorganizer.util.ModScreenHandlers;

public final class StorageManagerCodecs {
    private StorageManagerCodecs() {}

    private static Hand handFromOrdinal(int ord) {
        Hand[] values = Hand.values();
        if (ord < 0) ord = 0;
        if (ord >= values.length) ord = values.length - 1;
        return values[ord];
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static final PacketCodec<? super RegistryByteBuf, ModScreenHandlers.ContainerOpenData> CONTAINER_OPEN_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, d -> d.hand().ordinal(),
                    PacketCodecs.VAR_INT, ModScreenHandlers.ContainerOpenData::slotIndex,
                    PacketCodecs.VAR_INT, ModScreenHandlers.ContainerOpenData::rows,
                    (handOrd, slot, rows) -> new ModScreenHandlers.ContainerOpenData(
                            handFromOrdinal(handOrd),
                            clamp(slot, 0, 127),     // slot index sanity (real check happens server-side)
                            clamp(rows, 1, 6)        // chest-like rows (1..6)
                    )
            );
}
