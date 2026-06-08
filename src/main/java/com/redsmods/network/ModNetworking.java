package com.redsmods.network;

import com.redsmods.VisualDomain;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

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

        PayloadTypeRegistry.serverboundPlay().register(
                SongEndPayload.TYPE,
                SongEndPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SongEndPayload.TYPE,
                (payload, context) -> {
                    context.server().execute(() -> onFinishSong(context.player()));
                }
        );
    }

    public static void onFinishSong(Player player) {
        VisualDomain.domainMap.get(player).terminate();
    }
}