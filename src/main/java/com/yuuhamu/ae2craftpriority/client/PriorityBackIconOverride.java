package com.yuuhamu.ae2craftpriority.client;

public final class PriorityBackIconOverride {

    private static boolean fromCraftingStatus = false;

    private PriorityBackIconOverride() {
    }

    public static void set() {
        fromCraftingStatus = true;
    }

    /** フラグを取り出してクリアする。 */
    public static boolean take() {
        var result = fromCraftingStatus;
        fromCraftingStatus = false;
        return result;
    }

    public static void clear() {
        fromCraftingStatus = false;
    }
}
