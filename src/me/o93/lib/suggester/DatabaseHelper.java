package me.o93.lib.suggester;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "local-suggester.db";
    private static final int DATABASE_VERSION = 1;

    private static DatabaseHelper sInstance;

    public static DatabaseHelper getInstance(final Context c) {
        if (sInstance == null) sInstance = new DatabaseHelper(c);
        return sInstance;
    }

    public DatabaseHelper(final Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase database, final ConnectionSource source) {
        try {
            TableUtils.createTable(source, Dictionary.class);
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onUpgrade(
            final SQLiteDatabase database, final ConnectionSource source,
            final int oldVersion, final int newVersion) {
    }

}
