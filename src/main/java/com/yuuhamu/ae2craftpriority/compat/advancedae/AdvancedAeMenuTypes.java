package com.yuuhamu.ae2craftpriority.compat.advancedae;

import net.minecraft.world.inventory.MenuType;
import net.pedroksl.advanced_ae.gui.quantumcomputer.QuantumComputerMenu;

public final class AdvancedAeMenuTypes {

    private static volatile MenuType<QuantumComputerMenu> quantumComputerMenuType;

    private AdvancedAeMenuTypes() {
    }

    public static void capture(MenuType<QuantumComputerMenu> type) {
        if (quantumComputerMenuType == null) {
            quantumComputerMenuType = type;
        }
    }

    public static MenuType<QuantumComputerMenu> quantumComputerMenuTypeOrNull() {
        return quantumComputerMenuType;
    }
}
