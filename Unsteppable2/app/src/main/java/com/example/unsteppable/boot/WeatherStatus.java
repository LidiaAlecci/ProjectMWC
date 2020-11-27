package com.example.unsteppable.boot;

import android.graphics.drawable.Drawable;

import com.example.unsteppable.R;

public enum WeatherStatus{
        SUNNY {
            String name = "Clear";
            int id = R.drawable.ic_sun;
        },
        CLOUDY {
            String name = "Cloudy";
            int id = R.drawable.ic_cloudy;
        },
        RAINY {
            String name = "Rainy";
            int id = R.drawable.ic_rain;
        },
        SNOWY {
            String name = "Snow";
            int id = R.drawable.ic_snow;
        }

}
