package org.lccy.lucene.memory.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.lucene.document.*;
import org.apache.lucene.util.BytesRef;
import org.lccy.lucene.memory.exception.LuceneException;
import org.lccy.lucene.memory.index.mapping.IndexFieldMapping;
import org.lccy.lucene.memory.util.DateUtil;
import org.lccy.lucene.memory.util.StringUtil;

import java.util.List;

/**
 * 根据配置的字段类型，格式化为lucene类型
 *
 * @Date: 2023/11/22 09:07 <br>
 * @author: liuchen11
 */
public enum FieldTypeEnum {

    KEYWORD("keyword", ((document, fieldName, fieldConfig, value) -> {
        Field.Store store = fieldConfig.isStore() ? Field.Store.YES : Field.Store.NO;
        if (value instanceof List) {
            List<Object> valList = (List<Object>) value;
            for (Object val : valList) {
                if(val != null) {
                    document.add(new StringField(fieldName, StringUtil.conver2String(val), store));
                    if (fieldConfig.isDocValue()) {
                        document.add(new SortedSetDocValuesField(fieldName, new BytesRef(StringUtil.conver2String(val))));
                    }
                }
            }
        } else {
            document.add(new StringField(fieldName, StringUtil.conver2String(value), store));
            if (fieldConfig.isDocValue()) {
                document.add(new SortedSetDocValuesField(fieldName, new BytesRef(StringUtil.conver2String(value))));
            }
        }
    })), TEXT("text", ((document, fieldName, fieldConfig, value) -> {
        Field.Store store = fieldConfig.isStore() ? Field.Store.YES : Field.Store.NO;
        document.add(new TextField(fieldName, StringUtil.conver2String(value), store));
        if (fieldConfig.isDocValue()) {
            document.add(new SortedDocValuesField(fieldName, new BytesRef(StringUtil.conver2String(value))));
        }
    })), DATE("date", ((document, fieldName, fieldConfig, value) -> {
        String dateStr = StringUtil.conver2String(value);
        String formats = fieldConfig.getFormat();
        if (StringUtil.isEmpty(formats)) {
            throw new IllegalArgumentException("field:" + fieldName + ", type:date must has format");
        }
        Long date = DateUtil.convertTime(dateStr, formats.split("\\|\\|"));
        if (date == null) {
            throw new IllegalArgumentException("field:" + fieldName + ", date format error, value:" + value + ", format:" + fieldConfig.getFormat());
        }
        document.add(new LongPoint(fieldName, date));
        document.add(new NumericDocValuesField(fieldName, date));
        if(fieldConfig.isStore()) {
            document.add(new StoredField(fieldName, dateStr));
        }
    })), LONG("long", ((document, fieldName, fieldConfig, value) -> {
        Long data = Long.parseLong(StringUtil.conver2String(value));
        document.add(new LongPoint(fieldName, data));
        document.add(new NumericDocValuesField(fieldName, data));
        if(fieldConfig.isStore()) {
            document.add(new StoredField(fieldName, data));
        }
    })), FLOAT("float", ((document, fieldName, fieldConfig, value) -> {
        Float data = Float.parseFloat(StringUtil.conver2String(value));
        document.add(new FloatPoint(fieldName, data));
        document.add(new NumericDocValuesField(fieldName, Float.floatToIntBits(data)));
        if(fieldConfig.isStore()) {
            document.add(new StoredField(fieldName, data));
        }
    })), DOUBLE("double", ((document, fieldName, fieldConfig, value) -> {
        Double data = Double.parseDouble(StringUtil.conver2String(value));
        document.add(new DoublePoint(fieldName, data));
        document.add(new NumericDocValuesField(fieldName, Double.doubleToLongBits(data)));
        if(fieldConfig.isStore()) {
            document.add(new StoredField(fieldName, data));
        }
    })), STORE("store", ((document, fieldName, fieldConfig, value) -> {
        if (value instanceof List) {
            List<Object> valList = (List<Object>) value;
            for (Object val : valList) {
                if(val != null) {
                    document.add(new StoredField(fieldName, StringUtil.conver2String(val)));
                }
            }
        } else {
            document.add(new StoredField(fieldName, StringUtil.conver2String(value)));
        }

    })), JSON("json", ((document, fieldName, fieldConfig, value) -> {
        Field.Store store = fieldConfig.isStore() ? Field.Store.YES : Field.Store.NO;
        String valueJson = com.alibaba.fastjson.JSON.toJSONString(value);
        document.add(new StringField(fieldName, valueJson, store));
    })), GEO_POINT("geo_point",((document, fieldName, fieldConfig, value) -> {
        String point = StringUtil.conver2String(value);
        if(point.indexOf(",") < 0) {
            throw new LuceneException("geo_point setting error, please enter the correct value, example: 32.1, 33.2");
        }
        String[] vals = point.split(",", -1);
        Double lat = Double.parseDouble(vals[0].trim());
        Double lon = Double.parseDouble(vals[1].trim());
        document.add(new LatLonPoint(fieldName, lat, lon));
        document.add(new LatLonDocValuesField(fieldName, lat, lon));
        if(fieldConfig.isStore()) {
            document.add(new StoredField(fieldName, point));
        }
    })), NESTED("nested", ((document, fieldName, fieldConfig, value) -> {
        throw new LuceneException("nested not implemented");
    }));

    private String name;
    private FieldTypeConvert convert;

    FieldTypeEnum(String name, FieldTypeConvert convert) {
        this.name = name;
        this.convert = convert;
    }

    public String getName() {
        return name;
    }

    public FieldTypeConvert getConvert() {
        return convert;
    }

    @JsonValue
    public String value() {
        return this.name;
    }

    @JsonCreator
    public static FieldTypeEnum fromValue(String name) {
        for (FieldTypeEnum c : values()) {
            if (c.name.equals(name)) {
                return c;
            }
        }

        throw new IllegalArgumentException("FieldTypeEnum invalid name: " + name);
    }

    public void convertField(Document document, String fieldName, IndexFieldMapping fieldConfig, Object value) {
        if (value == null) {
            return;
        }
        this.convert.convert(document, fieldName, fieldConfig, value);
    }

    interface FieldTypeConvert {
        void convert(Document document, String fieldName, IndexFieldMapping fieldConfig, Object value);
    }
}
