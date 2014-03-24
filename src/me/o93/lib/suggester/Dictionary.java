package me.o93.lib.suggester;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "Dictionary")
public class Dictionary {
    private static final String TAG = "Dictionary";

    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String TYPE = "type";
    public static final String PRIORITY = "priority";

    public static final int TYPE_JA = 0;
    public static final int TYPE_EN = 1;

    @DatabaseField(generatedId = true)
    private Integer id;

    @DatabaseField(columnName = KEY, canBeNull = false, index = true, indexName = "DictionaryKey")
    public String key;
    @DatabaseField(columnName = VALUE, canBeNull = false, index = true, indexName = "DictionaryValue")
    public String value;
    @DatabaseField(columnName = TYPE, canBeNull = false, index = false, defaultValue = "0")
    public Integer type = 0;

    @DatabaseField(columnName = PRIORITY, canBeNull = false, index = false, defaultValue = "0")
    public Integer priority = 0;

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(TAG);
        builder
            .append("{")
            .append(" key:").append(key)
            .append(" value:").append(value)
            .append(" type:").append(type)
            .append(" priority:").append(priority)
            .append(" }");
        return builder.toString();
    }
}
