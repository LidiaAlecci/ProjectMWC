package com.example.unsteppable.boot;

import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;

public class AppState extends ViewModel {
    private static int currentGoal;
    private static int defaultGoal;
    private static WeatherStatus weather;
    private static AppState instance = null;

    private AppState(){
        //defaultGoal = db.getLastDefaultGoal();
        defaultGoal = 6000;
        //weather = WeatherService.getWeatherFromApi();
    }

    public AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return null;
    }
}
