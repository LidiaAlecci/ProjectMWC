package com.example.unsteppable.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class UnsteppableDayOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "unsteppable";

    public static final String TABLE_NAME = "num_steps";
    public static final String KEY_ID = "id";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_HOUR = "hour";
    public static final String KEY_DAY = "day";

    // Default SQL for creating a table in a database
    public static final String CREATE_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " (" +
            KEY_ID + " INTEGER PRIMARY KEY, " + KEY_DAY + " TEXT, " + KEY_HOUR + " TEXT, "
            + KEY_TIMESTAMP + " TEXT);";

    // Constructor
    public UnsteppableDayOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // onCreate
    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE_SQL);
        Log.v("DATABASE", "created days table");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing to do here
    }

    /**
     * Utility function to load all records in the database
     *
     * @param context: application context

    public static void loadRecords(Context context) {
        List<String> dates = new LinkedList<String>();
        UnsteppableDayOpenHelper databaseHelper = new UnsteppableDayOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String [] columns = new String [] {UnsteppableDayOpenHelper.KEY_TIMESTAMP};
        Cursor cursor = database.query(UnsteppableDayOpenHelper.TABLE_NAME, columns,
                null, null, UnsteppableDayOpenHelper.KEY_TIMESTAMP,
                null, null );

        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            dates.add(cursor.getString(0));
            cursor.moveToNext();
        }
        database.close();

        Log.d("STORED TIMESTAMPS: ", String.valueOf(dates));

    }*/

    /**
     * Utility function to delete all records from the data base
     *
     * @param context: application context

    public static void deleteRecords(Context context){
        UnsteppableDayOpenHelper databaseHelper = new UnsteppableDayOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        int numberDeletedRecords =0;

        numberDeletedRecords = database.delete(UnsteppableDayOpenHelper.TABLE_NAME, null, null);
        database.close();

        // display the number of deleted records with a Toast message
        Toast.makeText(context,"Deleted " + String.valueOf(numberDeletedRecords) + " records",Toast.LENGTH_LONG).show();
    }*/

    /**
     * Utility function to load records from a single day
     *
     * @param context: application context
     * @param date: today's date
     * @return numSteps: an integer value with the number of records in the database
     */
    //
    public static Integer loadSingleRecord(Context context, String date){
        List<String> steps = new LinkedList<String>();
        // Get the readable database
        UnsteppableDayOpenHelper databaseHelper = new UnsteppableDayOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String where = UnsteppableDayOpenHelper.KEY_DAY + " = ?";
        String [] whereArgs = { date };

        Cursor cursor = database.query(UnsteppableDayOpenHelper.TABLE_NAME, null, where, whereArgs, null,
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

    /**
     * Utility function to get the number of steps by hour for current date
     *
     * @param context: application context
     * @param date: today's date
     * @return map: map with key-value pairs hour->number of steps
     */
    //
    public static Map<Integer, Integer> loadStepsByHour(Context context, String date){
        // 1. Define a map to store the hour and number of steps as key-value pairs
        Map<Integer, Integer>  map = new HashMap<>();

        // 2. Get the readable database
        UnsteppableDayOpenHelper databaseHelper = new UnsteppableDayOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        // 3. Define the query to get the data
        Cursor cursor = database.rawQuery("SELECT hour, COUNT(*)  FROM num_steps " +
                "WHERE day = ? GROUP BY hour ORDER BY  hour ASC ", new String [] {date});

        // 4. Iterate over returned elements on the cursor
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            Integer tmpKey = Integer.parseInt(cursor.getString(0));
            Integer tmpValue = Integer.parseInt(cursor.getString(1));

            //2. Put the data from the database into the map
            map.put(tmpKey, tmpValue);


            cursor.moveToNext();
        }

        // 5. Close the cursor and database
        cursor.close();
        database.close();

        // 6. Return the map with hours and number of steps
        return map;
    }

    /**
     * Utility function to get the number of steps by day
     *
     * @param context: application context
     * @return map: map with key-value pairs hour->number of steps
     */
    //
    public static Map<String, Integer> loadStepsByDay(Context context){
        // 1. Define a map to store the hour and number of steps as key-value pairs
        Map<String, Integer>  map = new TreeMap<>();

        // 2. Get the readable database
        UnsteppableDayOpenHelper databaseHelper = new UnsteppableDayOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        // 3. Define the query to get the data
        Cursor cursor = database.rawQuery("SELECT day, COUNT(*)  FROM num_steps " +
                "GROUP BY day ORDER BY day ASC ", new String [] {});

        // 4. Iterate over returned elements on the cursor
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            String tmpKey = cursor.getString(0);
            Integer tmpValue = Integer.parseInt(cursor.getString(1));

            // Put the data from the database into the map
            map.put(tmpKey, tmpValue);
            cursor.moveToNext();
        }

        // 5. Close the cursor and database
        cursor.close();
        database.close();

        // 6. Return the map with hours and number of steps
        return map;
    }
}
