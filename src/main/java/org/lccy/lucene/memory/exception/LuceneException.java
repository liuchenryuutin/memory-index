package org.lccy.lucene.memory.exception;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/23 11:32 <br>
 * @author: liuchen11
 */
public class LuceneException extends RuntimeException {

    public LuceneException(String message) {
        super(message);
    }

    public LuceneException(Exception oriEx) {
        super(oriEx);
    }

    public LuceneException(Throwable oriEx) {
        super(oriEx);
    }

    public LuceneException(String message, Exception oriEx) {
        super(message, oriEx);
    }

    public LuceneException(String message, Throwable oriEx) {
        super(message, oriEx);
    }

}
