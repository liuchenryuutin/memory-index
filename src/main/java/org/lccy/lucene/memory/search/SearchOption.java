package org.lccy.lucene.memory.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.lucene.document.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;
import org.lccy.lucene.memory.constants.FieldTypeEnum;
import org.lccy.lucene.memory.constants.SearchCriteriaSettingKey;
import org.lccy.lucene.memory.exception.QueryException;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.memory.index.mapping.IndexFieldMapping;
import org.lccy.lucene.memory.util.DateUtil;
import org.lccy.lucene.memory.util.StringUtil;
import org.lccy.lucene.memory.util.geo.DistanceUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/04/23 09:57 <br>
 * @author: liuchen11
 */
public class SearchOption {

    private SearchLogic searchLogic;
    private SearchType searchType;

    private SearchOption() {
    }

    public SearchOption(SearchLogic searchLogic, SearchType searchType) {
        this.searchLogic = searchLogic;
        this.searchType = searchType;
    }

    public SearchLogic getSearchLogic() {
        return searchLogic;
    }

    public void setSearchLogic(SearchLogic searchLogic) {
        this.searchLogic = searchLogic;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public enum SearchLogic {
        must(1),
        should(2),
        must_not(3),
        filter(4);

        private final int value;

        private SearchLogic(int v) {
            this.value = v;
        }

        @JsonValue
        public int value() {
            return this.value;
        }

        @JsonCreator
        public static SearchLogic fromValue(int typeCode) {
            SearchLogic[] var1 = values();
            int var2 = var1.length;

            for (int var3 = 0; var3 < var2; ++var3) {
                SearchLogic c = var1[var3];
                if (c.value == typeCode) {
                    return c;
                }
            }

            throw new IllegalArgumentException("Invalid Status type code: " + typeCode);
        }

        public void convert(BooleanQuery.Builder booleanQuery, Query childQuery) {
            switch (this) {
                case should:
                    booleanQuery.add(childQuery, BooleanClause.Occur.SHOULD);
                    break;
                case must:
                    booleanQuery.add(childQuery, BooleanClause.Occur.MUST);
                    break;
                case must_not:
                    booleanQuery.add(childQuery, BooleanClause.Occur.MUST_NOT);
                    break;
                case filter:
                    booleanQuery.add(childQuery, BooleanClause.Occur.FILTER);
                    break;
                default:
                    throw new RuntimeException("Not support SearchLogic");
            }
        }
    }

    public enum SearchType {
        match_all(0, false, false, false,
                ((criteria, indexConfig) -> new MatchAllDocsQuery())),

        /**
         * 全文查询 start
         */
        match(1, true, true, false,
                ((criteria, indexConfig) -> {
                    String field = criteria.getField();
                    if (!indexConfig.containsField(field)) {
                        throw new QueryException("The field is not configured and search is not supported, field:" + field);
                    }

                    String value = StringUtil.conver2String(criteria.getValues().get(0));
                    IndexFieldMapping fieldConf = indexConfig.getFieldConfig(field);
                    Query query;
                    try {
                        query = new QueryParser(field, fieldConf.convertSearchAnalyzer()).parse(QueryParserUtil.escape(value));
                        return query;
                    } catch (ParseException e) {
                        throw new QueryException("lucene match query parse error", e);
                    }
                })),

        match_bool_prefix(2, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),

        match_phrase(3, true, true, false,
                ((criteria, indexConfig) -> {
                    String field = criteria.getField();
                    if (!indexConfig.containsField(field)) {
                        throw new QueryException("The field is not configured and search is not supported, field:" + field);
                    }

                    String value = StringUtil.conver2String(criteria.getValues().get(0));
                    IndexFieldMapping fieldConf = indexConfig.getFieldConfig(field);
                    QueryBuilder builder = new QueryBuilder(fieldConf.convertSearchAnalyzer());
                    return builder.createPhraseQuery(field, value);
                })),

        match_phrase_prefix(4, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),

        multi_match(5, true, true, false,
                ((criteria, indexConfig) -> {
                    String[] fields = criteria.getField().split(",");
                    String value = StringUtil.conver2String(criteria.getValues().get(0));
                    List<Query> multiList = new ArrayList<>();
                    try {
                        for (int i = 0; i < fields.length; i++) {
                            String[] t = fields[i].split("=");
                            if (t.length != 2) {
                                throw new QueryException("multi_match must has boost, example: title=3,contxt=1");
                            }
                            String field = t[0];
                            float boost = Float.parseFloat(t[1]);
                            if (!indexConfig.containsField(field)) {
                                throw new QueryException("The field is not configured and search is not supported, field:" + field);
                            }

                            IndexFieldMapping fieldConf = indexConfig.getFieldConfig(field);
                            BoostQuery boostQuery = new BoostQuery(new QueryParser(field, fieldConf.convertSearchAnalyzer()).parse(QueryParserUtil.escape(value)), boost);
                            multiList.add(boostQuery);
                        }
                    } catch (ParseException ex) {
                        throw new QueryException("lucene match query parse error", ex);
                    }
                    return new DisjunctionMaxQuery(multiList, 0f);
                })),

        query_string(6, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),

        simple_query_string(7, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),
        /** 全文查询 end*/

        /**
         * 词项级查询 start
         */
        term(8, true, true, true,
                ((criteria, indexConfig) -> {
                    String field = criteria.getField();
                    if (!indexConfig.containsField(field)) {
                        throw new QueryException("The field is not configured and search is not supported, field:" + field);
                    }
                    List<Object> values = criteria.getValues();
                    IndexFieldMapping fieldConf = indexConfig.getFieldConfig(field);
                    if (FieldTypeEnum.KEYWORD != fieldConf.getType() && FieldTypeEnum.TEXT != fieldConf.getType()) {
                        throw new QueryException("Only keyword | text type supports term query, field:" + field);
                    }
                    if (values.size() == 1) {
                        String value = StringUtil.conver2String(criteria.getValues().get(0));
                        return new TermQuery(new Term(field, value));
                    } else {
                        List<BytesRef> valusList = values.stream().map(x -> new BytesRef(StringUtil.conver2String(x))).collect(Collectors.toList());
                        return new TermInSetQuery(field, valusList);
                    }
                })),

        exists(9, true, false, false,
                ((criteria, indexConfig) -> {
                    String field = criteria.getField();
                    IndexFieldMapping fieldConf = indexConfig.getFieldConfig(field);
                    Query query;
                    switch (fieldConf.getType()) {
                        case KEYWORD:
                        case TEXT:
                        case JSON:
                            if (fieldConf.isDocValue()) {
                                query = new DocValuesFieldExistsQuery(field);
                            } else {
                                query = new NormsFieldExistsQuery(field);
                            }
                            break;
                        case DATE:
                        case LONG:
                        case FLOAT:
                        case DOUBLE:
                        case GEO_POINT:
                            query = new DocValuesFieldExistsQuery(field);
                            break;
                        default:
                            throw new QueryException("Field" + field + "not support exists query");
                    }
                    return query;
                })),

        ids(10, false, true, true,
                ((criteria, indexConfig) -> {
                    String field = indexConfig.getPrimaryField().getName();
                    if (!indexConfig.containsField(field)) {
                        throw new QueryException("The field is not configured and search is not supported, field:" + field);
                    }
                    List<Object> values = criteria.getValues();
                    if (values.size() == 1) {
                        String value = StringUtil.conver2String(criteria.getValues().get(0));
                        return new TermQuery(new Term(field, value));
                    } else {
                        List<BytesRef> valusList = values.stream().map(x -> new BytesRef(StringUtil.conver2String(x))).collect(Collectors.toList());
                        return new TermInSetQuery(field, valusList);
                    }
                })),

        prefix(11, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),

        range(12, true, true, true,
                (criteria, indexConfig) -> {
                    String field = criteria.getField();
                    List<Object> values = criteria.getValues();
                    if (!indexConfig.containsField(field)) {
                        throw new QueryException("The field is not configured and search is not supported, field:" + field);
                    }
                    if (values.size() < 2) {
                        throw new QueryException("range query need 2 value, please check.");
                    }
                    Object lowerObj = values.get(0);
                    Object upperObj = values.get(1);
                    boolean lowerInclude = criteria.getSetting(SearchCriteriaSettingKey.range_includeLower, true, boolean.class);
                    boolean upperInclude = criteria.getSetting(SearchCriteriaSettingKey.range_includeUpper, true, boolean.class);
                    IndexFieldMapping fieldConf = indexConfig.getFieldConfig(field);
                    Query query = null;
                    switch (fieldConf.getType()) {
                        case KEYWORD:
                            BytesRef lowerByte = lowerObj == null ? null : new BytesRef(StringUtil.conver2String(lowerObj));
                            BytesRef upperByte = upperObj == null ? null : new BytesRef(StringUtil.conver2String(upperObj));
                            query = new TermRangeQuery(field, lowerByte, upperByte, lowerInclude, upperInclude);
                            break;
                        case DATE:
                            long lowerDate;
                            if (lowerObj != null) {
                                lowerDate = DateUtil.convertTime(StringUtil.conver2String(lowerObj), fieldConf.getFormat().split("\\|\\|"));
                            } else {
                                lowerDate = Long.MIN_VALUE;
                            }
                            if (!lowerInclude && lowerDate != Long.MAX_VALUE) {
                                lowerDate = Math.addExact(lowerDate, 1);
                            }
                            long upperDate;
                            if (upperObj != null) {
                                upperDate = DateUtil.convertTime(StringUtil.conver2String(upperObj), fieldConf.getFormat().split("\\|\\|"));
                            } else {
                                upperDate = Long.MAX_VALUE;
                            }
                            if (!upperInclude && upperDate != Long.MIN_VALUE) {
                                upperDate = Math.addExact(upperDate, -1);
                            }
                            query = LongPoint.newRangeQuery(field, lowerDate, upperDate);
                            break;
                        case LONG:
                            long lowerLong;
                            if (lowerObj != null) {
                                lowerLong = Long.parseLong(StringUtil.conver2String(lowerObj));
                            } else {
                                lowerLong = Long.MIN_VALUE;
                            }
                            if (!lowerInclude && lowerLong != Long.MAX_VALUE) {
                                lowerLong = Math.addExact(lowerLong, 1);
                            }
                            long upperLong;
                            if (upperObj != null) {
                                upperLong = Long.parseLong(StringUtil.conver2String(upperObj));
                            } else {
                                upperLong = Long.MAX_VALUE;
                            }
                            if (!upperInclude && upperLong != Long.MIN_VALUE) {
                                upperLong = Math.addExact(upperLong, -1);
                            }
                            query = LongPoint.newRangeQuery(field, lowerLong, upperLong);
                            break;

                        case FLOAT:
                            float lowerFloat;
                            if (lowerObj != null) {
                                lowerFloat = Float.parseFloat(StringUtil.conver2String(lowerObj));
                            } else {
                                lowerFloat = Float.MIN_VALUE;
                            }
                            if (!lowerInclude && lowerFloat != Float.MAX_VALUE) {
                                lowerFloat = lowerFloat + 1f;
                            }
                            float upperFloat;
                            if (upperObj != null) {
                                upperFloat = Float.parseFloat(StringUtil.conver2String(upperObj));
                            } else {
                                upperFloat = Float.MAX_VALUE;
                            }
                            if (!upperInclude && upperFloat != Float.MIN_VALUE) {
                                upperFloat = upperFloat - 1f;
                            }
                            query = FloatPoint.newRangeQuery(field, lowerFloat, upperFloat);
                            break;

                        case DOUBLE:
                            double lowerDouble;
                            if (lowerObj != null) {
                                lowerDouble = Double.parseDouble(StringUtil.conver2String(lowerObj));
                            } else {
                                lowerDouble = Double.MIN_VALUE;
                            }
                            if (!lowerInclude && lowerDouble != Double.MAX_VALUE) {
                                lowerDouble = lowerDouble + 1;
                            }
                            double upperDouble;
                            if (upperObj != null) {
                                upperDouble = Double.parseDouble(StringUtil.conver2String(upperObj));
                            } else {
                                upperDouble = Double.MAX_VALUE;
                            }
                            if (!upperInclude && upperDouble != Double.MIN_VALUE) {
                                upperDouble = upperDouble - 1;
                            }
                            query = DoublePoint.newRangeQuery(field, lowerDouble, upperDouble);
                            break;
                        default:
                            throw new QueryException("Only keyword | long | date | float | double field support range query");
                    }
                    return query;
                }),

        regexp(13, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),

        wildcard(14, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),

        terms_set(15, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),
        /** 词项级查询 end*/

        /**
         * 地理位置 start
         */
        geo_bounding_box(16, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),

        geo_distance(17, true, true, false,
                ((criteria, indexConfig) -> {
                    String field = criteria.getField();
                    if (!indexConfig.containsField(field)) {
                        throw new QueryException("The field is not configured and search is not supported, field:" + field);
                    }
                    String value = StringUtil.conver2String(criteria.getValues().get(0));
                    String[] vals = value.split(",");
                    if (vals.length < 3) {
                        throw new QueryException("geo_distance query need value, please check.");
                    }
                    Double lat = Double.parseDouble(vals[0].trim());
                    Double lon = Double.parseDouble(vals[1].trim());
                    Double meter = DistanceUnit.DEFAULT.parse(vals[2].trim(), DistanceUnit.DEFAULT);
                    if(meter == null) {
                        throw new QueryException("geo_distance meter setting error, example:10m");
                    }

                    // 两种方式，看哪种快，会比较Weight的cost
                    Query query = LatLonPoint.newDistanceQuery(field, lat, lon, meter);
                    if (indexConfig.getFieldConfig(field).isDocValue()) {
                        Query dvQuery = LatLonDocValuesField.newSlowDistanceQuery(field, lat, lon, meter);
                        query = new IndexOrDocValuesQuery(query, dvQuery);
                    }
                    return query;
                })),

        geo_polygon(18, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),

        geo_shape(19, true, true, false,
                ((criteria, indexConfig) -> {
                    throw new QueryException("Method not implemented");
                })),
        /** 地理位置 end*/


        /**
         * 连结查询 end
         */
        function_score(24, false, false, false, null),

        // bool连接，子嵌套查询用
        bool(25, false, false, true, null),

        // 自定义查询用
        custom(26, false, false, true, null),

        // 无评分查询
        constant_score(27, false, false, true, null);

        private int value;
        private boolean mustField;
        private boolean mustValue;
        private boolean accurate;
        private SearchConvert convert;

        SearchType(int value, boolean mustField, boolean mustValue, boolean accurate, SearchConvert convert) {
            this.value = value;
            this.mustField = mustField;
            this.mustValue = mustValue;
            this.accurate = accurate;
            this.convert = convert;
        }

        public int getValue() {
            return value;
        }

        public boolean isMustField() {
            return mustField;
        }

        public boolean isMustValue() {
            return mustValue;
        }

        public boolean isAccurate() {
            return accurate;
        }

        public SearchConvert getConvert() {
            return convert;
        }

        public Query convert(SearchCriteria criteria, IndexConfig indexConfig) {
            return this.convert.convert(criteria, indexConfig);
        }

    }

    public interface SearchConvert {
        Query convert(SearchCriteria criteria, IndexConfig indexConfig) throws QueryException;
    }


}
