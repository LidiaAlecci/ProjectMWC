package com.example.unsteppable;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import static com.example.unsteppable.MainActivity.CHANNEL_ID;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class StepCountService extends Service implements SensorEventListener {
    SensorManager sensorManager;
   // Sensor stepCounterSensor;
    Sensor sensorStepCounter;
    private static final String TAG = "STEP_SERVICE";
    private final Handler handler = new Handler();
    private Notification notification = null;
    // Android step counter
    public int androidStepCounter = 0;
    public int oldSteps = 0;
    boolean serviceStopped; // Boolean variable to control if the service is stopped

    // SQLite Database
    SQLiteDatabase database;

    public String timestamp;
    public String day;
    public String hour;


    Intent intent;
    public static final String BROADCAST_ACTION = "com.example.unsteppable.mybroadcast";

    /** Called when the service is being created. */
    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
    }
    // TODO
    /*
    public void setDatabase(SQLiteDatabase db){
        database = db;
    }*/


    public void createNotification(int steps){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Unsteppable is running")
                .setContentText(steps + " steps done")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1,notification);
    }

    /** Called by startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Start");
        // Initialize Handler
        // remove any existing callbacks to the handler
        handler.removeCallbacks(updateBroadcastData);
        // call handler
        handler.post(updateBroadcastData);

        sensorManager = (SensorManager) getSystemService(getApplicationContext().SENSOR_SERVICE);
        sensorStepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensorStepCounter != null) {
            sensorManager.registerListener(this, sensorStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        }else {
            //handler.post(new ToastRunnable(R.string.step_not_available));
        }
        serviceStopped = false;
        if(notification == null){
            createNotification(0);
        }

        // Get an instance of the database
        UnsteppableOpenHelper databaseOpenHelper = new UnsteppableOpenHelper(getApplicationContext());
        database = databaseOpenHelper.getWritableDatabase();

        return START_STICKY;
    }

    // Required method
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Required method: Sensor Event
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_STEP_COUNTER:
                // Calculate the number of steps
                int countSteps = (int) event.values[0];
                if (oldSteps == 0) {
                    oldSteps = (int) event.values[0];
                }
                //androidStepCounter += (int) event.values[0];
                androidStepCounter = countSteps - oldSteps;
                Log.v(TAG, "Num.steps: " + String.valueOf(androidStepCounter));
                // Timestamp
                long timeInMillis = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000;
                updateTimeStamp(timeInMillis);
                // Insert the data in the database
                ContentValues values = new ContentValues();
                values.put(UnsteppableOpenHelper.KEY_TIMESTAMP, timestamp);
                values.put(UnsteppableOpenHelper.KEY_DAY, day);
                values.put(UnsteppableOpenHelper.KEY_HOUR, hour);
                long id = database.insert(UnsteppableOpenHelper.TABLE_NAME, null, values);
                Log.v("DATABASE TRY", "LONG " + id);
                createNotification(androidStepCounter);
        }
    }

    private void updateTimeStamp(long timeInMillis){
        // Convert the timestamp to date
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        jdf.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        String date = jdf.format(timeInMillis);
        // Get the date, the day and the hour
        timestamp = date;
        day = date.substring(0,10);
        hour = date.substring(11,13);
    }

    // Required method
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    /** Called when the service is no longer used and is being destroyed*/
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Stop");

        serviceStopped = true;
    }
    /** ToastRunnable in order to make toast message */
    private class ToastRunnable implements Runnable {
        int idText;
        public ToastRunnable(int idText) {
            this.idText = idText;
        }
        @Override
        public void run(){
            Toast.makeText(getApplicationContext(), idText, Toast.LENGTH_SHORT).show();
        }
    }

    // updateBroadcastData in order to choose the delay and not update every time
    private Runnable updateBroadcastData = new Runnable() {
        public void run() {
            if (!serviceStopped) { // If service is still running keep it updated
                broadcastSensorValue();
                // After the delay this Runnable will be executed again
                handler.postDelayed(this, 1000);
            }
        }
    };

    /** Add data to the intent and send broadcast */
    private void broadcastSensorValue() {
        //Log.v(TAG, "Data to Activity");
        // add data to intent
        intent.putExtra("Counted_Steps_Int", androidStepCounter);
        intent.putExtra("Counted_Steps", String.valueOf(androidStepCounter));
        //intent.putExtra("Goal_Steps_Int", );
        // call sendBroadcast with the intent: sends a message to whoever is registered
        sendBroadcast(intent);
    }

    // updateDatabase in order to update the Database every hour
    private Runnable updateDatabase = new Runnable() {
        public void run() {
            if (!serviceStopped) { // If service is still running keep it updated
                //TODO
                // Update second database

                // After the delay this Runnable will be executed again
                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(60));
            }
        }
    };
}
