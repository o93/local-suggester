package me.o93.lib.suggester;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class DicDatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "local-suggester.db";
    private static final int DATABASE_VERSION = 1;

    private static DicDatabaseHelper sInstance;

    public static DicDatabaseHelper getInstance(final Context c) {
        if (sInstance == null) sInstance = new DicDatabaseHelper(c);
        return sInstance;
    }

    public DicDatabaseHelper(final Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase database, final ConnectionSource source) {
        try {
            TableUtils.createTable(source, DicJa.class);
            TableUtils.createTable(source, DicEn.class);
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
