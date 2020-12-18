package com.example.unsteppable.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.example.unsteppable.R;
import com.example.unsteppable.ui.main.SectionsPagerAdapter;
import com.google.android.material.tabs.TabLayout;

public class HomeFragment extends Fragment {


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final ViewPager viewPager = root.findViewById(R.id.pager);
        final SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this.getContext(), this.getChildFragmentManager());



        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabs = root.findViewById(R.id.tabs);
        //in order to swipe between tabs
        tabs.setupWithViewPager(viewPager);


        return root;
    }
}