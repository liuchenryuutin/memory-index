package org.lccy.lucene.memory.query.nested;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/07 12:14 <br>
 * @author: liuchen11
 */
public class NestedQuery {

    //TODO 查询利用ToParentBlockJoinQuery
    // 写入利用IndexWriter.addDocuments() or IndexWriter.updateDocuments()
    // ES保存实现在DocumentParser.nestedContext, nested文档添加了_id=父文档id
}
