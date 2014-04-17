package me.o93.lib.suggester;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "DicEn")
public class DicEn {
    private static final String TAG = "DicEn";

    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String PRIORITY = "priority";

    @DatabaseField(generatedId = true)
    private Integer _id;

    @DatabaseField(columnName = KEY, canBeNull = false, index = true, indexName = "DicEnKey", width = 24)
    public String key;
    @DatabaseField(columnName = VALUE, canBeNull = false, index = false, width = 12)
    public String value;

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(TAG);
        builder
            .append("{")
            .append(" key:").append(key)
            .append(" value:").append(value)
            .append(" }");
        return builder.toString();
    }
}
