package org.lccy.lucene.memory.query;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIntMap;
import org.apache.lucene.search.DocIdSetIterator;

import java.io.IOException;
import java.util.Arrays;

/**
 * 基于List构造一个docId迭代器
 *
 * @Date: 2023/11/26 16:15 <br>
 * @author: liuchen11
 */
public class CustomDocIdSetIterator extends DocIdSetIterator {

    private int[] docIds;
    private int length;
    private int idx;

    public CustomDocIdSetIterator(IntArrayList docIdList, IntIntMap docIdIdxMap) {
        this.idx = -1;
        this.docIds = docIdList.toArray();
        this.length = docIds.length;
        Arrays.sort(this.docIds);
    }

    @Override
    public int docID() {
        if (idx == -1) {
            return -1;
        } else if (idx >= length) {
            return NO_MORE_DOCS;
        } else {
            return docIds[idx];
        }
    }

    @Override
    public int nextDoc() throws IOException {
        this.idx = this.idx + 1;
        return docID();
    }

    @Override
    public int advance(int target) throws IOException {
        int doc = docID();
        while (doc < target) {
            doc = nextDoc();
        }
        return doc;
    }

    @Override
    public long cost() {
        return length;
    }
}
