package me.o93.lib.suggester;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "DicJa")
public class DicJa {
    private static final String TAG = "DicJa";

    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String PRIORITY = "priority";

    @DatabaseField(generatedId = true)
    private Integer _id;

    @DatabaseField(columnName = KEY, canBeNull = false, index = true, indexName = "DicJaKey", width = 12)
    public String key;
    @DatabaseField(columnName = VALUE, canBeNull = false, index = false)
    public String value;

    @DatabaseField(columnName = PRIORITY, canBeNull = false, index = false, defaultValue = "0", width = 12)
    public Integer priority = 0;

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(TAG);
        builder
            .append("{")
            .append(" key:").append(key)
            .append(" value:").append(value)
            .append(" priority:").append(priority)
            .append(" }");
        return builder.toString();
    }
}
