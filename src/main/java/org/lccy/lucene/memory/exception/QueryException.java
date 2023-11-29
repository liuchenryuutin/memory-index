package org.lccy.lucene.memory.exception;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/23 11:33 <br>
 * @author: liuchen11
 */
public class QueryException extends LuceneException {

    public QueryException(String message) {
        super(message);
    }

    public QueryException(Exception oriEx) {
        super(oriEx);
    }

    public QueryException(Throwable oriEx) {
        super(oriEx);
    }

    public QueryException(String message, Exception oriEx) {
        super(message, oriEx);
    }

    public QueryException(String message, Throwable oriEx) {
        super(message, oriEx);
    }
}
