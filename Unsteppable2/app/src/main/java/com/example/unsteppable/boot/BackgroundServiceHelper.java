package com.example.unsteppable.boot;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class BackgroundServiceHelper {
    String countedStep;
    private static final String TAG = "SENSOR_EVENT";
    public static final String CHANNEL_ID = "ServiceStepDetectorChannel";

    private static BackgroundServiceHelper instance = null;

    private BackgroundServiceHelper(){

    }
    public static BackgroundServiceHelper getInstance(){
        if(instance==null){
            instance = new BackgroundServiceHelper();
        }
        return instance;
    }

    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // intent is holding data to display
            countedStep = intent.getStringExtra("Counted_Steps");
        }
    };

    public void createNotificationChannel(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager= context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startService(Context context, Context baseContext){
        context.registerReceiver(broadcastReceiver, new IntentFilter(StepDetectorService.BROADCAST_ACTION));
        context.startForegroundService(new Intent(baseContext, StepDetectorService.class));
    }

}
