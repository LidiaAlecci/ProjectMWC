package com.example.unsteppable.ui.tabs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.unsteppable.R;
import com.example.unsteppable.boot.StepDetectorService;
import com.example.unsteppable.db.UnsteppableOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

import me.itangqi.waveloadingview.WaveLoadingView;

public class TodayTabFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;
    private WaveLoadingView mWaveLoad;

    /* BROADCAST STUFF */

    private int countedStep;
    private int baseGoal = 6000;
    private int actualGoal = 6000;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // intent is holding data to display
            countedStep = intent.getIntExtra("Counted_Steps_Int", 0);//intent.getStringExtra("Counted_Step");
            baseGoal = intent.getIntExtra("Base_Goal_Int", 6000);
            actualGoal = intent.getIntExtra("Actual_Goal_Int", 6000);
            //Log.d("BROADCAST in TodayTabFragment", String.valueOf(countedStep));
            mWaveLoad.setProgressValue(countedStep*100/actualGoal);
            mWaveLoad.setCenterTitle(String.valueOf(countedStep));
            mWaveLoad.setBottomTitle(String.valueOf(actualGoal));
            //Log.d("BROADCAST in TodayTabFragment: getProgressValue", String.valueOf(mWaveLoad.getProgressValue()));
        }
    };

    /* END BROADCAST STUFF */

    public static TodayTabFragment newInstance() {
        TodayTabFragment fragment = new TodayTabFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_today_tab, container, false);
        mWaveLoad = root.findViewById(R.id.waveLoadingView);
        mWaveLoad.setAnimDuration(5000);
        // BROADCAST
        this.getContext().registerReceiver(broadcastReceiver, new IntentFilter(StepDetectorService.BROADCAST_ACTION)); // BROADCAST

        // Get the number of steps stored in the current date
        Date cDate = new Date();
        String fDate = new SimpleDateFormat("yyyy-MM-dd").format(cDate);
        countedStep = UnsteppableOpenHelper.getStepsByDayFromTab1(this.getContext(), fDate);
        Log.d("STORED STEPS TODAY", "countedStep " + String.valueOf(countedStep));
        mWaveLoad.setProgressValue(countedStep*100/actualGoal);
        mWaveLoad.setCenterTitle(String.valueOf(countedStep));
        Log.d("STORED STEPS TODAY", "countedStep " + mWaveLoad.getCenterTitle());



        return root;

    }

}