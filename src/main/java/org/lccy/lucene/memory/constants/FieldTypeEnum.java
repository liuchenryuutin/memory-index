package org.lccy.lucene.memory.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.lccy.lucene.memory.index.dto.IndexFieldDto;
import org.lccy.lucene.util.DateUtil;
import org.lccy.lucene.util.StringUtil;
import org.apache.lucene.document.*;
import org.apache.lucene.util.BytesRef;

import java.util.List;

/**
 * 根据配置的字段类型，格式化为lucene类型
 *
 * @Date: 2023/11/22 09:07 <br>
 * @author: liuchen11
 */
public enum FieldTypeEnum {

    KEYWORD("keyword", ((document, fieldName, fieldConfig, value) -> {
        if (value instanceof List) {
            List<Object> valList = (List<Object>) value;
            for (Object val : valList) {
                document.add(new StringField(fieldName, StringUtil.conver2String(val), Field.Store.YES));
                if (fieldConfig.isDocValue()) {
                    document.add(new SortedSetDocValuesField(fieldName, new BytesRef(StringUtil.conver2String(val))));
                }
            }
        } else {
            document.add(new StringField(fieldName, StringUtil.conver2String(value), Field.Store.YES));
            if (fieldConfig.isDocValue()) {
                document.add(new SortedDocValuesField(fieldName, new BytesRef(StringUtil.conver2String(value))));
            }
        }
    })), TEXT("text", ((document, fieldName, fieldConfig, value) -> {
        document.add(new TextField(fieldName, StringUtil.conver2String(value), Field.Store.YES));
        if (fieldConfig.isDocValue()) {
            document.add(new SortedDocValuesField(fieldName, new BytesRef(StringUtil.conver2String(value))));
        }
    })), DATE("date", ((document, fieldName, fieldConfig, value) -> {
        String dateStr = StringUtil.conver2String(value);
        String formats = fieldConfig.getFormat();
        if (StringUtil.isEmpty(formats)) {
            throw new IllegalArgumentException("field:" + fieldName + ", type:date must has format");
        }
        Long date = DateUtil.converTime(dateStr, formats.split("\\|\\|"));
        if (date == null) {
            throw new IllegalArgumentException("field:" + fieldName + ", date format error, value:" + value + ", format:" + fieldConfig.getFormat());
        }
        document.add(new LongPoint(fieldName, date));
        document.add(new StoredField(fieldName, dateStr));
        document.add(new NumericDocValuesField(fieldName, date));
    })), LONG("long", ((document, fieldName, fieldConfig, value) -> {
        Long data = Long.parseLong(StringUtil.conver2String(value));
        document.add(new LongPoint(fieldName, data));
        document.add(new StoredField(fieldName, data));
        document.add(new NumericDocValuesField(fieldName, data));
    })), FLOAT("float", ((document, fieldName, fieldConfig, value) -> {
        Float data = Float.parseFloat(StringUtil.conver2String(value));
        document.add(new FloatPoint(fieldName, data));
        document.add(new StoredField(fieldName, data));
        document.add(new NumericDocValuesField(fieldName, Float.floatToIntBits(data)));
    })), DOUBLE("double", ((document, fieldName, fieldConfig, value) -> {
        Double data = Double.parseDouble(StringUtil.conver2String(value));
        document.add(new DoublePoint(fieldName, data));
        document.add(new StoredField(fieldName, data));
        document.add(new NumericDocValuesField(fieldName, Double.doubleToLongBits(data)));
    })), STORE("store", ((document, fieldName, fieldConfig, value) -> {
        if (value instanceof List) {
            List<Object> valList = (List<Object>) value;
            for (Object val : valList) {
                document.add(new StoredField(fieldName, StringUtil.conver2String(val)));
            }
        } else {
            document.add(new StoredField(fieldName, StringUtil.conver2String(value)));
        }

    })), JSON("json", ((document, fieldName, fieldConfig, value) -> {
        String valueJson = com.alibaba.fastjson.JSON.toJSONString(value);
        document.add(new StringField(fieldName, valueJson, Field.Store.YES));
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

    public void convertField(Document document, String fieldName, IndexFieldDto fieldConfig, Object value) {
        if (value == null) {
            return;
        }
        this.convert.convert(document, fieldName, fieldConfig, value);
    }

    interface FieldTypeConvert {
        void convert(Document document, String fieldName, IndexFieldDto fieldConfig, Object value);
    }
}
