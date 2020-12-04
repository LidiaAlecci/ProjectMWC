package com.example.unsteppable;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.example.unsteppable.boot.BackgroundServiceHelper;
import com.example.unsteppable.boot.WeatherService;
import com.example.unsteppable.boot.WeatherStatus;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    private BackgroundServiceHelper backgroundService;
    private static final int REQUEST_ACTIVITY_RECOGNITION_PERMISSION = 45;
    private static final int REQUEST_FOREGROUND_SERVICE_PERMISSION = 10003;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 46;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 47;
    private static final int REQUEST_RECEIVE_BOOT_COMPLETED = 55;

    private AppBarConfiguration mAppBarConfiguration;
    public static final String CHANNEL_ID = "ServiceStepDetectorChannel";
    private boolean runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWriteExternalStorage();
        getReadExternalStorage();

        //Log.d("TRY PERMISSION", "Before if");
        // Ask for activity recognition permission
        if (runningQOrLater) {
            //Log.d("TRY PERMISSION", "trytry");
            getActivity();
        }
        backgroundService = BackgroundServiceHelper.getInstance();
        //Log.d("TRY PERMISSION", "After if");

        //Weather API part
//        getLocation();
        //String weather = getWeatherFromApi();
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
        WeatherStatus weather = getCurrentLocation();
        //((ImageView) findViewById(R.id.weather_image)).setImageResource(weather.getIcon());
        //((TextView) findViewById(R.id.weather_text)).setText(weather.getName());

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery)
                .setDrawerLayout(drawer)
                .build();


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        /** START THINGS FOR SERVICE **/
        backgroundService.startService(this, this.getBaseContext());
        backgroundService.createNotificationChannel(this);
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

    //LOCATION PART
    @SuppressLint("MissingPermission")
    private WeatherStatus getCurrentLocation() {
        final LocationRequest locationRequest = new LocationRequest();
        final double[] latitude = new double[1];
        final double[] longitude = new double[1];
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);



        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback() {

                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
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

    private void getLocation(){
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION
            );

        } else {
            getCurrentLocation();
        }
    }
    // Ask for write external storage permission
    private void getWriteExternalStorage() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    // Ask for read external storage permission
    private void getReadExternalStorage() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    // Ask for boot permission
    private void getBoot() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECEIVE_BOOT_COMPLETED)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.RECEIVE_BOOT_COMPLETED},
                    REQUEST_RECEIVE_BOOT_COMPLETED);
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

            case REQUEST_CODE_LOCATION_PERMISSION:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    getLocation();
                } else {
                    Toast.makeText(this,
                            R.string.permission_denied,
                            Toast.LENGTH_SHORT).show();
                }

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

            case REQUEST_RECEIVE_BOOT_COMPLETED:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getBoot();
                }  else {
                    Toast.makeText(this,
                            R.string.permission_denied,
                            Toast.LENGTH_SHORT).show();
                }

            case REQUEST_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getReadExternalStorage();
                }  else {
                    Toast.makeText(this,
                            R.string.permission_denied,
                            Toast.LENGTH_SHORT).show();
                }

        }
    }
}