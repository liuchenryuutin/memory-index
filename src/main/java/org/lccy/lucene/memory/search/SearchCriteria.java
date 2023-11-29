package org.lccy.lucene.memory.search;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.search.Query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/04/23 09:30 <br>
 * @author: liuchen11
 */
@Getter
@Setter
public class SearchCriteria implements Serializable {
    private static final long serialVersionUID = 9152243091714512036L;
    private String field;
    private List<Object> values;
    private SearchOption option;
    private List<SearchCriteria> subCriterias;
    private Query customQuery;
    private Map<String, Object> settings;
    public SearchCriteria() {}

    public SearchCriteria(SearchOption searchOption) {
        this();
        this.option = searchOption;
    }

    public SearchCriteria(String field, SearchOption searchOption) {
        this(searchOption);
        this.field = field;
    }

    public SearchCriteria(String field, Object value, SearchOption searchOption) {
        this(field, searchOption);
        if (value instanceof List) {
            this.setValues((List) value);
        } else if (value instanceof Object[]) {
            for (Object val : (Object[]) value) {
                this.addValue(val);
            }
        } else {
            this.addValue(value);
        }
    }

    public SearchCriteria(String field, Object value, SearchOption searchOption, Map settings) {
        this(field, value, searchOption);
        this.settings = settings;
    }

    public SearchCriteria addSubCriterias(SearchCriteria sub) {
        if (this.subCriterias == null) {
            this.subCriterias = new ArrayList<>();
        }
        this.subCriterias.add(sub);
        return this;
    }

    public SearchCriteria createSubCriterias() {
        if (this.subCriterias == null) {
            this.subCriterias = new ArrayList<>();
        }
        SearchCriteria sub = new SearchCriteria();
        this.subCriterias.add(sub);
        return sub;
    }

    public void addValue(Object value) {
        if(this.values == null) {
            this.values = new ArrayList();
        }
        this.values.add(value);
    }

    public void addValues(List<Object> values) {
        if(this.values == null) {
            this.values = new ArrayList();
        }
        this.values.addAll(values);
    }

    public Map<String, Object> putSettings(String name, String value) {
        if(this.settings == null) {
            this.settings = new HashMap<>();
        }
        this.settings.put(name, value);
        return this.settings;
    }

    public boolean hasSubCriterias() {
        return subCriterias != null && !subCriterias.isEmpty();
    }

    public Object getSetting(String key) {
        if(this.settings == null || this.settings.isEmpty() || !this.settings.containsKey(key)) {
            return null;
        }
        return settings.get(key);
    }

    public <T> T getSetting(String key, T defaultVal, Class<T> type) {
        Object value = getSetting(key);
        if (value == null) {
            return defaultVal;
        }
        if(type.isInstance(value)) {
            return type.cast(value);
        }
        return defaultVal;
    }
}
