package org.lccy.lucene.memory.search;

import org.lccy.lucene.memory.exception.QueryException;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.util.StringUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.util.List;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/22 17:08 <br>
 * @author: liuchen11
 */
@Getter
@Setter
public class SearchRequest {

    private PageArg pageArg;
    private List<SearchCriteria> criteriaList;

    public SearchRequest() {
    }


    public SearchRequest(PageArg pageArg) {
        this();
        this.pageArg = pageArg;
    }

    public SearchRequest(PageArg pageArg, List<SearchCriteria> criteriaList) {
        this(pageArg);
        this.criteriaList = criteriaList;
    }

    /**
     * 构建查询条件
     *
     * @param criteriaList
     * @param indexConfig
     * @return
     */
    public Query createQuery(List<SearchCriteria> criteriaList, IndexConfig indexConfig) throws QueryException {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return null;
        }
        BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
        boolean onlyOneCriteria = criteriaList.size() == 1 ? true : false;
        Query rootQuery = null;

        for (SearchCriteria criteria : criteriaList) {
            SearchOption searchOption = criteria.getOption();
            if (searchOption == null) {
                throw new QueryException("search criteria must has option, please check.");
            }
            if (searchOption.getSearchType() == null) {
                throw new QueryException("search criteria must has search type, please check.");
            }
            SearchOption.SearchType searchType = searchOption.getSearchType();
            if (StringUtil.isEmpty(criteria.getField()) && searchType.isMustField()) {
                throw new QueryException("search criteria must has field, please check.");
            }
            SearchOption.SearchLogic searchLogic = searchOption.getSearchLogic();
            Query query;
            if (criteria.hasSubCriterias()) {
                Query childQuery = createQuery(criteria.getSubCriterias(), indexConfig);
                if (searchType == SearchOption.SearchType.bool) {
                    query = childQuery;
                } else {
                    throw new QueryException("not support child query, except bool.");
                }
            } else if (searchType == SearchOption.SearchType.bool) {
                throw new QueryException("bool must has child query");
            } else {
                query = buildCriteria(criteria, indexConfig);
            }
            if (onlyOneCriteria) {
                rootQuery = query;
            } else {
                searchLogic.convert(boolQueryBuilder, query);
            }
        }
        if (!onlyOneCriteria) {
            rootQuery = boolQueryBuilder.build();
        }

        return rootQuery;
    }

    public Query buildCriteria(SearchCriteria criteria, IndexConfig indexConfig) throws QueryException {
        SearchOption.SearchType searchType = criteria.getOption().getSearchType();
        Query result;
        if (searchType == SearchOption.SearchType.custom) {
            if (criteria.getCustomQuery() != null) {
                result = criteria.getCustomQuery();
            } else {
                throw new QueryException("custom query must has customBuilder");
            }
        } else {
            if (searchType.isMustValue() && (criteria.getValues() == null || criteria.getValues().isEmpty())) {
                throw new QueryException(searchType + " must has values");
            }
            result = searchType.convert(criteria, indexConfig);
        }
        if (result == null) {
            throw new QueryException(searchType + " not implemented.");
        }
        return result;
    }
}
