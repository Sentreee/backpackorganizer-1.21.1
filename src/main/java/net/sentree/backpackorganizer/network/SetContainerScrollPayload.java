package net.sentree.backpackorganizer.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sentree.backpackorganizer.Backpackorganizer;

public record SetContainerScrollPayload(int scrollRow) implements CustomPayload {
    public static final Id<SetContainerScrollPayload> ID =
            new Id<>(Identifier.of(Backpackorganizer.MOD_ID, "set_container_scroll"));

    public static final PacketCodec<? super RegistryByteBuf, SetContainerScrollPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, SetContainerScrollPayload::scrollRow,
                    SetContainerScrollPayload::new
            );

    public static SetContainerScrollPayload of(int scrollRow) {
        return new SetContainerScrollPayload(scrollRow);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
