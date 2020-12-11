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
import android.content.SharedPreferences;
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
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;

import com.example.unsteppable.MainActivity;
import com.example.unsteppable.SettingsActivity;
import com.example.unsteppable.db.UnsteppableOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class StepDetectorService extends Service implements SensorEventListener {
    SensorManager sensorManager;
    Sensor sensorStepDetector;
    private static final String TAG = "STEP_SERVICE";
    private UnsteppableOpenHelper databaseOpenHelper = null;
    private final Handler handler = new Handler();
    private Notification notification = null;
    int appIcon = R.drawable.ic_launcher_foreground;
    int badgeIcon = R.drawable.ic_launcher_foreground;
    // Android step counter
    public int androidSteps = 0;
    public int baseGoal = 200;
    public int actualGoal = 200;
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
        databaseOpenHelper = UnsteppableOpenHelper.getInstance(getBaseContext());
        day = getCurrentDay();
        // get, if any, the steps already register in the db
        String currentDay = getCurrentDay();
        androidSteps = UnsteppableOpenHelper.getStepsByDayFromTab1(getBaseContext(),currentDay);
        int baseGoalDB = UnsteppableOpenHelper.getBaseGoalByDate(getBaseContext(),currentDay);
        int actualGoalDB =  UnsteppableOpenHelper.getActualGoalByDate(getBaseContext(),currentDay);
        if(actualGoalDB != 0){
            actualGoal = actualGoalDB;
        }
        if(baseGoalDB == 0){
            baseGoal = baseGoalDB;
        }
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
        createNotification(androidSteps);
        UnsteppableOpenHelper.insertDayReport(getBaseContext(), baseGoal, actualGoal);
        broadcastSensorValue();
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
        createNotification(0);
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

        // Get an instance of the database
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
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_STEP_DETECTOR:
                // Calculate the number of steps
                androidSteps += 1;
                Log.v(TAG, "Num.steps: " + String.valueOf(androidSteps));
                /*
                if(androidSteps != 0) { // It's not the initialize phase
                    // Timestamp

                }*/
                long timeInMillis = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000;
                updateTimeStamp(timeInMillis);
                // Insert the data in the database
                UnsteppableOpenHelper.insertSingleStep(getBaseContext(), timestamp, day, hour);
                createNotification(androidSteps);
                checkGoal();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkGoal(){
        if (androidSteps == actualGoal){
            // Create an explicit intent for an Activity in your app
            Intent notificationIntent = new Intent(this, SettingsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntentNotification = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(badgeIcon)
                    .setContentTitle("Congrats! You reach your daily goal!")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    // Set the intent that will fire when the user taps the notification
                    .setContentIntent(pendingIntentNotification)
                    .setAutoCancel(true);
            Calendar calendar = Calendar.getInstance();
            int hour24hrs = calendar.get(Calendar.HOUR_OF_DAY);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            Log.v(TAG, "Calendar hour of day = "+ hour24hrs);
            if(hour24hrs > 17){
                builder.setContentText("This day is almost over, but why not try to push yourself further?")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("This day is almost over, but why not try to push yourself further?"));
            }else{
                builder.setContentText("There is still enough time to break your records, change your daily goal!\n" +
                        "“When you push yourself beyond limits, you discover inner reserves, which you never thought existed earlier.” - Manoj Arora, Dream On ")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("There is still enough time to break your records, change your daily goal!\n" +
                                        "“When you push yourself beyond limits, you discover inner reserves, which you never thought existed earlier.” - Manoj Arora, Dream On "));
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(3, builder.build());
            UnsteppableOpenHelper.insertBadges(getBaseContext(), timestamp, day, hour,"1", "Daily goal reached!", "You reach your daily goal");
            checkBadge3days();
            if(dayOfWeek == Calendar.SUNDAY){
                checkBadgeWeek();
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkBadge3days(){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_1 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_2 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        boolean day_m_1_reached = UnsteppableOpenHelper.getReachedByDate(getBaseContext(),day_m_1),
                day_m_2_reached = UnsteppableOpenHelper.getReachedByDate(getBaseContext(),day_m_2);
        if(day_m_1_reached && day_m_2_reached){
            UnsteppableOpenHelper.insertBadges(getBaseContext(), timestamp, day, hour,"2", "Daily goal reached 3 days in a row!", "You reach your daily goal in the last three days, keep going!");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkBadgeWeek(){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_1 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_2 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_3 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_4 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_5 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String day_m_6 = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
        boolean day_m_1_reached = UnsteppableOpenHelper.getReachedByDate(getBaseContext(),day_m_1),
                day_m_2_reached = UnsteppableOpenHelper.getReachedByDate(getBaseContext(),day_m_2),
                day_m_3_reached = UnsteppableOpenHelper.getReachedByDate(getBaseContext(),day_m_3),
                day_m_4_reached = UnsteppableOpenHelper.getReachedByDate(getBaseContext(),day_m_4),
                day_m_5_reached = UnsteppableOpenHelper.getReachedByDate(getBaseContext(),day_m_5),
                day_m_6_reached = UnsteppableOpenHelper.getReachedByDate(getBaseContext(),day_m_6);
        if(day_m_1_reached && day_m_2_reached && day_m_3_reached && day_m_4_reached && day_m_5_reached && day_m_6_reached){
            UnsteppableOpenHelper.insertBadges(getBaseContext(), timestamp, day, hour,"3", "Daily goal reached in all days in the previous week, well done!", "You reach your daily goal for all days in the previous week, ad maiora semper!");
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        baseGoal = Integer. parseInt(preferences.getString(getApplicationContext().getResources().getString(R.string.base_goal), String.valueOf(baseGoal)));
        actualGoal = baseGoal;//TODO
        //Log.v(TAG, "Data to Activity");
        // add data to intent
        intent.putExtra("Counted_Steps_Int", androidSteps);
        intent.putExtra("Counted_Steps", String.valueOf(androidSteps));
        intent.putExtra("Base_Goal_Int", baseGoal);
        intent.putExtra("Actual_Goal_Int", actualGoal);
        // call sendBroadcast with the intent: sends a message to whoever is registered
        sendBroadcast(intent);
    }

    // updateWeather updates the Weather every x hours
    /*
    private Runnable updateDatabase = new Runnable() {
        public void run() {
            if (!serviceStopped) { // If service is still running keep it updated
                //TO DO
                // Update weather

                // After the delay this Runnable will be executed again
                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(x*60));
            }
        }
    };*/
}
