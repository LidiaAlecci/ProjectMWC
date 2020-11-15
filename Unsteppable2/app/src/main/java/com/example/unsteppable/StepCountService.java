package com.example.unsteppable;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class StepCountService extends Service implements SensorEventListener {
    SensorManager sensorManager;
   // Sensor stepCounterSensor;
    Sensor sensorStepCounter;
    private static final String TAG = "STEP_SERVICE";
    private final Handler handler = new Handler();
    // Android step detector
    public int androidStepCounter = 0;
    public int oldSteps = 0;
    boolean serviceStopped; // Boolean variable to control if the service is stopped


    Intent intent;
    public static final String BROADCAST_ACTION = "com.example.unsteppable.mybroadcast";

    /** Called when the service is being created. */
    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
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
            handler.post(new ToastRunnable(R.string.step_not_available));
        }
        serviceStopped = false;
        SharedPreferences prefs= getApplicationContext().getSharedPreferences("com.example.unsteppable.ServiceRunning", getApplicationContext().MODE_PRIVATE);
        androidStepCounter = prefs.getInt("androidStepCounter", 0);
        oldSteps = prefs.getInt("oldSteps", 0);
        /*
        if(androidStepCounter == 0 && oldSteps == 0){
            if (sensorStepCounter != null) {
                sensorManager.registerListener(this, sensorStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
            }else {
                handler.post(new ToastRunnable(R.string.step_not_available));
            }
        }*/

        broadcastSensorValue();
        return Service.START_STICKY; // Tells Android to attempt to restart the service if it has stopped.
    }

    // Required method
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        return null;
    }
    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
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
        }
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
        try {
            SharedPreferences prefs= getSharedPreferences("com.example.unsteppable.ServiceRunning", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("androidStepCounter", androidStepCounter);
            editor.putInt("oldSteps", oldSteps);
            editor.apply();
            //Long.i("MoveMore", "Saving readings to preferences");
        } catch (NullPointerException e) {
            Log.e(TAG, "error saving: are you testing?" +e.getMessage());
        }
        Intent broadcastIntent = new Intent(this, SensorRestarterBroadcastReceiverWithValue.class);
        //broadcastIntent.putExtra("isForeground", )

        sendBroadcast(broadcastIntent);
        Log.v(TAG, "After sendBroadcast");
    }
    /** ToastRunnable in order to make toast message*/
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

    // updateBroadcastData
    private Runnable updateBroadcastData = new Runnable() {
        public void run() {
            if (!serviceStopped) { // If service is still running keep it updated
                broadcastSensorValue();
                // After the delay this Runnable will be executed again
                handler.postDelayed(this, 3000);
            }
        }
    };

    /** Add data to the intent and send broadcast */
    private void broadcastSensorValue() {
        Log.v(TAG, "Data to Activity");
        // add data to intent
        intent.putExtra("Counted_Step_Int", androidStepCounter);
        intent.putExtra("Counted_Step", String.valueOf(androidStepCounter));
        // call sendBroadcast with the intent: sends a message to whoever is registered
        sendBroadcast(intent);
    }
}
