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
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import static com.example.unsteppable.MainActivity.CHANNEL_ID;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.example.unsteppable.MainActivity;
import com.example.unsteppable.settings.SettingsActivity;
import com.example.unsteppable.db.UnsteppableOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Observable;
import java.util.Observer;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class StepDetectorService extends Service implements SensorEventListener, Observer {
    SensorManager sensorManager;
    Sensor sensorStepDetector;
    private static final String TAG = "STEP_SERVICE";
    private static final String YES_INCREASE_ACTION = "YesI";
    private static final String YES_DECREASE_ACTION = "YesD";
    private static final String NO_ACTION = "No";

    private UnsteppableOpenHelper databaseOpenHelper = null;
    private final Handler handler = new Handler(Looper.myLooper());
    private Notification notification = null;
    int appIcon = R.mipmap.ic_launcher_round;
    int badgeIcon = R.drawable.ic_trophy_notification;
    double p = 0.0;// percent of change of actual goal based on weather and base goal
    String weather_main;
    // Android step counter
    public int androidSteps = 0;
    public int baseGoal = 3000;
    public int actualGoal = 3000;
    boolean serviceStopped; // Boolean variable to control if the service is stopped

    PendingIntent pendingIntent;
    PendingIntent pendingIntent2;
    AlarmManager alarmManager;
    BroadcastReceiver broadcastAlarmReceiver;
    BroadcastReceiver broadcastAlarmReceiver2;
    BroadcastReceiver broadcastResponseReceiver;
    Calendar calendar;

    // SQLite Database
    SQLiteDatabase database;

    public String timestamp;
    public String day;
    public String hour;

    Intent intent;
    public static final String BROADCAST_ACTION = "com.example.unsteppable.mybroadcast";
    public static final String BROADCAST_ACTION_ALARM = "com.example.unsteppable.alarm";
    public static final String BROADCAST_ACTION_ALARM2 = "com.example.unsteppable.alarm2";

    /** Called when the service is being created. */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();
        databaseOpenHelper = UnsteppableOpenHelper.getInstance(getBaseContext());
        day = getCurrentDay();
        hour = String.valueOf(Calendar.getInstance().get(Calendar.HOUR));
        // get, if any, the steps already register in the db
        checkValuesInDb();
        registerBroadcasts();
        createNotification(androidSteps);
        UnsteppableOpenHelper.insertDayReport(getBaseContext(), baseGoal, actualGoal);
        broadcastSensorValue();
        WeatherService.getInstance().register(this);
    }

    private void checkValuesInDb(){
        androidSteps = UnsteppableOpenHelper.getStepsByDayFromTab1(getBaseContext(),day);
        int baseGoalDB = UnsteppableOpenHelper.getBaseGoalByDate(getBaseContext(),day);
        int actualGoalDB =  UnsteppableOpenHelper.getActualGoalByDate(getBaseContext(),day);
        if(actualGoalDB != 0){
            actualGoal = actualGoalDB;
        }
        if(baseGoalDB != 0){
            baseGoal = baseGoalDB;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void registerBroadcasts(){
        intent = new Intent(BROADCAST_ACTION);
        //Register AlarmManager Broadcast receive to save steps at midnight.
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0); // alarm hour
        calendar.set(Calendar.MINUTE, 0); // alarm minute
        calendar.set(Calendar.SECOND, 0); // alarm second
        long intendedTime = calendar.getTimeInMillis();
        registerAlarmBroadcast();
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, intendedTime, 24*60*60*1000, pendingIntent);
        //Register AlarmManager Broadcast receive to save steps at midnight.
        calendar.set(Calendar.HOUR_OF_DAY, 16); // alarm hour
        intendedTime = calendar.getTimeInMillis();
        registerAlarmBroadcast2();
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, intendedTime, 24*60*60*1000, pendingIntent2);
        registerBroadcastResponse();
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

    private void restart() {
        androidSteps = 0;
        createNotification(0);
    }


    public void createNotification(int steps){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntentNotification = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Unsteppable is running")
                .setContentText(steps + " steps")
                .setSmallIcon(appIcon)
                .setNotificationSilent()
                .setContentIntent(pendingIntentNotification)
                .build();

        startForeground(1,notification);
    }

    /** Called by startService() */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Start");

        // Get an instance of the database
        database = databaseOpenHelper.getWritableDatabase();

        // Initialize Handler
        // remove any existing callbacks to the handler
        //handler.removeCallbacks(updateBroadcastData);
        // call handler
        handler.post(updateBroadcastData);
        handler.post(updateWeather);

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

                long timeInMillis = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000;
                updateTimeStamp(timeInMillis);
                // Insert the data in the database
                UnsteppableOpenHelper.insertSingleStep(getBaseContext(), timestamp, day, hour);
                createNotification(androidSteps);
                checkGoal(false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkGoal(boolean actualGoalChanged){
        if (androidSteps == actualGoal || actualGoalChanged){
            // Create an explicit intent for an Activity in your app
            Intent notificationIntent = new Intent(this, SettingsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntentNotification = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(badgeIcon)
                    .setContentTitle("Congrats! You've reachead your daily goal!")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    // Set the intent that will fire when the user taps the notification
                    //.setContentIntent(pendingIntentNotification)
                    .setAutoCancel(true);
            Calendar calendar = Calendar.getInstance();
            int hour24hrs = calendar.get(Calendar.HOUR_OF_DAY);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            Log.v(TAG, "Calendar hour of day = "+ hour24hrs);
            String message;

            int newGoal;
            int notificationId = 3;
            //Yes intent
            Intent yesReceive = new Intent();
            yesReceive.setAction(YES_INCREASE_ACTION);
            yesReceive.putExtra("notificationId", notificationId);
            newGoal = (int) (baseGoal + baseGoal*0.1);
            if(hour24hrs > 17){
                message = "This day is almost over, but why not try to push yourself further?\n Change it to: " + newGoal;

            }else{
                message = "Push your limits! Change your daily goal!\n Change it to: " + newGoal;
            }
            PendingIntent pendingIntentYes = PendingIntent.getBroadcast(this, 0, yesReceive, PendingIntent.FLAG_UPDATE_CURRENT);

            //No intent
            Intent noReceive = new Intent();
            noReceive.setAction(NO_ACTION);
            noReceive.putExtra("notificationId", notificationId);
            PendingIntent pendingIntentNo = PendingIntent.getBroadcast(this, 0, noReceive, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(message))
                    .addAction(0,"Yes", pendingIntentYes)
                    .addAction(0,"No", pendingIntentNo);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(notificationId, builder.build());
            UnsteppableOpenHelper.insertBadges(getBaseContext(), timestamp, day, hour,"1", "Daily goal reached!", "You reached your daily goal");
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
            String title ="Daily goal reached 3 days in a row!";
            String message ="You reached your daily goal in the last three days, keep going!";
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntentNotification = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID)
                    .setSmallIcon(badgeIcon)
                    .setContentTitle(title)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentText(message)
                    .setContentIntent(pendingIntentNotification)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(message))
                    .setAutoCancel(true);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(5, builder.build());
            UnsteppableOpenHelper.insertBadges(getBaseContext(), timestamp, day, hour,"2", title, message);
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
            String title ="Daily goal reached all week, well done!";
            String message ="You reached your daily goal every day for this week!";
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntentNotification = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID)
                    .setSmallIcon(badgeIcon)
                    .setContentTitle(title)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentText(message)
                    .setContentIntent(pendingIntentNotification)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(message))
                    .setAutoCancel(true);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(6, builder.build());
            UnsteppableOpenHelper.insertBadges(getBaseContext(), timestamp, day, hour,"3", title, message);
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
        unregisterAlarmBroadcast2();
        getBaseContext().unregisterReceiver(broadcastResponseReceiver);
        serviceStopped = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void update(Observable o, Object arg) {
        WeatherStatus status = (WeatherStatus) arg;
        double lastP = p;
        switch(status) {
            case THUNDERSTORM:
                p = -0.5;// -50%
                break;
            case FOG:
                p = -0.2;// -20%
                break;
            case MIST:
                p = -0.2;// -20%
                break;
            case RAIN:
                p = -0.4;// -40%
                break;
            case SNOW:
                p = -0.3;// -30%
                break;
            case SQUALL:
                p = -0.5;// -50%
                break;
            case TORNADO:
                p = -0.7;// -70%
                break;
            case CLEAR:
                p = +0.3;// +30%
                break;
            case CLOUDS:
                p = +0.2;// +20%
                break;
            default:
                p = 0.0;
        }
        updateActualGoal(lastP != p);
        if(lastP != p){
            createNotificationWeather(p, status);
        }
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

    // updateWeather updates the weather every 3 hours
    private Runnable updateWeather = new Runnable() {

        @RequiresApi(api = Build.VERSION_CODES.N)
        public void run() {
            if (!serviceStopped) { // If service is still running keep it updated
                // Check weather
                WeatherService.getInstance().getCurrentWeather();
                // After the delay this Runnable will be executed again
                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(3*60));
            }
        }
    };


    private void createNotificationWeather(double p, WeatherStatus status) {
        String title;
        if(p >=0){
            title ="The weather is good, perfect for a walk!";
        }else{
            //EASTER EGG
            if(status == WeatherStatus.TORNADO) {
                title = "Stay home unless you want to go to Oz!";
            }
            else{
                title ="Weather is bad, but you can still do some steps!";
            }
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntentNotification = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        String message ="Now your daily goal is " + actualGoal +" steps.";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID)
                .setSmallIcon(appIcon)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentText(message)
                .setContentIntent(pendingIntentNotification)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(8, builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateActualGoal(boolean baseGoalChanged) {
        actualGoal = (int) (baseGoal + baseGoal*p);
        if(actualGoal <= 0){
            actualGoal = 1;
        }
        if(baseGoalChanged && androidSteps >= actualGoal){
            checkGoal(true);
        }
    }
    /* BROADCAST */
    // updateBroadcastData in order to choose the delay and not update every time
    private Runnable updateBroadcastData = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        public void run() {
            if (!serviceStopped) { // If service is still running keep it updated
                broadcastSensorValue();
                // After the delay this Runnable will be executed again
                handler.postDelayed(this, 1000);
            }
        }
    };
    /** Add data to the intent and send broadcast */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void broadcastSensorValue() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        int newBaseGoal = Integer.parseInt(preferences.getString(getApplicationContext().getResources().getString(R.string.base_goal), String.valueOf(baseGoal)));
        if(newBaseGoal != baseGoal){
            baseGoal = newBaseGoal;
            updateActualGoal(true);

        }

        // add data to intent
        intent.putExtra("Counted_Steps_Int", androidSteps);
        intent.putExtra("Counted_Steps", String.valueOf(androidSteps));
        intent.putExtra("Base_Goal_Int", baseGoal);
        intent.putExtra("Actual_Goal_Int", actualGoal);
        // call sendBroadcast with the intent: sends a message to whoever is registered
        sendBroadcast(intent);
    }
    private void registerBroadcastResponse(){
        broadcastResponseReceiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int notificationId = intent.getIntExtra("notificationId", -1);
                if(YES_INCREASE_ACTION.equals(action)) {
                    baseGoal =  (int) (baseGoal + baseGoal*0.1);
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(getString(R.string.base_goal), String.valueOf(baseGoal));
                    editor.apply();
                    updateActualGoal(true);

                    Log.v("RESPONSE_RECEIVER","Pressed Yes Increase");
                }
                else if(YES_DECREASE_ACTION.equals(action)) {
                    baseGoal =  (int) (baseGoal - baseGoal*0.1);
                    updateActualGoal(true);
                    Log.v("RESPONSE_RECEIVER","Pressed Yes Decrease");
                }else if(NO_ACTION.equals(action)) {
                    Log.v("RESPONSE_RECEIVER","Pressed No");
                }
                if(notificationId != -1){
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.cancel(notificationId);
                }
            }
        };
        // register the receiver
        IntentFilter intentFilter =  new IntentFilter();
        intentFilter.addAction(YES_DECREASE_ACTION);
        intentFilter.addAction(YES_INCREASE_ACTION);
        intentFilter.addAction(NO_ACTION);
        registerReceiver(broadcastResponseReceiver, intentFilter);
    }

    private void registerAlarmBroadcast2() {
        //This will be call when alarm time will reached.
        final Context t = this;
        broadcastAlarmReceiver2 = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onReceive(Context context, Intent intent) {
                if(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY){
                    Intent notificationIntent = new Intent(t, SettingsActivity.class);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntentNotification = PendingIntent.getActivity(t, 0, notificationIntent, 0);
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(t, CHANNEL_ID)
                            .setSmallIcon(appIcon)
                            .setContentTitle("Weekly report")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            // Set the intent that will fire when the user taps the notification
                            //.setContentIntent(pendingIntentNotification)
                            .setAutoCancel(true);
                    Calendar calendar = Calendar.getInstance();
                    String day;
                    int daysReached = 0;
                    for(int i = 0; i<7; i++){
                        calendar.add(Calendar.DAY_OF_YEAR, -1);
                        day = UnsteppableOpenHelper.getDay(calendar.getTimeInMillis());
                        if(UnsteppableOpenHelper.getReachedByDate(t,day)){
                            daysReached++;
                        }
                    }

                    int notificationId = 4;
                    //Yes intent
                    Intent yesReceive = new Intent();
                    yesReceive.putExtra("notificationId", notificationId);
                    String message;
                    int newGoal;
                    if(daysReached > 3){
                        yesReceive.setAction(YES_INCREASE_ACTION);
                        newGoal = (int) (baseGoal + baseGoal*0.1);
                        message = "You were unstoppable!"+
                                "Challenge yourself, change your goal to "+ newGoal;
                    }else{
                        yesReceive.setAction(YES_DECREASE_ACTION);
                        newGoal = (int) (baseGoal - baseGoal*0.1);
                        message = "This week was tough, next one will be better! " +
                                "There is no shame in decreasing your daily goal! Change it in "+ newGoal;
                    }


                    PendingIntent pendingIntentYes = PendingIntent.getBroadcast(t, 12345, yesReceive, PendingIntent.FLAG_UPDATE_CURRENT);

                    //No intent
                    Intent noReceive = new Intent();
                    noReceive.setAction(NO_ACTION);
                    noReceive.putExtra("notificationId", notificationId);
                    PendingIntent pendingIntentNo = PendingIntent.getBroadcast(t, 12345, noReceive, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentText(message)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(message))
                            .addAction(0,"Yes", pendingIntentYes)
                            .addAction(0,"No", pendingIntentNo);
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(t);
                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(notificationId, builder.build());
                }

            }
        };

        // register the receiver
        registerReceiver(broadcastAlarmReceiver2, new IntentFilter(BROADCAST_ACTION_ALARM2) );
        // create a pending intent that will be launched
        pendingIntent2 = PendingIntent.getBroadcast( this, 0, new Intent(BROADCAST_ACTION_ALARM2),0 );
        alarmManager = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
    }
    private void unregisterAlarmBroadcast2() {
        alarmManager.cancel(pendingIntent2);
        getBaseContext().unregisterReceiver(broadcastAlarmReceiver2);
    }

    private void registerAlarmBroadcast() {

        //This will be call when alarm time will reached.
        broadcastAlarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
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

}
