package org.lccy.lucene.memory.query;

import org.apache.lucene.search.DocIdSetIterator;

import java.io.IOException;
import java.util.List;

/**
 * 基于List构造一个docId迭代器
 *
 * @Date: 2023/11/26 16:15 <br>
 * @author: liuchen11
 */
public class CustomDocIdSetIterator extends DocIdSetIterator {

    private List<Integer> docIds;
    private int idx;

    public CustomDocIdSetIterator(List<Integer> docIds) {
        this.docIds = docIds;
        this.idx = -1;
    }

    @Override
    public int docID() {
        if(idx == -1) {
            return -1;
        } else if (idx >= docIds.size()) {
            return NO_MORE_DOCS;
        } else {
            return docIds.get(idx);
        }
    }

    @Override
    public int nextDoc() throws IOException {
        return advance(idx + 1);
    }

    @Override
    public int advance(int target) throws IOException {
        if (target < 0) {
            idx = 0;
        } else if (target >= docIds.size()) {
            idx = docIds.size();
        } else {
            idx = target;
        }
        if (idx >= docIds.size()) {
            return NO_MORE_DOCS;
        }
        return docIds.get(idx);
    }

    @Override
    public long cost() {
        return docIds.size();
    }
}
