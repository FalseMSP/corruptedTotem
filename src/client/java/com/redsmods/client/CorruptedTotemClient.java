package com.redsmods.client;

import com.redsmods.client.network.ClientNetworkHandler;
import net.fabricmc.api.ClientModInitializer;

public class CorruptedTotemClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientNetworkHandler.register();
	}
}