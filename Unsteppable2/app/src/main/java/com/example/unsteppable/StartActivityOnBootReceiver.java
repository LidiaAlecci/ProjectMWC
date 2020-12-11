package com.example.unsteppable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.unsteppable.boot.StepDetectorService;

public class StartActivityOnBootReceiver extends BroadcastReceiver {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent i = new Intent(context, StepDetectorService.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startForegroundService(i);
        }
    }
}