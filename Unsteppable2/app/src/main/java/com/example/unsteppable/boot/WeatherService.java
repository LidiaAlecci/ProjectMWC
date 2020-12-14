package com.example.unsteppable.boot;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.session.MediaSession;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.unsteppable.MainActivity;
import com.example.unsteppable.ui.tabs.TodayTabFragment;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.data.DataBufferObserver;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

//Wheather API part
public final class WeatherService extends AsyncTask<String, Void, String> {
    private static WeatherService instance;
    private MainActivity activity;
    List<Observer> observerList = new LinkedList();
    ObservableWeatherService observableService = new ObservableWeatherService();

    private WeatherService(){

    }

    public static WeatherService getInstance(){
        if (instance == null)
                instance = new WeatherService();
        return instance;
    }

    public void register(Observer o){
        observerList.add(o);
    }
    public void setActivity(MainActivity activity){
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

//        String apiKey = "e9fcc5721ca04b00b71bebed9a78bae3";
        String apiKey = "a26b3b70e9a9d91290b6a58219939c2b";
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
        return WeatherStatus.valueOf("UNKNOWN");
    }


    //LOCATION PART
    public void getCurrentWeather() {
        final LocationRequest locationRequest = new LocationRequest();

        locationRequest.setInterval(10000);
        locationRequest.setNumUpdates(1);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this.activity);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                LocationServices.getFusedLocationProviderClient(activity)
                        .requestLocationUpdates(locationRequest, new LocationCallback() {

                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                super.onLocationResult(locationResult);
                                LocationServices.getFusedLocationProviderClient(activity)
                                        .removeLocationUpdates(this);
                                if (locationResult != null && locationResult.getLocations().size() > 0) {
                                    double latitude;
                                    double longitude;
                                    int latestLocationIndex = locationResult.getLocations().size() - 1;
                                    latitude =
                                            locationResult.getLocations().get(latestLocationIndex).getLatitude();
                                    longitude =
                                            locationResult.getLocations().get(latestLocationIndex).getLongitude();
                                    observableService.notifyAll(WeatherService.getWeatherFromApi(latitude, longitude));

                                }
                            }
                        }, Looper.getMainLooper());
            }
        });
    }

    private class ObservableWeatherService extends Observable{
        public void notifyAll(WeatherStatus status){
            for (Observer o: observerList
                 ) {
                o.update(this, status);
                Log.d("update", status.getName());
            }
        }
    }

}