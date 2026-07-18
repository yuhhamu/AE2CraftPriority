package com.yuuhamu.ae2craftpriority.priority;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.inventory.MenuType;

import appeng.menu.locator.MenuLocator;

public final class PriorityReturnTarget {

    public record Target(MenuType<?> menuType, MenuLocator locator) {
    }

    private static final Map<UUID, Target> TARGETS = new ConcurrentHashMap<>();

    private PriorityReturnTarget() {
    }

    public static void set(UUID playerId, MenuType<?> menuType, MenuLocator locator) {
        TARGETS.put(playerId, new Target(menuType, locator));
    }

    public static Target take(UUID playerId) {
        return TARGETS.remove(playerId);
    }

    public static void clear(UUID playerId) {
        TARGETS.remove(playerId);
    }
}
