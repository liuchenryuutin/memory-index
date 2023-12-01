package org.lccy.lucene.memory.query;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIntMap;
import org.apache.lucene.search.DocIdSetIterator;

import java.io.IOException;

/**
 * 基于List构造一个docId迭代器
 *
 * @Date: 2023/11/26 16:15 <br>
 * @author: liuchen11
 */
public class CustomDocIdSetIterator extends DocIdSetIterator {

    private IntArrayList docIds;
    private IntIntMap docIdIdxMap;
    private int idx;

    public CustomDocIdSetIterator(IntArrayList docIds, IntIntMap docIdIdxMap) {
        this.idx = -1;
        this.docIds = docIds;
        this.docIdIdxMap = docIdIdxMap;
    }

    @Override
    public int docID() {
        if (idx == -1) {
            return -1;
        } else if (idx >= docIds.size()) {
            return NO_MORE_DOCS;
        } else {
            return docIds.get(idx);
        }
    }

    @Override
    public int nextDoc() throws IOException {
        this.idx = this.idx + 1;
        return docID();
    }

    @Override
    public int advance(int target) throws IOException {
        this.idx = docIdIdxMap.getOrDefault(target, 0);
        return docID();
    }

    @Override
    public long cost() {
        return docIds.size();
    }
}
