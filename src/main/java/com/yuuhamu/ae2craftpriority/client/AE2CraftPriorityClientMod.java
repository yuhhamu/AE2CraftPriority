package com.yuuhamu.ae2craftpriority.client;

import net.fabricmc.api.ClientModInitializer;

public final class AE2CraftPriorityClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientSetup.init();
    }
}
