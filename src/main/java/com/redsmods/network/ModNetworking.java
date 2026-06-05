package com.redsmods.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers all custom packet payloads.
 * Call ModNetworking.register() from your main ModInitializer.
 */
public class ModNetworking {

    public static void register() {
        // Register the payload type for play-phase S2C traffic.
        // This must be called on BOTH sides (server + client) before any
        // packet can be sent or received — putting it in the common
        // initializer satisfies that requirement automatically.
        PayloadTypeRegistry.clientboundPlay().register(
                TotemPopPayload.TYPE,
                TotemPopPayload.CODEC
        );
    }
}