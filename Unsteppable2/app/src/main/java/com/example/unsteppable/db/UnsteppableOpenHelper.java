package com.example.unsteppable.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class UnsteppableOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "unsteppable";
    public static final String TAG = "DATABASE";

    public static final String TABLE_NAME1 = "num_steps";
    public static final String TABLE_NAME2 = "dashboard";
    public static final String KEY_ID = "id";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_BASE_GOAL = "baseGoal";
    public static final String KEY_ACTUAL_GOAL = "actualGoal";
    public static final String KEY_STEPS = "steps";
    public static final String KEY_DAY = "day";
    public static final String KEY_HOUR = "hour";

    // Default SQL for creating a table in a database
    public static final String CREATE_TABLE2_SQL = "CREATE TABLE " + TABLE_NAME2 + " (" +
            KEY_ID + " INTEGER PRIMARY KEY, " + KEY_DAY + " TEXT, " + KEY_BASE_GOAL + " TEXT, "
            + KEY_ACTUAL_GOAL + " TEXT, " + KEY_STEPS + " TEXT, "  + KEY_TIMESTAMP + " TEXT);";

    public static final String CREATE_TABLE1_SQL = "CREATE TABLE " + TABLE_NAME1 + " (" +
            KEY_ID + " INTEGER PRIMARY KEY, " + KEY_DAY + " TEXT, " + KEY_HOUR + " TEXT, "
            + KEY_TIMESTAMP + " TEXT);";

    // Constructor
    public UnsteppableOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // onCreate
    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE1_SQL);
        db.execSQL(CREATE_TABLE2_SQL);
        Log.v(TAG, "created two tables");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing to do here
    }

    public static Integer getStepsByDayFromTab1(Context context, String date){
        Log.d("STORED STEPS TODAY: ", "In getStepsByDayFromTab1()");
        List<String> steps = new LinkedList<String>();
        // Get the readable database
        UnsteppableOpenHelper databaseHelper = new UnsteppableOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String where = UnsteppableOpenHelper.KEY_DAY + " = ?";
        String [] whereArgs = { date };

        Cursor cursor = database.query(UnsteppableOpenHelper.TABLE_NAME1, null, where, whereArgs, null,
                null, null );

        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            steps.add(cursor.getString(0));
            cursor.moveToNext();
        }
        database.close();

        Integer numSteps = steps.size();
        Log.d("STORED STEPS TODAY: ", String.valueOf(numSteps));
        return numSteps;
    }

    public static String getLastDayFromTab2(Context context){
        String day = null;
        // Get the readable database
        UnsteppableOpenHelper databaseHelper = new UnsteppableOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();


        Cursor cursor = database.rawQuery("SELECT * FROM " +UnsteppableOpenHelper.TABLE_NAME2 + " ORDER BY id DESC LIMIT 1;", null);

        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            day = cursor.getString(cursor.getColumnIndex("day"));
            cursor.moveToNext();
        }
        database.close();

        return day;
    }

}
