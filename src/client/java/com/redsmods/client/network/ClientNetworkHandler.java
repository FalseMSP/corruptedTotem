package com.redsmods.client.network;

import com.redsmods.client.screen.OsuMenuScreen;
import com.redsmods.network.SongEndPayload;
import com.redsmods.network.TotemPopPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;

/**
 * Registered from your ClientModInitializer.
 * Handles the TotemPopPayload sent by the server and triggers
 * whatever client-side effect you want (screen flash, sound, etc.).
 */
public class ClientNetworkHandler {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                TotemPopPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> onTotemPop(context.client()));
                }
        );
    }

    private static void onTotemPop(net.minecraft.client.Minecraft mc) {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new OsuMenuScreen(null));
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§cA soul was taken by the totem...")
            );
        }
    }

    public static void sendSongEnd(char grade) {
        if (ClientPlayNetworking.canSend(SongEndPayload.ID)) {
            ClientPlayNetworking.send(new SongEndPayload(grade));
        }
    }
}