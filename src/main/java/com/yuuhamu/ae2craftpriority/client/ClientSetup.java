package com.yuuhamu.ae2craftpriority.client;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;

import appeng.init.client.InitScreens;

public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init() {
        InitScreens.register(CraftPriorityStepMenu.TYPE, CraftPriorityStepScreen::new, "/screens/priority.json");
    }
}
