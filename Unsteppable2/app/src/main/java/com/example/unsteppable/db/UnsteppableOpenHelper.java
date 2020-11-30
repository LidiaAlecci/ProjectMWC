package com.example.unsteppable.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

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
        //Log.v(TAG, "created two tables");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing to do here
    }

    public static void insertSingleStep(Context context, String timestamp, String day, String hour){
        ContentValues values = new ContentValues();
        values.put(UnsteppableOpenHelper.KEY_TIMESTAMP, timestamp);
        values.put(UnsteppableOpenHelper.KEY_DAY, day);
        values.put(UnsteppableOpenHelper.KEY_HOUR, hour);
        SQLiteDatabase database = getDatabase(context);
        long id = database.insert(UnsteppableOpenHelper.TABLE_NAME1, null, values);
        Log.v(TAG, "insertSingleStep() - Id: " + id);
    }

    public static void insertDayReport(Context context,String timestamp, String day, Integer baseGoal, Integer actualGoal){
        String lastDayInTab2 = UnsteppableOpenHelper.getLastDayFromTab2(context);
        Log.d(TAG, "value of lastDayInTab2: " + lastDayInTab2);
        String[] currentDayAndDate = getCurrentDayAndDate();
        if(day == null || timestamp == null){
            day = currentDayAndDate[0];
            timestamp = currentDayAndDate[1];
        }
        ContentValues values = new ContentValues();
        values.put(UnsteppableOpenHelper.KEY_TIMESTAMP, timestamp);
        values.put(UnsteppableOpenHelper.KEY_DAY, day);
        values.put(UnsteppableOpenHelper.KEY_BASE_GOAL , baseGoal);
        values.put(UnsteppableOpenHelper.KEY_ACTUAL_GOAL , actualGoal);
        Integer androidStepCounter = UnsteppableOpenHelper.getStepsByDayFromTab1(context,day);
        values.put(UnsteppableOpenHelper.KEY_STEPS, androidStepCounter);
        SQLiteDatabase database = getDatabase(context);
        if(lastDayInTab2 != null){
            if(!day.equals(lastDayInTab2)){
                long id = database.insert(UnsteppableOpenHelper.TABLE_NAME2, null, values);
                Log.v("DATABASE TABLE2", "Insert row - LONG " + id);
            }else{
                long id = database.update(UnsteppableOpenHelper.TABLE_NAME2, values, "day = ?", new String[]{day});
                Log.v("DATABASE TABLE2", "Update row - LONG " + id);
            }
        }
        String currentDay = currentDayAndDate[0];
        if(!currentDay.equals(day) || lastDayInTab2 == null){ // if it's a new day or the table 2 it's empty
            values.clear();
            values.put(UnsteppableOpenHelper.KEY_TIMESTAMP, currentDayAndDate[1]);
            values.put(UnsteppableOpenHelper.KEY_DAY, currentDayAndDate[0]);
            values.put(UnsteppableOpenHelper.KEY_BASE_GOAL , baseGoal);
            values.put(UnsteppableOpenHelper.KEY_ACTUAL_GOAL , actualGoal);
            values.put(UnsteppableOpenHelper.KEY_STEPS, 0);
            long id = database.insert(UnsteppableOpenHelper.TABLE_NAME2, null, values);
            Log.v(TAG, "Insert empty row - LONG " + id);
        }

        long id = database.update(UnsteppableOpenHelper.TABLE_NAME2, values, "day = ?", new String[]{day});
        Log.v(TAG, "Update row - LONG " + id);

    }

    public static SQLiteDatabase getDatabase(Context context){
        UnsteppableOpenHelper databaseHelper = new UnsteppableOpenHelper(context);
        return databaseHelper.getReadableDatabase();
    }

    public static Integer getStepsByDayFromTab1(Context context, String date){
        List<String> steps = new LinkedList<String>();
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);

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
        SQLiteDatabase database = getDatabase(context);


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

    // utility to have current day and the timestamp in the right format
    private static String[] getCurrentDayAndDate(){
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        jdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        String currentDate = jdf.format(System.currentTimeMillis());
        // Get the date, the day and the hour
        String currentDay = currentDate.substring(0,10);
        return new String[]{currentDay, currentDate};
    }

}
