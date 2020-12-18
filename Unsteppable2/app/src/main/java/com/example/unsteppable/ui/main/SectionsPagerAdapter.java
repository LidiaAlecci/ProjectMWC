package com.example.unsteppable.ui.main;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.unsteppable.R;
import com.example.unsteppable.ui.tabs.*;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 * Adapted from:
 * https://github.com/atilsamancioglu/A18-TabbedJavaFragment/blob/master/app/src/main/java/com/atilsamancioglu/tabbedjavafragment/ui/main/SectionsPagerAdapter.java
 */

public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.today, R.string.week, R.string.month};
    private final Context mContext;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position){
        // getItem is called to instantiate the fragment for the given page. Tabs start at 0
        switch(position){
            case 1:
                return WeekTabFragment.newInstance();
            case 2:
                return MonthTabFragment.newInstance();
            default:
                return TodayTabFragment.newInstance();
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show 3 total pages.
        return 3;
    }
}