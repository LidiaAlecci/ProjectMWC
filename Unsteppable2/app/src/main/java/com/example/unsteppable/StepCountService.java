package com.example.unsteppable;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
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
    Sensor sensorStepDetector;
    private static final String TAG = "STEP_SERVICE";
    private final Handler handler = new Handler();
    // Android step detector
    public int androidStepDetector = 0;
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
        sensorStepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (sensorStepDetector != null) {
            sensorManager.registerListener(this, sensorStepDetector, SensorManager.SENSOR_DELAY_NORMAL);
        }else {
            //handler.post(new ToastRunnable(R.string.step_not_available));
        }
        serviceStopped = false;
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
            case Sensor.TYPE_STEP_DETECTOR:
                // Calculate the number of steps
                androidStepDetector += (int) event.values[0];
                Log.v(TAG, "Num.steps: " + String.valueOf(androidStepDetector));
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
                handler.postDelayed(this, 1000);
            }
        }
    };

    /** Add data to the intent and send broadcast */
    private void broadcastSensorValue() {
        Log.v(TAG, "Data to Activity");
        // add data to intent
        intent.putExtra("Detected_Step_Int", androidStepDetector);
        intent.putExtra("Detected_Step", String.valueOf(androidStepDetector));
        // call sendBroadcast with the intent: sends a message to whoever is registered
        sendBroadcast(intent);
    }
}
