package com.example.unsteppable.ui.tabs;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.unsteppable.MainActivity;
import com.example.unsteppable.R;
import com.example.unsteppable.boot.WeatherService;
import com.example.unsteppable.boot.WeatherStatus;
import com.example.unsteppable.boot.StepDetectorService;
import com.example.unsteppable.db.UnsteppableOpenHelper;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

import me.itangqi.waveloadingview.WaveLoadingView;

public class TodayTabFragment extends Fragment implements Observer {
    private WaveLoadingView mWaveLoad;
    private View root;
    private int countedStep = 0;
    private int baseGoal = 3000;
    private int actualGoal = 3000;
    //private final Handler handler = new Handler(Looper.myLooper());
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

        this.root = inflater.inflate(R.layout.fragment_today_tab, container, false);
        WeatherService.getInstance().register(this);
        mWaveLoad = root.findViewById(R.id.waveLoadingView);
        mWaveLoad.setAnimDuration(5000);
        WeatherService.getInstance().register(this);
        weatherImage = root.findViewById(R.id.weather_image);
        weatherText = (TextView) root.findViewById(R.id.weather_text);


        // BROADCAST
        this.getContext().registerReceiver(broadcastReceiver, new IntentFilter(StepDetectorService.BROADCAST_ACTION)); // BROADCAST

        // Get the number of steps stored in the current date
        Date cDate = new Date();
        String fDate = new SimpleDateFormat("yyyy-MM-dd").format(cDate);
        countedStep = UnsteppableOpenHelper.getStepsByDayFromTab1(this.getContext(), fDate);
        mWaveLoad.setProgressValue(countedStep*100/actualGoal);
        mWaveLoad.setCenterTitle(String.valueOf(countedStep));

        final LocationRequest locationRequest = new LocationRequest();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this.getActivity());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                WeatherService.getInstance().getCurrentWeather();
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(getActivity(), ((MainActivity)getActivity()).REQUEST_CODE_LOCATION_PERMISSION);
                    } catch (IntentSender.SendIntentException sendEx) {
                       WeatherStatus status = WeatherStatus.valueOf("UNKNOWN");
                        weatherImage.setImageResource(status.getIcon());
                        weatherText.setText(status.getName());
                    }
                }}

        });



        return root;

    }

    @Override
    public void update(Observable o, Object arg) {
        WeatherStatus status = (WeatherStatus) arg;
        weatherImage.setImageResource(status.getIcon());
        weatherText.setText(status.getName());
    }
}