package net.sentree.backpackorganizer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.sentree.backpackorganizer.Backpackorganizer;

public record OpenStorageTabPayload(int handOrdinal, int tabIndex) implements CustomPayload {
    // tabIndex: -1 = manager tab, 0..(managerSlots-1) = open that slot's container
    public static final Id<OpenStorageTabPayload> ID =
            new Id<>(Identifier.of(Backpackorganizer.MOD_ID, "open_storage_tab"));

    public static final PacketCodec<? super RegistryByteBuf, OpenStorageTabPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, OpenStorageTabPayload::handOrdinal,
                    PacketCodecs.VAR_INT, OpenStorageTabPayload::tabIndex,
                    OpenStorageTabPayload::new
            );

    public static OpenStorageTabPayload of(Hand hand, int tabIndex) {
        // keep tabIndex as-is; server validates against inventory size
        return new OpenStorageTabPayload(hand.ordinal(), tabIndex);
    }

    public Hand hand() {
        Hand[] vals = Hand.values();
        int o = Math.max(0, Math.min(vals.length - 1, handOrdinal));
        return vals[o];
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
