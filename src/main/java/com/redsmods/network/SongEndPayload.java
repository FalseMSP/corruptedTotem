package com.redsmods.network;

import com.redsmods.CorruptedTotem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent client → server when the local player finishes a song.
 * The server mod listens for this to trigger its visual/audio effects.
 */
public record SongEndPayload() implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(CorruptedTotem.MOD_ID, "sond_end");

    public static final CustomPacketPayload.Type<com.redsmods.network.SongEndPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    /** No fields — the event itself is the signal, nothing to read/write. */
    public static final StreamCodec<FriendlyByteBuf, com.redsmods.network.SongEndPayload> CODEC =
            StreamCodec.unit(new com.redsmods.network.SongEndPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
