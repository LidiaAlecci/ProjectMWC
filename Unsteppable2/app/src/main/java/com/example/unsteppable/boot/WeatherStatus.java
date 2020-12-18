package com.example.unsteppable.boot;

import com.example.unsteppable.R;

public enum WeatherStatus{
        //all possible values returned by the api
        CLEAR("Clear", R.drawable.ic_sunny),
        CLOUDS ("Cloudy",R.drawable.ic_cloudy),
        RAIN("Rainy", R.drawable.ic_rain),
        THUNDERSTORM("Rainy", R.drawable.ic_thunderstorm),
        DRIZZLE("Rainy", R.drawable.ic_rain),
        SNOW( "Snowy", R.drawable.ic_snowy),
        //All in the atmosphere category
        MIST("Misty", R.drawable.ic_foggy),
        FOG("Foggy", R.drawable.ic_foggy),
        SQUALL("Squall", R.drawable.ic_windy),
        SMOKE("Smoke", R.drawable.ic_windy),
        HAZE("Haze", R.drawable.ic_foggy),
        DUST("Dust", R.drawable.ic_windy),
        SAND("Sand", R.drawable.ic_foggy),
        ASH("Ash", R.drawable.ic_foggy),
        TORNADO("Tornado", R.drawable.ic_tornado),
        UNKNOWN("Error", R.drawable.ic_alert_circle);

        int icon;
        String name;

    WeatherStatus(String name, int icon) {
        this.name = name;
        this.icon = icon;
    }

    public int getIcon(){
            return icon;
        }
        public String getName(){
            return name;
        }

}
