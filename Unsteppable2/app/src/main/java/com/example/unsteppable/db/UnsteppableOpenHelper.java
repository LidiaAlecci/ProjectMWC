package com.example.unsteppable.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.icu.util.Calendar;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.unsteppable.boot.AppState;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class UnsteppableOpenHelper extends SQLiteOpenHelper {
    // code SQL similar to what we have done in the tutorials

    private static UnsteppableOpenHelper instance = null;
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "unsteppable";
    public static final String TAG = "DATABASE";

    public static final String TABLE_STEPS = "steps";
    public static final String TABLE_DASHBOARD = "dashboard";
    public static final String TABLE_BADGES = "badges";
    public static final String KEY_ID = "id";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_BASE_GOAL = "baseGoal";
    public static final String KEY_ACTUAL_GOAL = "actualGoal";
    public static final String KEY_TYPE = "type";
    public static final String KEY_NAME = "name";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_STEPS = "steps";
    public static final String KEY_DAY = "day";
    public static final String KEY_REACHED = "goalReached";
    public static final String KEY_HOUR = "hour";

    // Default SQL for creating a table in a database

    public static final String CREATE_TABLE_DASHBOARD_SQL = "CREATE TABLE " + TABLE_DASHBOARD + " (" +
            KEY_ID + " INTEGER PRIMARY KEY, " + KEY_DAY + " TEXT, " + KEY_BASE_GOAL + " TEXT, "
            + KEY_ACTUAL_GOAL + " TEXT, " + KEY_REACHED + " INTEGER DEFAULT 0, " + KEY_STEPS + " TEXT);";

    public static final String CREATE_TABLE_STEPS_SQL = "CREATE TABLE " + TABLE_STEPS + " (" +
            KEY_ID + " INTEGER PRIMARY KEY, " + KEY_DAY + " TEXT, " + KEY_HOUR + " TEXT, "
            + KEY_TIMESTAMP + " TEXT);";

    public static final String CREATE_TABLE_BADGES_SQL = "CREATE TABLE " + TABLE_BADGES + " (" +
            KEY_ID + " INTEGER PRIMARY KEY, " + KEY_DAY + " TEXT, "  + KEY_HOUR + " TEXT, "
            + KEY_TYPE + " TEXT, " + KEY_NAME + " TEXT, " + KEY_DESCRIPTION + " LONGTEXT, "
            + KEY_TIMESTAMP + " TEXT);";

    // Constructor
    private UnsteppableOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static UnsteppableOpenHelper getInstance(Context context) {
        if (instance == null) {
            instance = new UnsteppableOpenHelper(context);
        }
        return instance;
    }


    // onCreate
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_STEPS_SQL);
        db.execSQL(CREATE_TABLE_DASHBOARD_SQL);
        db.execSQL(CREATE_TABLE_BADGES_SQL);
        insertSomeResult(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing to do here
    }

    public static void insertBadges(Context context, String timestamp, String day, String hour, String type, String name, String description){
        ContentValues values = new ContentValues();
        values.put(KEY_TIMESTAMP, timestamp);
        values.put(KEY_DAY, day);
        values.put(KEY_HOUR, hour);
        values.put(KEY_TYPE, type);
        values.put(KEY_NAME, name);
        values.put(KEY_DESCRIPTION, description);
        SQLiteDatabase database = getDatabase(context);
        long id = database.insert(TABLE_BADGES, null, values);
    }

    public static void insertSingleStep(Context context, String timestamp, String day, String hour){
        ContentValues values = new ContentValues();
        values.put(KEY_TIMESTAMP, timestamp);
        values.put(KEY_DAY, day);
        values.put(KEY_HOUR, hour);
        SQLiteDatabase database = getDatabase(context);
        long id = database.insert(TABLE_STEPS, null, values);
    }

    public static void insertDayReport(Context context, Integer baseGoal, Integer actualGoal){
        long id;
        String lastDayInDashboard = getLastDayFromDashboard(context);
        String lastDayInNumSteps = getLastDayFromNumSteps(context);
        String[] currentDayAndDate = getCurrentDayAndDate();
        if(lastDayInNumSteps != null && !currentDayAndDate[0].equals(lastDayInNumSteps)){
            ContentValues values = new ContentValues();
            values.put(KEY_DAY, lastDayInNumSteps);
            Integer androidStepCounter = getStepsByDayFromTab1(context, lastDayInNumSteps);
            values.put(KEY_STEPS, androidStepCounter);
            int reached = 0;
            if(androidStepCounter > actualGoal){
                reached = 1;
            }
            values.put(KEY_REACHED, reached);
            values.put(KEY_BASE_GOAL , baseGoal);
            values.put(KEY_ACTUAL_GOAL , actualGoal);
            SQLiteDatabase database = getDatabase(context);
            if(lastDayInNumSteps.equals(lastDayInDashboard)){
                id = database.update(TABLE_DASHBOARD, values, KEY_DAY + " = ?", new String[]{lastDayInDashboard});
            }else{
                id = database.insert(TABLE_DASHBOARD, null, values);
            }
            database.delete(TABLE_STEPS,KEY_DAY + " = ?",new String[]{lastDayInNumSteps});
        }

    }

    public static SQLiteDatabase getDatabase(Context context){
        UnsteppableOpenHelper databaseHelper = new UnsteppableOpenHelper(context);
        return databaseHelper.getReadableDatabase();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Map<String, Integer> getStepsLast7Days(Context context){
        Map<String, Integer>  map = new TreeMap<>();
        Calendar calendar = Calendar.getInstance();
        String day = getDay(calendar.getTimeInMillis());
        map.put(day, getStepsByDayFromTab1(context,day));
        for(int i=1; i<7; i++) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            day = getDay(calendar.getTimeInMillis());
            map.put(day, getStepsFromDashboardByDate(context, day));
        }
        return map;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Map<String, Integer> getStepsLast30Days(Context context){
        Map<String, Integer>  map = new TreeMap<>();
        Calendar calendar = Calendar.getInstance();
        String day = getDay(calendar.getTimeInMillis());
        map.put(day, getStepsByDayFromTab1(context,day));
        for(int i=1; i<30; i++) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            day = getDay(calendar.getTimeInMillis());
            map.put(day, getStepsFromDashboardByDate(context, day));
        }
        return map;
    }

    public static Integer getStepsByDayFromTab1(Context context, String date){
        List<String> steps = new LinkedList<>();
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);

        String where = KEY_DAY + " = ?";
        String [] whereArgs = { date };

        Cursor cursor = database.query(TABLE_STEPS, null, where, whereArgs, null,
                null, null );

        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            steps.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        database.close();

        Integer numSteps = steps.size();
        return numSteps;
    }

    public static Integer getStepsFromDashboardByDate(Context context, String date){
        String where = KEY_DAY + " = ?";
        String [] whereArgs = { date };
        int steps = 0;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.query(TABLE_DASHBOARD, null, where, whereArgs, null,
                null, null );
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            steps = cursor.getInt(cursor.getColumnIndex(KEY_STEPS));
            cursor.moveToNext();
        }
        cursor.close();
        database.close();
        return steps;
    }

    public static Integer getBaseGoalByDate(Context context, String date){
        String where = KEY_DAY + " = ?";
        String [] whereArgs = { date };
        int baseGoal = 0;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.query(TABLE_DASHBOARD, null, where, whereArgs, null,
                null, null );
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            baseGoal = cursor.getInt(cursor.getColumnIndex(KEY_BASE_GOAL));
            cursor.moveToNext();
        }
        cursor.close();
        database.close();
        return baseGoal;
    }

    public static Integer getActualGoalByDate(Context context, String date){
        String where = KEY_DAY + " = ?";
        String [] whereArgs = { date };
        int actualGoal = 0;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.query(TABLE_DASHBOARD, null, where, whereArgs, null,
                null, null );
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            actualGoal = cursor.getInt(cursor.getColumnIndex(KEY_ACTUAL_GOAL));
            cursor.moveToNext();
        }
        cursor.close();
        database.close();
        return actualGoal;
    }

    public static boolean getReachedByDate(Context context, String date){
        String where = KEY_DAY + " = ?";
        String [] whereArgs = { date };
        int reached = 0;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.query(TABLE_DASHBOARD, null, where, whereArgs, null,
                null, null );
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            reached = cursor.getInt(cursor.getColumnIndex(KEY_REACHED));
            cursor.moveToNext();
        }
        cursor.close();
        database.close();
        return reached == 1;
    }

    public static String getLastDayFromDashboard(Context context){
        String day = null;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.rawQuery("SELECT * FROM " +TABLE_DASHBOARD + " ORDER BY id DESC LIMIT 1;", null);
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            day = cursor.getString(cursor.getColumnIndex(KEY_DAY));
            cursor.moveToNext();
        }
        cursor.close();
        database.close();
        return day;
    }

    public static ArrayList<Badge> getAllBadges(Context context){
        ArrayList<Badge> badges = new ArrayList<Badge>();
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        String date = "", description="";
        Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_BADGES, null);

        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            date = cursor.getString((cursor.getColumnIndex(KEY_DAY)));
            description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION));
            badges.add(new Badge(date, description));
            cursor.moveToNext();
        }

        cursor.close();
        database.close();
        return badges;

    }

    public static String getLastDayFromNumSteps(Context context){
        String day = null;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.rawQuery("SELECT * FROM " +TABLE_STEPS + " ORDER BY id DESC LIMIT 1;", null);
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            day = cursor.getString(cursor.getColumnIndex(KEY_DAY));
            cursor.moveToNext();
        }
        cursor.close();
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
    // utility to have day in the right format from timeMillis
    public static String getDay(long timeMillis){
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        jdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        String currentDate = jdf.format(timeMillis);
        // Get the date, the day and the hour
        String currentDay = currentDate.substring(0,10);
        return currentDay;
    }
    // Utility to have something in the db in the last 4 days and some badges
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void insertSomeResult(SQLiteDatabase db) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_1 = getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_2 = getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_3 = getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_4 = getDay(calendar.getTimeInMillis());
        ContentValues values = new ContentValues();
        // values in day_m_1
        values.put(KEY_DAY, day_m_1);
        values.put(KEY_BASE_GOAL , 1000);
        values.put(KEY_ACTUAL_GOAL , 1000);
        values.put(KEY_STEPS, 1527);
        values.put(KEY_REACHED, 1);
        db.insert(TABLE_DASHBOARD, null, values);

        // values in day_m_2
        values.remove(KEY_DAY);
        values.remove(KEY_STEPS);
        values.put(KEY_DAY, day_m_2);
        values.put(KEY_STEPS, 1111);
        db.insert(TABLE_DASHBOARD, null, values);

        // values in day_m_3
        values.remove(KEY_DAY);
        values.remove(KEY_STEPS);
        values.remove(KEY_REACHED);
        values.put(KEY_DAY, day_m_3);
        values.put(KEY_STEPS, 200);
        values.put(KEY_REACHED, 0);
        db.insert(TABLE_DASHBOARD, null, values);

        // values in day_m_4
        values.remove(KEY_DAY);
        values.remove(KEY_STEPS);
        values.remove(KEY_REACHED);
        values.put(KEY_DAY, day_m_4);
        values.put(KEY_STEPS, 700);
        values.put(KEY_REACHED, 1);
        db.insert(TABLE_DASHBOARD, null, values);

        // Insert some badges
        values.clear();
        values.put(KEY_TIMESTAMP, "");
        values.put(KEY_DAY, day_m_1);
        values.put(KEY_HOUR, "20");
        values.put(KEY_TYPE, "1");
        values.put(KEY_NAME, "Daily goal reached!");
        values.put(KEY_DESCRIPTION, "You reached your daily goal");
        db.insert(TABLE_BADGES, null, values);

        values.remove(KEY_DAY);
        values.remove(KEY_HOUR);
        values.put(KEY_DAY, day_m_2);
        values.put(KEY_HOUR, "17");
        db.insert(TABLE_BADGES, null, values);

        values.remove(KEY_DAY);
        values.remove(KEY_HOUR);
        values.put(KEY_DAY, day_m_3);
        values.put(KEY_HOUR, "23");
        db.insert(TABLE_BADGES, null, values);

        values.remove(KEY_DESCRIPTION);
        values.remove(KEY_NAME);
        values.remove(KEY_DAY);
        values.remove(KEY_TYPE);
        values.put(KEY_DAY, day_m_4);
        values.put(KEY_TYPE, "3");
        values.put(KEY_NAME, "Daily goal reached in all days in the previous week, well done!");
        values.put(KEY_DESCRIPTION, "You reached your daily goal for a week!");
        db.insert(TABLE_BADGES, null, values);

        values.remove(KEY_DESCRIPTION);
        values.remove(KEY_NAME);
        values.remove(KEY_TYPE);
        values.put(KEY_TYPE, "3");
        values.put(KEY_NAME, "Daily goal reached 3 days in a row!");
        values.put(KEY_DESCRIPTION, "You reached your daily goal for three days, keep going!");
        db.insert(TABLE_BADGES, null, values);
    }

    public static class Badge {
        public String day;
        public String description;

        public Badge(String day, String description){
            this.day = day;
            this.description = description;
        }

        public String getDay(){
            return day;
        }

        public String getDescription(){
            return description;
        }
    }
}
