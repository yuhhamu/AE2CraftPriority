package com.yuuhamu.ae2craftpriority.priority;

public final class PendingCraftPriority {

    private static final ThreadLocal<Integer> VALUE = new ThreadLocal<Integer>();

    private PendingCraftPriority() {
    }

    public static void set(int priority) {
        VALUE.set(Integer.valueOf(priority));
    }

    public static Integer get() {
        return VALUE.get();
    }

    public static void clear() {
        VALUE.remove();
    }
}
