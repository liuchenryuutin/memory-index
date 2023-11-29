package org.lccy.lucene.memory.search;

import java.io.Serializable;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/04/23 16:13 <br>
 * @author: liuchen11
 */
public class PageArg implements Serializable {
    private static final long serialVersionUID = 1L;
    private int pageNum = 1;
    private int pageSize = 10;

    public PageArg() {
    }

    public PageArg(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public int getPageNum() {
        return this.pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
