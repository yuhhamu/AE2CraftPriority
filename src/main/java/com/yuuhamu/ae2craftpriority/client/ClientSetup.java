package com.yuuhamu.ae2craftpriority.client;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import appeng.init.client.InitScreens;

public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init(RegisterMenuScreensEvent event) {
        InitScreens.register(event, CraftPriorityStepMenu.TYPE, CraftPriorityStepScreen::new,
                "/screens/ae2craftpriority_priority.json");
    }
}
