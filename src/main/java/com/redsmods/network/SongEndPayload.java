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
public record SongEndPayload(char grade) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(CorruptedTotem.MOD_ID, "song_end");

    public static final CustomPacketPayload.Type<SongEndPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SongEndPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeChar(payload.grade()),
                    buf -> new SongEndPayload(buf.readChar())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
