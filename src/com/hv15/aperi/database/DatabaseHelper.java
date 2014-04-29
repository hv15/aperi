package com.hv15.aperi.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Wrapper for a {@link SQLiteDatabase} instance. This fulfils two purposes:
 * <ol>
 * <li>All database queries are <i>synchronised</i>, preventing memory
 * inconsistencies</li>
 * <li>The database instance is preserved between calls. Normally a good deal of
 * code would be needed for each query; with this wrapper, this isn't necessary
 * anymore</li>
 * </ol>
 * 
 * @author Hans-Nikolai Viessmann
 * 
 */
public class DatabaseHelper extends SQLiteOpenHelper
{
    private static DatabaseHelper mInstance = null;

    public static final String DATABASE_TABLE = "macrelip";
    public static final String DATABASE_MAC = "mac";
    public static final String DATABASE_IP = "ip";
    public static final String[] DATABASE_FIELDS = {
            DATABASE_MAC, DATABASE_IP
    };
    private static final String DATABASE_TABLE_CREATE = "CREATE TABLE "
            + DATABASE_TABLE + " (" + DATABASE_MAC + " TEXT NOT NULL UNIQUE, "
            + DATABASE_IP + " TEXT NOT NULL);";
    private static final int DATABASE_VERSION = 3;

    public DatabaseHelper(Context context)
    {
        super(context, null, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(DATABASE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // Will do nothing
    }

    /**
     * Get a <i>synchronised</i> instance of the {@link DatabaseHelper}
     * <p>
     * Using this instance, multiple objects can interact with the SQLite
     * database associated here.
     * 
     * @param context
     * @return a instance of {@link DatabaseHelper}
     */
    public static synchronized DatabaseHelper getHelper(Context context)
    {
        if(mInstance == null){
            mInstance = new DatabaseHelper(context);
        }
        return mInstance;
    }
}
