package org.lccy.lucene.memory.aggs.collector.aggregation;

/**
 * 分组结果父类（内部用）
 *
 * @Date: 2023/12/11 19:14 <br>
 * @author: liuchen11
 */
public abstract class InternalAggregation implements Aggregation {

    private String name;

    private Float maxScore;

    public InternalAggregation(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * 获取当前分组的最大评分，子类实现
     * @return
     */
    protected abstract float doGetMaxScore();

    /**
     * 获取当前分组的最大评分（懒加载并缓存），如果没有评分返回Float.NaN
     * @return
     */
    public float getMaxScore() {
        if(maxScore == null) {
            maxScore = doGetMaxScore();
        }
        return maxScore;
    }
}
