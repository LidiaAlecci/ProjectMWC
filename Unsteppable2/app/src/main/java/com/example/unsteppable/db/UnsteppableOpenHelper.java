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

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class UnsteppableOpenHelper extends SQLiteOpenHelper {
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void insertSomeResult(SQLiteDatabase db) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_1 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_2 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_3 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_4 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        ContentValues values = new ContentValues();
        // values in day_m_1
        values.put(UnsteppableOpenHelper.KEY_DAY, day_m_1);
        values.put(UnsteppableOpenHelper.KEY_BASE_GOAL , 1000);
        values.put(UnsteppableOpenHelper.KEY_ACTUAL_GOAL , 1000);
        values.put(UnsteppableOpenHelper.KEY_STEPS, 1527);
        values.put(UnsteppableOpenHelper.KEY_REACHED, 1);
        Long id = db.insert(UnsteppableOpenHelper.TABLE_DASHBOARD, null, values);
        Log.v("DATABASE", "Insert row in Dashboard - LONG " + id);
        // values in day_m_2
        values.remove(UnsteppableOpenHelper.KEY_DAY);
        values.remove(UnsteppableOpenHelper.KEY_STEPS);
        values.put(UnsteppableOpenHelper.KEY_DAY, day_m_2);
        values.put(UnsteppableOpenHelper.KEY_STEPS, 1111);
        id = db.insert(UnsteppableOpenHelper.TABLE_DASHBOARD, null, values);
        Log.v("DATABASE", "Insert row in Dashboard - LONG " + id);
        // values in day_m_3
        values.remove(UnsteppableOpenHelper.KEY_DAY);
        values.remove(UnsteppableOpenHelper.KEY_STEPS);
        values.remove(KEY_REACHED);
        values.put(UnsteppableOpenHelper.KEY_DAY, day_m_3);
        values.put(UnsteppableOpenHelper.KEY_STEPS, 2000);
        values.put(UnsteppableOpenHelper.KEY_REACHED, 0);
        id = db.insert(UnsteppableOpenHelper.TABLE_DASHBOARD, null, values);
        Log.v("DATABASE", "Insert row in Dashboard - LONG " + id);
        // values in day_m_4
        values.remove(UnsteppableOpenHelper.KEY_DAY);
        values.remove(UnsteppableOpenHelper.KEY_STEPS);
        values.remove(KEY_REACHED);
        values.put(UnsteppableOpenHelper.KEY_DAY, day_m_4);
        values.put(UnsteppableOpenHelper.KEY_STEPS, 700);
        values.put(UnsteppableOpenHelper.KEY_REACHED, 1);
        id = db.insert(UnsteppableOpenHelper.TABLE_DASHBOARD, null, values);
        Log.v("DATABASE", "Insert row in Dashboard - LONG " + id);
        // Insert some badges
        values.clear();
        values.put(UnsteppableOpenHelper.KEY_TIMESTAMP, "");
        values.put(UnsteppableOpenHelper.KEY_DAY, day_m_1);
        values.put(UnsteppableOpenHelper.KEY_HOUR, "20");
        values.put(UnsteppableOpenHelper.KEY_TYPE, "1");
        values.put(UnsteppableOpenHelper.KEY_NAME, "Daily goal reached!");
        values.put(UnsteppableOpenHelper.KEY_DESCRIPTION, "You reach your daily goal");
        id = db.insert(UnsteppableOpenHelper.TABLE_BADGES, null, values);
        Log.v(TAG, "insertBadges() - Id: " + id);

        values.remove(UnsteppableOpenHelper.KEY_DAY);
        values.remove(UnsteppableOpenHelper.KEY_HOUR);
        values.put(UnsteppableOpenHelper.KEY_DAY, day_m_2);
        values.put(UnsteppableOpenHelper.KEY_HOUR, "17");
        id = db.insert(UnsteppableOpenHelper.TABLE_BADGES, null, values);
        Log.v(TAG, "insertBadges() - Id: " + id);

        values.remove(UnsteppableOpenHelper.KEY_DAY);
        values.remove(UnsteppableOpenHelper.KEY_HOUR);
        values.put(UnsteppableOpenHelper.KEY_DAY, day_m_3);
        values.put(UnsteppableOpenHelper.KEY_HOUR, "23");
        id = db.insert(UnsteppableOpenHelper.TABLE_BADGES, null, values);
        Log.v(TAG, "insertBadges() - Id: " + id);

        values.remove(KEY_DESCRIPTION);
        values.remove(KEY_NAME);
        values.remove(KEY_DAY);
        values.remove(KEY_TYPE);
        values.put(KEY_DAY, day_m_4);
        values.put(KEY_TYPE, "3");
        values.put(KEY_NAME, "Daily goal reached in all days in the previous week, well done!");
        values.put(KEY_DESCRIPTION, "You reach your daily goal for all days in the previous week, ad maiora semper!");
        id = db.insert(UnsteppableOpenHelper.TABLE_BADGES, null, values);
        Log.v(TAG, "insertBadges() - Id: " + id);

        values.remove(KEY_DESCRIPTION);
        values.remove(KEY_NAME);
        values.remove(KEY_TYPE);
        values.put(KEY_TYPE, "3");
        values.put(KEY_NAME, "Daily goal reached 3 days in a row!");
        values.put(KEY_DESCRIPTION, "You reach your daily goal in the last three days, keep going!");
        id = db.insert(UnsteppableOpenHelper.TABLE_BADGES, null, values);
        Log.v(TAG, "insertBadges() - Id: " + id);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing to do here
    }

    public static void insertBadges(Context context, String timestamp, String day, String hour, String type, String name, String description){
        ContentValues values = new ContentValues();
        values.put(UnsteppableOpenHelper.KEY_TIMESTAMP, timestamp);
        values.put(UnsteppableOpenHelper.KEY_DAY, day);
        values.put(UnsteppableOpenHelper.KEY_HOUR, hour);
        values.put(UnsteppableOpenHelper.KEY_TYPE, type);
        values.put(UnsteppableOpenHelper.KEY_NAME, name);
        values.put(UnsteppableOpenHelper.KEY_DESCRIPTION, description);
        SQLiteDatabase database = getDatabase(context);
        long id = database.insert(UnsteppableOpenHelper.TABLE_BADGES, null, values);
        Log.v(TAG, "insertBadges() - Id: " + id);
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
            int reached = 0;
            if(androidStepCounter > actualGoal){
                reached = 1;
            }
            values.put(UnsteppableOpenHelper.KEY_REACHED, reached);
            values.put(UnsteppableOpenHelper.KEY_BASE_GOAL , baseGoal);
            values.put(UnsteppableOpenHelper.KEY_ACTUAL_GOAL , actualGoal);
            SQLiteDatabase database = getDatabase(context);
            if(lastDayInNumSteps.equals(lastDayInDashboard)){
                id = database.update(UnsteppableOpenHelper.TABLE_DASHBOARD, values, "day = ?", new String[]{lastDayInDashboard});
                Log.v("DATABASE", "Update row in Dashboard - LONG " + id);
            }else{
                id = database.insert(UnsteppableOpenHelper.TABLE_DASHBOARD, null, values);
                Log.v("DATABASE", "Insert row in Dashboard - LONG " + id);
            }
            database.delete(UnsteppableOpenHelper.TABLE_STEPS,"day = ?",new String[]{lastDayInNumSteps});
        }

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


    public static Integer getStepsFromDashboardByDate(Context context, String date){
        String where = UnsteppableOpenHelper.KEY_DAY + " = ?";
        String [] whereArgs = { date };
        Integer steps = 0;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.query(UnsteppableOpenHelper.TABLE_DASHBOARD, null, where, whereArgs, null,
                null, null );
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            steps = cursor.getInt(cursor.getColumnIndex("steps"));
            cursor.moveToNext();
        }
        database.close();
        return steps;
    }

    public static Integer getBaseGoalByDate(Context context, String date){
        String where = UnsteppableOpenHelper.KEY_DAY + " = ?";
        String [] whereArgs = { date };
        Integer baseGoal = 0;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.query(UnsteppableOpenHelper.TABLE_DASHBOARD, null, where, whereArgs, null,
                null, null );
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            baseGoal = cursor.getInt(cursor.getColumnIndex("baseGoal"));
            cursor.moveToNext();
        }
        database.close();
        return baseGoal;
    }

    public static Integer getActualGoalByDate(Context context, String date){
        String where = UnsteppableOpenHelper.KEY_DAY + " = ?";
        String [] whereArgs = { date };
        Integer actualGoal = 0;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.query(UnsteppableOpenHelper.TABLE_DASHBOARD, null, where, whereArgs, null,
                null, null );
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            actualGoal = cursor.getInt(cursor.getColumnIndex("actualGoal"));
            cursor.moveToNext();
        }
        database.close();
        return actualGoal;
    }

    public static boolean getReachedByDate(Context context, String date){
        String where = UnsteppableOpenHelper.KEY_DAY + " = ?";
        String [] whereArgs = { date };
        Integer reached = 0;
        // Get the readable database
        SQLiteDatabase database = getDatabase(context);
        Cursor cursor = database.query(UnsteppableOpenHelper.TABLE_DASHBOARD, null, where, whereArgs, null,
                null, null );
        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            reached = cursor.getInt(cursor.getColumnIndex("reached"));
            cursor.moveToNext();
        }
        database.close();
        if(reached == 1){
            return true;
        }
        return false;
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
    // utility to have day in the right format from timeMillis
    public static String getDay(long timeMillis){
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        jdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        String currentDate = jdf.format(timeMillis);
        // Get the date, the day and the hour
        String currentDay = currentDate.substring(0,10);
        return currentDay;
    }

}
