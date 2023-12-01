# memory-index

基于lucene的内存索引，支持索引创建、导入、新增、修改、删除、查询、计数等操作，支持自定义查询、自定义分词器注册

## 索引配置文件
memory_index_setting.json文件内容：
```json
{
  "setting": {
    "refreshInterval": 5000
  },
  "mapping": [
    {
      "name": "dataNo",
      "type": "keyword",
      "primary": true
    },
    {
      "name": "itemId",
      "type": "keyword"
    },
    {
      "name": "title",
      "type": "text",
      "analyzer": "ik_max_word"
    },
    {
      "name": "titleFullPy",
      "type": "text",
      "analyzer": "ik_smart_full_word"
    },
    {
      "name": "titleSimPy",
      "type": "text",
      "analyzer": "ik_smart_simple_word"
    },
    {
      "name": "titleStandardPy",
      "type": "text",
      "analyzer": "standard_full_word"
    },
    {
      "name": "titleSuggestFullPy",
      "type": "text",
      "analyzer": "edge_ngram_full_word"
    },
    {
      "name": "titleSuggestSimPy",
      "type": "text",
      "analyzer": "edge_ngram_simple_word",
      "searchAnalyzer": "standard_full_word"
    },
    {
      "name": "atomTag",
      "type": "text",
      "analyzer": "douhao_punct"
    },
    {
      "name": "atomTagFullPy",
      "type": "text",
      "analyzer": "douhao_full_word"
    },
    {
      "name": "atomTagSimPy",
      "type": "text",
      "analyzer": "douhao_simple_word"
    },
    {
      "name": "startTime",
      "type": "date",
      "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd"
    },
    {
      "name": "shieldInfo",
      "type": "json"
    },
    {
      "name": "location",
      "type": "geo_point"
    },
    {
      "name": "exposure",
      "type": "long"
    },
    {
      "name": "systemId",
      "type": "keyword",
      "docValue": false,
      "store": false
    },
    {
      "name": "click",
      "type": "float"
    },
    {
      "name": "dataType",
      "type": "text",
      "analyzer": "douhao"
    }
  ]
}
```


## 普通分页查询
```java
public class DemoTest {
    
    @Test
    public void search() throws LuceneException, InterruptedException {
        // 索引配置文件
        IndexConfig indexConfig = new ClassPathIndexConfig("memory_index_setting.json");
        // 索引数据源
        IndexDataLoader dataLoader = new ClassPathIndexDataLoader("memory_index_data.json");
        // 构建索引
        MemoryIndex memoryIndex = new MemoryIndex(indexConfig, dataLoader);

        // 分页
        PageArg pageArg = new PageArg(1, 10);
        // 查询条件
        List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(new SearchCriteria("title", "测试", new SearchOption(SearchOption.SearchLogic.filter, SearchOption.SearchType.match)));
        criteriaList.add(new SearchCriteria("exposure", null, new SearchOption(SearchOption.SearchLogic.filter, SearchOption.SearchType.exists)));
        criteriaList.add(new SearchCriteria("titleFullPy", null, new SearchOption(SearchOption.SearchLogic.filter, SearchOption.SearchType.exists)));
        criteriaList.add(new SearchCriteria("location", null, new SearchOption(SearchOption.SearchLogic.filter, SearchOption.SearchType.exists)));
        SearchRequest request = new SearchRequest(pageArg, criteriaList);
        // 排序
        List<SortFieldInfo> sorts = new ArrayList<>();
        sorts.add(new SortFieldInfo("exposure", null, 100l, SortFieldInfo.SortMode.ASC));
        sorts.add(SortFieldInfo._SCORE);
        request.setSorts(sorts);
        // 查询结果
        SearchResponse response = memoryIndex.search(request);
        System.out.println("结果:" + JSON.toJSONString(response));
    }
}
```

## 插入/更新
```java
public class DemoTest {

    @Test
    public void insert() throws LuceneException, InterruptedException {
        IndexConfig indexConfig = new ClassPathIndexConfig("memory_index_setting.json");
        IndexDataLoader dataLoader = new ClassPathIndexDataLoader("memory_index_data.json");
        MemoryIndex memoryIndex = new MemoryIndex(indexConfig, dataLoader);
        List<Map<String, Object>> insertList = new ArrayList<>();
        Map<String, Object> insert2 = new HashMap<>();
        insert2.put("dataNo", "1");
        insert2.put("itemId", "TEST111");
        insertList.add(insert2);
        Map<String, Object> insert3 = new HashMap<>();
        insert3.put("dataNo", "2");
        insert3.put("itemId", "TEST111");
        insertList.add(insert3);
        Map<String, Object> insert4 = new HashMap<>();
        insert4.put("dataNo", "3");
        insert4.put("itemId", "TEST111");
        insertList.add(insert4);
        Map<String, Object> insert5 = new HashMap<>();
        insert5.put("dataNo", "4");
        insert5.put("itemId", "TEST111");
        insertList.add(insert5);
        memoryIndex.batchInsertUpdate(insertList);

        // 休眠5秒，因为设置的索引刷新时间是5秒
        Thread.sleep(5001);

        List<SearchCriteria> criteriaList2 = new ArrayList<>();
        criteriaList2.add(new SearchCriteria("itemId", "TEST111", new SearchOption(SearchOption.SearchLogic.filter, SearchOption.SearchType.term)));
        SearchRequest request2 = new SearchRequest(pageArg, criteriaList2);
        SearchResponse response3 = memoryIndex.search(request2);
        System.out.println("更新后:" + JSON.toJSONString(response3));
    }
}
```

## 删除
```java
public class DemoTest {
    
    @Test
    public void delete() throws LuceneException, InterruptedException {
        // 索引配置文件
        IndexConfig indexConfig = new ClassPathIndexConfig("memory_index_setting.json");
        // 索引数据源(每行一条数据，格式为json)
        IndexDataLoader dataLoader = new ClassPathIndexDataLoader("memory_index_data.json");
        // 构建索引
        MemoryIndex memoryIndex = new MemoryIndex(indexConfig, dataLoader);
        
        String dataNo = "test1";
        // 删除
        memoryIndex.delete(dataNo);
    }
}
```