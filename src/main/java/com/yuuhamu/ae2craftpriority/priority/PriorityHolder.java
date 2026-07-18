package com.yuuhamu.ae2craftpriority.priority;

public interface PriorityHolder {

    int DEFAULT_PRIORITY = 0;

    int ae2cp$getPriority();

    void ae2cp$setPriority(int priority);

    static int getPriorityOrDefault(Object cluster) {
        if (cluster instanceof PriorityHolder holder) {
            return holder.ae2cp$getPriority();
        }
        return DEFAULT_PRIORITY;
    }

    static void setPriority(Object cluster, int priority) {
        if (cluster instanceof PriorityHolder holder) {
            holder.ae2cp$setPriority(priority);
        }
    }
}
