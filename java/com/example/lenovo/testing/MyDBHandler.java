package com.example.lenovo.testing;

import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.widget.Toast;

public class MyDBHandler extends SQLiteOpenHelper {
    //information of database
    private static final int DATABASE_VERSION = 1;
    private long num = 1;
    private static final String DATABASE_NAME = "GPS_TIME_STAMP.db";
    public static final String TABLE_NAME = "Date_Time_Stamp";
    public static final String COLUMN_ID = "Serial";
    public static final String COLUMN_DATE = "Date";
    public static final String COLUMN_LOC = "Location";

    //initialize the database
    public MyDBHandler(Context context, SQLiteDatabase.CursorFactory factory) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
            String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "( " + COLUMN_ID +
                    " INTEGER PRIMARY KEY autoincrement, " + COLUMN_DATE + " TEXT, " + COLUMN_LOC + " TEXT )";
            db.execSQL(CREATE_TABLE);
    }

    private boolean checkDataBase() {
        SQLiteDatabase checkDB = this.getWritableDatabase();
        String path = checkDB.getPath();
        try {
            checkDB = SQLiteDatabase.openDatabase(path, null,
                    SQLiteDatabase.OPEN_READONLY);
            checkDB.close();
            return true;
        } catch (SQLiteException e) {
            // database doesn't exist yet.
            return false;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {}

    public String loadHandler() {
        String result = "";
        String query = "Select * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            int result_0 = cursor.getInt(0);
            String result_1 = cursor.getString(1);
            String result_2 = cursor.getString(2);
            result += String.valueOf(result_0) + " " + result_1 + " " + result_2 +
                    System.getProperty("line.separator");
        }
        cursor.close();
        db.close();
        return result;
    }

    public void addHandler(Time_Stamp TS) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, TS.getDate());
        values.put(COLUMN_LOC, TS.getLocation());
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TABLE_NAME, null, values);
        db.close();
    }
}
