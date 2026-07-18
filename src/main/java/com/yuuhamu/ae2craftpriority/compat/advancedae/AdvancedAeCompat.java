package com.yuuhamu.ae2craftpriority.compat.advancedae;

import com.yuuhamu.ae2craftpriority.api.CraftPriorityApi;

public final class AdvancedAeCompat {

    public static final String MOD_ID = "advanced_ae";

    private AdvancedAeCompat() {
    }

    public static void init() {
        CraftPriorityApi.registerAdapter(new AdvancedAeCpuAdapter());
    }
}
