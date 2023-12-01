package org.lccy.lucene.memory.builder;

import org.apache.lucene.document.LatLonDocValuesField;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.lccy.lucene.memory.constants.Constants;
import org.lccy.lucene.memory.constants.FieldTypeEnum;
import org.lccy.lucene.memory.exception.QueryException;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.memory.index.mapping.IndexFieldMapping;
import org.lccy.lucene.memory.search.SortFieldInfo;
import org.lccy.lucene.memory.util.CollectionUtils;
import org.lccy.lucene.memory.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 排序Sort构造
 *
 * @Date: 2023/12/15 23:31 <br>
 * @author: liuchen11
 */
public final class SortBuilder {

    private SortBuilder() {}

    /**
     * 构造排序sort
     *
     * @param sorts
     * @param indexConfig
     * @return
     */
    public static Sort buildSort(List<SortFieldInfo> sorts, IndexConfig indexConfig) {
        Sort result = null;
        if (CollectionUtils.isNotEmpty(sorts)) {
            List<SortField> sortFieldList = new ArrayList<>();
            for (SortFieldInfo sortInfo : sorts) {
                String fieldName = sortInfo.getName();
                if (StringUtil.isEmpty(fieldName)) {
                    throw new QueryException("Sort must has field name.");
                }
                if (Constants._SCORE.equals(fieldName)) {
                    sortFieldList.add(SortField.FIELD_SCORE);
                    continue;
                }
                if (Constants._ID.equals(fieldName)) {
                    sortFieldList.add(SortField.FIELD_DOC);
                    continue;
                }
                IndexFieldMapping fieldConf = indexConfig.getFieldConfig(fieldName);
                if (fieldConf.isDefaultFd() || !fieldConf.canSort()) {
                    throw new QueryException("field:" + fieldName + " not support sort.");
                }
                SortField sortField;
                if (fieldConf.getType() == FieldTypeEnum.GEO_POINT) {
                    String point = StringUtil.conver2String(sortInfo.getValue());
                    if (StringUtil.isEmpty(point) || point.indexOf(",") < 0) {
                        throw new QueryException("geo sort must has lat and lon, example:34.1, 115.1");
                    }
                    String[] vals = point.split(",", -1);
                    Double lat = Double.parseDouble(vals[0].trim());
                    Double lon = Double.parseDouble(vals[1].trim());
                    sortField = LatLonDocValuesField.newDistanceSort(fieldName, lat, lon);
                } else {
                    boolean reverse = false;
                    if(SortFieldInfo.SortMode.DESC == sortInfo.getSortMode()) {
                        reverse = true;
                    }
                    sortField = new SortField(fieldName, fieldConf.getSortType(), reverse);
                    if (!fieldConf.checkMissing(sortInfo.getMissingValue())) {
                        throw new QueryException("Sort missing value is illegal.");
                    } else {
                        sortField.setMissingValue(sortInfo.getMissingValue());
                    }
                }
                sortFieldList.add(sortField);
            }

            result = new Sort(sortFieldList.toArray(new SortField[sortFieldList.size()]));
        }
        return result;
    }

}
