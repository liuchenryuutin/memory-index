package org.lccy.lucene.memory.builder;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.lccy.lucene.memory.exception.QueryException;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.memory.query.funcation.FunctionScoreQuery;
import org.lccy.lucene.memory.search.SearchCriteria;
import org.lccy.lucene.memory.search.SearchOption;
import org.lccy.lucene.memory.util.StringUtil;

import java.util.List;

/**
 * 查询Query构造
 *
 * @Date: 2023/12/15 23:51 <br>
 * @author: liuchen11
 */
public final class QueryBuilder {

    private QueryBuilder() {}

    /**
     * 构建查询条件
     *
     * @param criteriaList
     * @param indexConfig
     * @param parent
     * @return
     */
    public static Query createQuery(List<SearchCriteria> criteriaList, IndexConfig indexConfig, SearchCriteria parent) throws QueryException {
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
                Query childQuery = createQuery(criteria.getSubCriterias(), indexConfig, criteria);
                if (searchType == SearchOption.SearchType.bool) {
                    query = childQuery;
                } else if(searchType == SearchOption.SearchType.function_score) {
                    if(criteria.getScoreFunction() == null || criteria.getCombineFunction() == null) {
                        throw new QueryException("function score query must has scoreFunction and combineFunction");
                    }
                    float maxboost = criteria.getFunctionScoreMaxBoost() == null ? Float.MAX_VALUE : criteria.getFunctionScoreMaxBoost();
                    FunctionScoreQuery functionQuery = new FunctionScoreQuery(childQuery, criteria.getScoreFunction(), criteria.getCombineFunction(), maxboost);
                    query = functionQuery;
                } else if(searchType == SearchOption.SearchType.constant_score) {
                    ConstantScoreQuery constantScoreQuery = new ConstantScoreQuery(childQuery);
                    if(criteria.getBoost() != null) {
                        query = new BoostQuery(constantScoreQuery, criteria.getBoost());
                    } else {
                        query = constantScoreQuery;
                    }
                } else {
                    throw new QueryException("not support child query, except bool.");
                }
            } else if (searchType == SearchOption.SearchType.bool || searchType == SearchOption.SearchType.function_score
                    || searchType == SearchOption.SearchType.constant_score) {
                throw new QueryException("bool, funcation_score must has child query");
            } else {
                query = buildCriteria(criteria, indexConfig);
            }
            if (onlyOneCriteria && parent == null) {
                rootQuery = query;
            } else {
                searchLogic.convert(boolQueryBuilder, query);
            }
        }
        if (rootQuery == null) {
            rootQuery = boolQueryBuilder.build();
        }

        return rootQuery;
    }

    private static Query buildCriteria(SearchCriteria criteria, IndexConfig indexConfig) throws QueryException {
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
