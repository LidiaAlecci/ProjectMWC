package com.example.unsteppable;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;

import android.view.MenuItem;

import android.view.Menu;
import android.widget.Toast;



import com.example.unsteppable.boot.BackgroundServiceHelper;
import com.example.unsteppable.boot.WeatherService;
import com.example.unsteppable.boot.WeatherStatus;
import com.example.unsteppable.settings.SettingsActivity;
import com.google.android.material.navigation.NavigationView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


public class MainActivity extends AppCompatActivity{

    private BackgroundServiceHelper backgroundService;
    private static final int REQUEST_ACTIVITY_RECOGNITION_PERMISSION = 45;
    private static final int REQUEST_FOREGROUND_SERVICE_PERMISSION = 10003;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 46;
    private static final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 47;
    private static final int REQUEST_RECEIVE_BOOT_COMPLETED_PERMISSION = 55;

    private static final int MULTIPLE_PERMISSIONS = 10;

    String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private AppBarConfiguration mAppBarConfiguration;
    public static final String CHANNEL_ID = "ServiceStepDetectorChannel";
    private boolean runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());
        String theme = preferences.getString(getResources().getString(R.string.app_theme_option), "Light");
        setTheme(theme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        checkPermissions();

        backgroundService = BackgroundServiceHelper.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //NAVIGATION
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery)
                .setDrawerLayout(drawer)
                .build();


        NavController navController = Navigation.
                findNavController(this, R.id.nav_host_fragment);
        NavigationUI.
                setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        WeatherService weatherService = WeatherService.getInstance();
        weatherService.setActivity(this);

        backgroundService.startService(this, this.getBaseContext());
        backgroundService.createNotificationChannel(this);

    }

    private void setTheme(String theme) {
        switch(theme){
            case "Light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

                break;
            case "Dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
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




    /*** PERMISSION ***/
    // Like the tutorials made in class
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
                MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION
            );
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
                    REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    // Ask for boot permission
    private void getBoot() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECEIVE_BOOT_COMPLETED)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.RECEIVE_BOOT_COMPLETED},
                    REQUEST_RECEIVE_BOOT_COMPLETED_PERMISSION);
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

                break;

            case REQUEST_CODE_LOCATION_PERMISSION:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
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

            case REQUEST_RECEIVE_BOOT_COMPLETED_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getBoot();
                }  else {
                    Toast.makeText(this,
                            R.string.permission_denied,
                            Toast.LENGTH_SHORT).show();
                }

            case REQUEST_READ_EXTERNAL_STORAGE_PERMISSION:
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
    // Taken from https://stackoverflow.com/questions/34342816/android-6-0-multiple-permissions and adapted
    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(getApplicationContext(),p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.
                    requestPermissions(this, listPermissionsNeeded.
                            toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

}