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

    public static final String TABLE_STEPS = "steps";
    public static final String TABLE_DASHBOARD = "dashboard";
    public static final String KEY_ID = "id";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_BASE_GOAL = "baseGoal";
    public static final String KEY_ACTUAL_GOAL = "actualGoal";
    public static final String KEY_STEPS = "steps";
    public static final String KEY_DAY = "day";
    public static final String KEY_HOUR = "hour";

    // Default SQL for creating a table in a database

    public static final String CREATE_TABLE_DASHBOARD_SQL = "CREATE TABLE " + TABLE_DASHBOARD + " (" +
            KEY_ID + " INTEGER PRIMARY KEY, " + KEY_DAY + " TEXT, " + KEY_BASE_GOAL + " TEXT, "
            + KEY_ACTUAL_GOAL + " TEXT, " + KEY_STEPS + " TEXT);";

    public static final String CREATE_TABLE_STEPS_SQL = "CREATE TABLE " + TABLE_STEPS + " (" +
            KEY_ID + " INTEGER PRIMARY KEY, " + KEY_DAY + " TEXT, " + KEY_HOUR + " TEXT, "
            + KEY_TIMESTAMP + " TEXT);";

    // Constructor
    public UnsteppableOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // onCreate
    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE_STEPS_SQL);
        db.execSQL(CREATE_TABLE_DASHBOARD_SQL);
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
        long id = database.insert(UnsteppableOpenHelper.TABLE_STEPS, null, values);
        Log.v(TAG, "insertSingleStep() - Id: " + id);
    }

    public static void insertDayReport(Context context, Integer baseGoal, Integer actualGoal){
        long id = 0;
        String lastDayInDashboard = UnsteppableOpenHelper.getLastDayFromDashboard(context);
        Log.d(TAG, "value of lastDayInDashboard: " + lastDayInDashboard);
        String lastDayInNumSteps = UnsteppableOpenHelper.getLastDayFromNumSteps(context);
        Log.d(TAG, "value of lastDayInNumSteps: " + lastDayInNumSteps);
        String[] currentDayAndDate = getCurrentDayAndDate();
        if(lastDayInNumSteps != null && !currentDayAndDate[0].equals(lastDayInNumSteps)){
            ContentValues values = new ContentValues();
            values.put(UnsteppableOpenHelper.KEY_DAY, lastDayInNumSteps);
            Integer androidStepCounter = UnsteppableOpenHelper.getStepsByDayFromTab1(context, lastDayInNumSteps);
            values.put(UnsteppableOpenHelper.KEY_STEPS, androidStepCounter);
            SQLiteDatabase database = getDatabase(context);
            if(lastDayInNumSteps.equals(lastDayInDashboard)){
                id = database.update(UnsteppableOpenHelper.TABLE_DASHBOARD, values, "day = ?", new String[]{lastDayInDashboard});
                Log.v("DATABASE", "Update row in Dashboard - LONG " + id);
            }else{
                values.put(UnsteppableOpenHelper.KEY_BASE_GOAL , baseGoal);
                values.put(UnsteppableOpenHelper.KEY_ACTUAL_GOAL , actualGoal);
                id = database.insert(UnsteppableOpenHelper.TABLE_DASHBOARD, null, values);
                Log.v("DATABASE", "Insert row in Dashboard - LONG " + id);
            }
            database.delete(UnsteppableOpenHelper.TABLE_STEPS,"day = ?",new String[]{lastDayInNumSteps});
        }

        /*if(day == null || timestamp == null){
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
        Log.v(TAG, "Update row - LONG " + id);*/

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

        Cursor cursor = database.query(UnsteppableOpenHelper.TABLE_STEPS, null, where, whereArgs, null,
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

    public static String getLastDayFromDashboard(Context context){
        String day = null;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.rawQuery("SELECT * FROM " +UnsteppableOpenHelper.TABLE_DASHBOARD + " ORDER BY id DESC LIMIT 1;", null);
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            day = cursor.getString(cursor.getColumnIndex("day"));
            cursor.moveToNext();
        }
        database.close();
        return day;
    }

    public static String getLastDayFromNumSteps(Context context){
        String day = null;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.rawQuery("SELECT * FROM " +UnsteppableOpenHelper.TABLE_STEPS + " ORDER BY id DESC LIMIT 1;", null);
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
