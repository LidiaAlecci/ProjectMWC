package com.example.unsteppable.boot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.unsteppable.MainActivity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

//Wheather API part
public final class WeatherService extends AsyncTask<String, Void, String> {
    private static WeatherService instance;
    private AppCompatActivity activity;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    private WeatherService(){

    }

    public static WeatherService getInstance(){
        if (instance == null)
                instance = new WeatherService();
        return instance;
    }
    public void setActivity(AppCompatActivity activity){
        this.activity = activity;
    }

    @Override
    protected String doInBackground(String... address) {
        //String... means multiple address can be send. It acts as array
        try {
            URL url = new URL(address[0]);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            //Establish connection with address
            connection.connect();

            //Retrive data from url
            InputStream is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);

            //Retrive data and return it as String
            int data = isr.read();
            String content = "";
            char ch;
            while (data != -1) {
                ch = (char) data;
                content += ch;
                data = isr.read();
            }
            return content;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static WeatherStatus getWeatherFromApi(double latitude, double longitude) {
        String content;
        //String city = "Lugano";

        String apiKey = "e9fcc5721ca04b00b71bebed9a78bae3";
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat="+latitude+"&lon="+longitude+"&appid="+apiKey;
        //String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey;

        WeatherService weather = new WeatherService();
        try {
            //content = weather.execute("https://api.openweathermap.org/data/2.5/weather?q=Lugano&appid=e9fcc5721ca04b00b71bebed9a78bae3").get();
            content = weather.execute(apiUrl).get();
            //First we will check data is retrieve successfully or not
            Log.i("contentData", content);

            //JSON
            JSONObject jsonObject = new JSONObject(content);
            String weatherData = jsonObject.getString("weather");
            Log.i("weatherData", weatherData);

            //weather data is in Array
            JSONArray array = new JSONArray(weatherData);

            String main = "";
            String description = "";

            for (int i = 0; i < array.length(); i++) {
                JSONObject weatherPart = array.getJSONObject(i);
                main = weatherPart.getString("main");
                description = weatherPart.getString("description");
            }

            Log.d("main", main);
            Log.d("description", description);
            return WeatherStatus.valueOf(main.toUpperCase());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    //LOCATION PART
    @SuppressLint("MissingPermission")
    public WeatherStatus getCurrentWeather() {
        final LocationRequest locationRequest = new LocationRequest();
        final double[] latitude = new double[1];
        final double[] longitude = new double[1];
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);



        LocationServices.getFusedLocationProviderClient(this.activity)
                .requestLocationUpdates(locationRequest, new LocationCallback() {

                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(activity)
                                .removeLocationUpdates(this);
                        if (locationResult != null && locationResult.getLocations().size() > 0) {
                            int latestLocationIndex = locationResult.getLocations().size() - 1;
                            latitude[0] =
                                    locationResult.getLocations().get(latestLocationIndex).getLatitude();
                            longitude[0] =
                                    locationResult.getLocations().get(latestLocationIndex).getLongitude();

                        }
                    }
                }, Looper.getMainLooper());

        return WeatherService.getWeatherFromApi(latitude[0], longitude[0]);
    }

    private void getLocation(){
        if (ContextCompat.checkSelfPermission(
                activity.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION
            );

        } else {
            getCurrentWeather();
        }
    }
}