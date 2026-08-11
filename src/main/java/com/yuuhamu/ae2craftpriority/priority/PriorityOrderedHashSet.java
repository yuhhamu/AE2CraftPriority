package com.yuuhamu.ae2craftpriority.priority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class PriorityOrderedHashSet<E> extends HashSet<E> {

    private final Comparator<? super E> comparator;

    public PriorityOrderedHashSet(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    @Override
    public Iterator<E> iterator() {
        List<E> sorted = new ArrayList<>(size());
        Iterator<E> raw = super.iterator();
        while (raw.hasNext()) {
            sorted.add(raw.next());
        }
        sorted.sort(comparator);
        return sorted.iterator();
    }
}
