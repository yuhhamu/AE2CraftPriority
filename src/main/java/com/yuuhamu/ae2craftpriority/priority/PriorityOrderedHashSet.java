package com.yuuhamu.ae2craftpriority.priority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * 反復順序を優先度降順に固定するHashSetの派生クラス。
 *
 * <p>{@code CraftingGridCacheMixin}が{@code craftingCPUClusters}フィールドの実体をこのクラスの
 * インスタンスに差し替えることで、AE2本体のロジック({@code updateCPUClusters()}が
 * {@code clear()}/{@code add()}するだけで、フィールド自体の再代入は行わない)を変更せずに
 * 反復順序だけを変えることができる。</p>
 */
public class PriorityOrderedHashSet<E> extends HashSet<E> {

    private final Comparator<? super E> comparator;

    public PriorityOrderedHashSet(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    @Override
    public Iterator<E> iterator() {
        List<E> sorted = new ArrayList<E>(size());
        Iterator<E> raw = super.iterator();
        while (raw.hasNext()) {
            sorted.add(raw.next());
        }
        sorted.sort(comparator);
        return sorted.iterator();
    }
}
