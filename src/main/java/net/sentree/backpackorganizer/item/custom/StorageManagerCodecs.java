package net.sentree.backpackorganizer.item.custom;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Hand;
import net.sentree.backpackorganizer.util.ModScreenHandlers;

public final class StorageManagerCodecs {
    private StorageManagerCodecs() {}

    private static final Hand[] HANDS = Hand.values();

    private static Hand handFromOrdinal(int ord) {
        if (ord < 0) ord = 0;
        if (ord >= HANDS.length) ord = HANDS.length - 1;
        return HANDS[ord];
    }

    public static final PacketCodec<RegistryByteBuf, ModScreenHandlers.ManagerOpenData> MANAGER_OPEN_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, d -> d.hand().ordinal(),
                    PacketCodecs.VAR_INT, ModScreenHandlers.ManagerOpenData::managerSlots,
                    (handOrd, slots) -> new ModScreenHandlers.ManagerOpenData(handFromOrdinal(handOrd), slots)
            );

    public static final PacketCodec<RegistryByteBuf, ModScreenHandlers.ContainerOpenData> CONTAINER_OPEN_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, d -> d.hand().ordinal(),
                    PacketCodecs.VAR_INT, ModScreenHandlers.ContainerOpenData::slotIndex,
                    PacketCodecs.VAR_INT, ModScreenHandlers.ContainerOpenData::containerSlots,
                    (handOrd, slot, containerSlots) -> new ModScreenHandlers.ContainerOpenData(handFromOrdinal(handOrd), slot, containerSlots)
            );
}
