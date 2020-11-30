package com.example.unsteppable.boot;

import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;

import com.example.unsteppable.R;

public enum WeatherStatus{
        CLEAR("Sunny", R.drawable.ic_sunny),
        CLOUDS ("Cloudy",R.drawable.ic_cloudy),
        RAINY("Rainy", R.drawable.ic_rain),
        SNOWY( "Snowy", R.drawable.ic_snowy);
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
