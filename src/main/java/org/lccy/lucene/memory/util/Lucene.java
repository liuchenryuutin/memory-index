package org.lccy.lucene.memory.util;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.ScorerSupplier;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.util.Bits;

import java.io.IOException;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/11 17:10 <br>
 * @author: liuchen11
 */
public class Lucene {


    /**
     * Return a {@link Bits} view of the provided scorer.
     * <b>NOTE</b>: that the returned {@link Bits} instance MUST be consumed in order.
     * @see #asSequentialAccessBits(int, ScorerSupplier, long)
     */
    public static Bits asSequentialAccessBits(final int maxDoc, ScorerSupplier scorerSupplier) throws IOException {
        return asSequentialAccessBits(maxDoc, scorerSupplier, 0L);
    }

    /**
     * Given a {@link ScorerSupplier}, return a {@link Bits} instance that will match
     * all documents contained in the set.
     * <b>NOTE</b>: that the returned {@link Bits} instance MUST be consumed in order.
     * @param estimatedGetCount an estimation of the number of times that {@link Bits#get} will get called
     */
    public static Bits asSequentialAccessBits(final int maxDoc, ScorerSupplier scorerSupplier,
                                              long estimatedGetCount) throws IOException {
        if (scorerSupplier == null) {
            return new Bits.MatchNoBits(maxDoc);
        }
        // Since we want bits, we need random-access
        final Scorer scorer = scorerSupplier.get(estimatedGetCount); // this never returns null
        final TwoPhaseIterator twoPhase = scorer.twoPhaseIterator();
        final DocIdSetIterator iterator;
        if (twoPhase == null) {
            iterator = scorer.iterator();
        } else {
            iterator = twoPhase.approximation();
        }

        return new Bits() {

            int previous = -1;
            boolean previousMatched = false;

            @Override
            public boolean get(int index) {
                if (index < 0 || index >= maxDoc) {
                    throw new IndexOutOfBoundsException(index + " is out of bounds: [" + 0 + "-" + maxDoc + "[");
                }
                if (index < previous) {
                    throw new IllegalArgumentException("This Bits instance can only be consumed in order. "
                            + "Got called on [" + index + "] while previously called on [" + previous + "]");
                }
                if (index == previous) {
                    // we cache whether it matched because it is illegal to call
                    // twoPhase.matches() twice
                    return previousMatched;
                }
                previous = index;

                int doc = iterator.docID();
                if (doc < index) {
                    try {
                        doc = iterator.advance(index);
                    } catch (IOException e) {
                        throw new IllegalStateException("Cannot advance iterator", e);
                    }
                }
                if (index == doc) {
                    try {
                        return previousMatched = twoPhase == null || twoPhase.matches();
                    } catch (IOException e) {
                        throw new IllegalStateException("Cannot validate match", e);
                    }
                }
                return previousMatched = false;
            }

            @Override
            public int length() {
                return maxDoc;
            }
        };
    }
}
