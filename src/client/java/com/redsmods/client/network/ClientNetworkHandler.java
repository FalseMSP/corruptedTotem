package com.redsmods.client.network;

import com.redsmods.client.screen.OsuMenuScreen;
import com.redsmods.network.TotemPopPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
                    // context.client() gives you the Minecraft instance.
                    // This lambda already runs on the NETWORK thread — schedule
                    // anything that touches game state onto the main thread:
                    context.client().execute(() -> onTotemPop(context.client()));
                }
        );
    }

    private static void onTotemPop(net.minecraft.client.Minecraft mc) {
        // TODO: replace with your actual client effect, e.g.:
        //   mc.getSoundManager().play(...)
        //   overlay flash via a HUD render event
        //   particle burst via mc.particleEngine
        //   screen effect via mc.gameRenderer
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new OsuMenuScreen(null));
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§cA soul was taken by the totem...")
            );
        }
    }
}