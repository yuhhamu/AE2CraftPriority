package com.yuuhamu.ae2craftpriority.priority;

public interface PriorityHolder {

    int DEFAULT_PRIORITY = 0;

    int ae2cp$getPriority();

    void ae2cp$setPriority(int priority);

    static int getPriorityOrDefault(Object cluster) {
        if (cluster instanceof PriorityHolder) {
            return ((PriorityHolder) cluster).ae2cp$getPriority();
        }
        return DEFAULT_PRIORITY;
    }

    static void setPriority(Object cluster, int priority) {
        if (cluster instanceof PriorityHolder) {
            ((PriorityHolder) cluster).ae2cp$setPriority(priority);
        }
    }
}
