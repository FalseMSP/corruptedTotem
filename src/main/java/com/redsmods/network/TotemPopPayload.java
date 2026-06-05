package com.redsmods.network;

import com.redsmods.CorruptedTotem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent server → client when the local player pops a totem.
 * The client mod listens for this to trigger its visual/audio effects.
 *
 * NOTE: In 26.1+ use net.minecraft.resources.Identifier (Mojang official name),
 * NOT ResourceLocation (retired Yarn name).
 */
public record TotemPopPayload() implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(CorruptedTotem.MOD_ID, "totem_pop");

    public static final CustomPacketPayload.Type<TotemPopPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    /** No fields — the event itself is the signal, nothing to read/write. */
    public static final StreamCodec<FriendlyByteBuf, TotemPopPayload> CODEC =
            StreamCodec.unit(new TotemPopPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}