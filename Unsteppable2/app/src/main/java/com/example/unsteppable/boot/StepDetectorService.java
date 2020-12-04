package com.example.unsteppable.boot;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;

import com.example.unsteppable.R;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import static com.example.unsteppable.MainActivity.CHANNEL_ID;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.unsteppable.MainActivity;
import com.example.unsteppable.db.UnsteppableOpenHelper;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class StepDetectorService extends Service implements SensorEventListener {
    SensorManager sensorManager;
    Sensor sensorStepDetector;
    private static final String TAG = "STEP_SERVICE";
    private final Handler handler = new Handler();
    private Notification notification = null;
    int appIcon = R.drawable.ic_launcher_foreground;
    // Android step counter
    public int androidSteps = 0;
    public int baseGoal = 6000;
    public int actualGoal = 6000;
    boolean serviceStopped; // Boolean variable to control if the service is stopped

    PendingIntent pendingIntent;
    AlarmManager alarmManager;
    BroadcastReceiver broadcastAlarmReceiver;
    Calendar calendar;

    // SQLite Database
    SQLiteDatabase database;

    public String timestamp;
    public String day;
    public String hour;


    Intent intent;
    public static final String BROADCAST_ACTION = "com.example.unsteppable.mybroadcast";
    public static final String BROADCAST_ACTION_ALARM = "com.example.unsteppable.alarm";

    /** Called when the service is being created. */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
        //Register AlarmManager Broadcast receive.
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0); // alarm hour
        calendar.set(Calendar.MINUTE, 0); // alarm minute
        calendar.set(Calendar.SECOND, 0); // alarm second
        Log.v("ALARM Broadcast", "calendar: " + String.valueOf(calendar.getTime()));
        long intendedTime = calendar.getTimeInMillis();
        registerAlarmBroadcast();
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, intendedTime, 24*60*60*1000, pendingIntent);
        // get, if any, the steps already register in the db
        androidSteps = UnsteppableOpenHelper.getStepsByDayFromTab1(getBaseContext(),getCurrentDay());
        createNotification(androidSteps);
        UnsteppableOpenHelper.insertDayReport(getBaseContext(),baseGoal,actualGoal);
    }

    // utility to have current day and the timestamp in the right format
    private String getCurrentDay(){
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        jdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        String currentDate = jdf.format(System.currentTimeMillis());
        // Get the date, the day and the hour
        String currentDay = currentDate.substring(0,10);
        return currentDay;
    }

    private void registerAlarmBroadcast() {
        Log.i(TAG, "Going to register Intent.RegisterAlarmBroadcast");

        //This will be call when alarm time will reached.
        broadcastAlarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG,"BroadcastReceiver::OnReceive()");
                // Insert the data in the database
                UnsteppableOpenHelper.insertDayReport(context, baseGoal, actualGoal);
                if(!getCurrentDay().equals(day)){
                    restart();
                }
            }
        };

        // register the receiver
        registerReceiver(broadcastAlarmReceiver, new IntentFilter(BROADCAST_ACTION_ALARM) );
        // create a pending intent that will be launched
        pendingIntent = PendingIntent.getBroadcast( this, 0, new Intent(BROADCAST_ACTION_ALARM),0 );
        alarmManager = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
    }
    private void unregisterAlarmBroadcast() {
        alarmManager.cancel(pendingIntent);
        getBaseContext().unregisterReceiver(broadcastAlarmReceiver);
    }

    private void restart() {
        androidSteps = 0;
    }


    public void createNotification(int steps){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntentNotification = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        //Log.v(TAG, "createNotification with " + steps + " steps");


        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Unsteppable is running")
                .setContentText(steps + " steps done")
                .setSmallIcon(appIcon)
                .setNotificationSilent()
                .setContentIntent(pendingIntentNotification)
                .build();

        startForeground(1,notification);
    }

    /** Called by startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Start");

        // Get an instance of the databases
        UnsteppableOpenHelper databaseOpenHelper = new UnsteppableOpenHelper(getApplicationContext());
        database = databaseOpenHelper.getWritableDatabase();

        // Initialize Handler
        // remove any existing callbacks to the handler
        handler.removeCallbacks(updateBroadcastData);
        // call handler
        handler.post(updateBroadcastData);

        sensorManager = (SensorManager) getSystemService(getApplicationContext().SENSOR_SERVICE);
        sensorStepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (sensorStepDetector != null) {
            sensorManager.registerListener(this, sensorStepDetector, SensorManager.SENSOR_DELAY_NORMAL);
        }else {
            handler.post(new ToastRunnable(R.string.step_not_available));
        }
        serviceStopped = false;
        if(notification == null){
            createNotification(0);
        }
        return START_STICKY;
    }

    // Required method
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Required method: Sensor Event
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_STEP_DETECTOR:
                // Calculate the number of steps
                androidSteps += 1;
                Log.v(TAG, "Num.steps: " + String.valueOf(androidSteps));
                if(androidSteps != 0) { // It's not the initialize phase
                    // Timestamp
                    long timeInMillis = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000;
                    updateTimeStamp(timeInMillis);
                    // Insert the data in the database
                    UnsteppableOpenHelper.insertSingleStep(getBaseContext(), timestamp, day, hour);
                }
                createNotification(androidSteps);
        }
    }

    private void updateTimeStamp(long timeInMillis){
        // Convert the timestamp to date
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        jdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));
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
        //UnsteppableOpenHelper.insertDayReport(getBaseContext(), baseGoal, actualGoal);
        Log.v(TAG, "Stop");
        unregisterAlarmBroadcast();
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
        intent.putExtra("Counted_Steps_Int", androidSteps);
        intent.putExtra("Counted_Steps", String.valueOf(androidSteps));
        //intent.putExtra("Goal_Steps_Int", );
        // call sendBroadcast with the intent: sends a message to whoever is registered
        sendBroadcast(intent);
    }

    // updateDatabase updates the Database at 23:59
    /*
    private Runnable updateDatabase = new Runnable() {
        public void run() {
            if (!serviceStopped) { // If service is still running keep it updated
                //TO DO
                // Update second database

                // After the delay this Runnable will be executed again
                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(60));
            }
        }
    };*/
}
