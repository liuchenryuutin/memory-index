package org.lccy.lucene.memory.aggs.sort;

import org.lccy.lucene.memory.aggs.collector.aggregation.MultiBucketsAggregation;

import java.util.Comparator;

/**
 * 分桶排序类
 *
 * @Date: 2023/12/13 11:16 <br>
 * @author: liuchen11
 */
public class BucketsSort {

    public static final BucketsSort COUNT_DESC = new BucketsSort(comparingCounts().reversed());

    public static final BucketsSort COUNT_ASC = new BucketsSort(comparingCounts());

    public static final BucketsSort SCORE_DESC = new BucketsSort(comparingScore().reversed());

    public static final BucketsSort SCORE_ASC = new BucketsSort(comparingScore());

    public static Comparator<? super MultiBucketsAggregation.Bucket> comparingCounts() {
        return Comparator.comparingLong(MultiBucketsAggregation.Bucket::getDocCount);
    }

    public static Comparator<? super MultiBucketsAggregation.Bucket> comparingScore() {
        return Comparator.comparingDouble(MultiBucketsAggregation.Bucket::getMaxScore);
    }

    private Comparator<? super MultiBucketsAggregation.Bucket> comparator;

    public BucketsSort(Comparator<? super MultiBucketsAggregation.Bucket> comparator) {
        this.comparator = comparator;
    }

    public Comparator<? super MultiBucketsAggregation.Bucket> getComparator() {
        return this.comparator;
    }

}
