package com.example.unsteppable.boot;

public class AppState {
    private static int currentGoal;
    private static int defaultGoal;
    private static WeatherStatus weather;
    private static AppState instance = null;

    private AppState(){
        //defaultGoal = db.getLastDefaultGoal();
        defaultGoal = 6000;
        weather = WeatherService.getWeatherFromApi(0,0);//TODO
    }

    public AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return null;
    }
}
