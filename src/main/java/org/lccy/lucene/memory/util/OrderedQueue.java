package org.lccy.lucene.memory.util;

import org.apache.lucene.util.PriorityQueue;

import java.util.Comparator;

/**
 * 一个由小到大的队列，插入时自动调整元素。
 *
 * @Date: 2023/12/12 14:14 <br>
 * @author: liuchen11
 */
public class OrderedQueue<B extends Object> extends PriorityQueue<B> {

    private final Comparator<? super B> comparator;

    public OrderedQueue(int size, Comparator<? super B> comparator) {
        super(size);
        this.comparator = comparator;
    }

    @Override
    protected boolean lessThan(B a, B b) {
        return comparator.compare(a, b) < 0;
    }
}
