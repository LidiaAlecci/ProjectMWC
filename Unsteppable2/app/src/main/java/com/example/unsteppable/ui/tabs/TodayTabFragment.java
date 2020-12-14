package com.example.unsteppable.ui.tabs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.unsteppable.R;
import com.example.unsteppable.boot.WeatherService;
import com.example.unsteppable.boot.WeatherStatus;
import com.example.unsteppable.boot.StepDetectorService;
import com.example.unsteppable.db.UnsteppableOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

import me.itangqi.waveloadingview.WaveLoadingView;

public class TodayTabFragment extends Fragment implements Observer {
    private WaveLoadingView mWaveLoad;

    private int countedStep = 0;
    private int baseGoal = 3000;
    private int actualGoal = 3000;
    private final Handler handler = new Handler(Looper.myLooper());
    private ImageView weatherImage;
    private TextView weatherText;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // intent is holding data to display
            countedStep = intent.getIntExtra("Counted_Steps_Int", countedStep);//intent.getStringExtra("Counted_Step");
            baseGoal = intent.getIntExtra("Base_Goal_Int", baseGoal);
            actualGoal = intent.getIntExtra("Actual_Goal_Int", actualGoal);
            mWaveLoad.setProgressValue(countedStep*100/actualGoal);
            mWaveLoad.setCenterTitle(String.valueOf(countedStep));
            mWaveLoad.setBottomTitle(String.valueOf(actualGoal));
        }
    };

    public static TodayTabFragment newInstance() {

        TodayTabFragment fragment = new TodayTabFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_today_tab, container, false);
        mWaveLoad = root.findViewById(R.id.waveLoadingView);
        mWaveLoad.setAnimDuration(5000);

        // BROADCAST
        this.getContext().registerReceiver(broadcastReceiver, new IntentFilter(StepDetectorService.BROADCAST_ACTION)); // BROADCAST

        // Get the number of steps stored in the current date
        Date cDate = new Date();
        String fDate = new SimpleDateFormat("yyyy-MM-dd").format(cDate);
        countedStep = UnsteppableOpenHelper.getStepsByDayFromTab1(this.getContext(), fDate);
        mWaveLoad.setProgressValue(countedStep*100/actualGoal);
        mWaveLoad.setCenterTitle(String.valueOf(countedStep));
        WeatherService.getInstance().register(this);
        WeatherStatus weather = WeatherService.getInstance().getCurrentWeather();
        Log.v("WEATHER", weather.getName());
        weatherImage = root.findViewById(R.id.weather_image);
        weatherImage.setImageResource(weather.getIcon());
        weatherText = (TextView) root.findViewById(R.id.weather_text);
        weatherText.setText(weather.getName());
        //handler.removeCallbacks(updateWeather);
        handler.post(updateWeather);

        return root;

    }

    private Runnable updateWeather = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
        public void run() {
            // Update weather
            WeatherStatus weather = WeatherService.getInstance().getCurrentWeather();
            weatherImage.setImageResource(weather.getIcon());
            weatherText.setText(weather.getName());
            handler.postDelayed(this, TimeUnit.MINUTES.toMillis(120));
        }
    };

    @Override
    public void update(Observable o, Object arg) {
        handler.post(updateWeather);
    }
}