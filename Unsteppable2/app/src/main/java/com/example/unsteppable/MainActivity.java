package com.example.unsteppable;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    // PERMISSION
    private static final int REQUEST_ACTIVITY_RECOGNITION_PERMISSION = 45;
    private static final int REQUEST_FOREGROUND_SERVICE_PERMISSION = 10003;

    private boolean runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    /** START THINGS FOR SERVICE **/
    TextView stepCounterTxV; // TODO Debug
    String countedStep;
    private static final String TAG = "SENSOR_EVENT";
    public static final String CHANNEL_ID = "ServiceStepCounterChannel";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // intent is holding data to display
            countedStep = intent.getStringExtra("Counted_Steps");
            //Log.d(TAG, String.valueOf(countedStep));

            //stepCounterTxV.setText('"' + String.valueOf(countedStep) + '"' + " Steps Detected");
        }
    };

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager= getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public void startService(){
        registerReceiver(broadcastReceiver, new IntentFilter(StepCountService.BROADCAST_ACTION));
        //stepCountTxV = (TextView)findViewById(R.id.stepCountTxV);
        //stepCounterTxV = (TextView)findViewById(R.id.stepCountTxV);
        startForegroundService(new Intent(getBaseContext(), StepCountService.class));
    }

    /** END THINGS FOR SERVICE **/

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Log.d("TRY PERMISSION", "Before if");
        // Ask for activity recognition permission
        if (runningQOrLater) {
            //Log.d("TRY PERMISSION", "trytry");
            getActivity();
        }
        //Log.d("TRY PERMISSION", "After if");

        //Weather API part
        getWheatherFromApi();

        //TAB
        Toolbar toolbar = findViewById(R.id.toolbar);
        TabLayout tabLayout = findViewById(R.id.tabs);
        setSupportActionBar(toolbar);


        //FLOATING BUTTON
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //NAVIGATION
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();



        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        /** START THINGS FOR SERVICE **/
        startService();
        createNotificationChannel();
        /** END THINGS FOR SERVICE **/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Wheather API part
    class Weather extends AsyncTask<String, Void, String> {

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
                while(data != -1){
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
    }

    protected void getWheatherFromApi(){
        String content;
        String city = "Lugano";
        String apiKey = "e9fcc5721ca04b00b71bebed9a78bae3";
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q="+city+"&appid="+apiKey;

        Weather weather = new Weather();
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

            for(int i = 0; i < array.length(); i++){
                JSONObject weatherPart = array.getJSONObject(i);
                main = weatherPart.getString("main");
                description = weatherPart.getString("description");
            }

            Log.i("main", main);
            Log.i("descritpion", description);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*** PERMISSION ***/

    // Ask for permission
    private void getActivity() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACTIVITY_RECOGNITION},
                    REQUEST_ACTIVITY_RECOGNITION_PERMISSION);
        }
    }
    
    private void getForeground() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.FOREGROUND_SERVICE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.FOREGROUND_SERVICE},
                    REQUEST_FOREGROUND_SERVICE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACTIVITY_RECOGNITION_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getActivity();
                }  else {
                    Toast.makeText(this,
                            R.string.permission_denied,
                            Toast.LENGTH_SHORT).show();
                }
            
            case REQUEST_FOREGROUND_SERVICE_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getForeground();
                }  else {
                    Toast.makeText(this,
                            R.string.permission_denied,
                            Toast.LENGTH_SHORT).show();
                }
                /*
            case REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getWriteExternalStorage();
                }  else {
                    Toast.makeText(this,
                            R.string.permission_denied,
                            Toast.LENGTH_SHORT).show();
                }


             */
        }
    }
}