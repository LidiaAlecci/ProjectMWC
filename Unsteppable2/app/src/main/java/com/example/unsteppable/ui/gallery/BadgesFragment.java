package com.example.unsteppable.ui.gallery;

import android.content.Context;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.anychart.charts.Cartesian;
import com.example.unsteppable.R;
import com.example.unsteppable.db.UnsteppableOpenHelper.Badge;
import com.example.unsteppable.db.UnsteppableOpenHelper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//the dynamic list was built by looking at this tutorials:
//https://www.freakyjolly.com/add-list-item-in-listview-android-example/#.X9aOcy9h3OQ
//https://guides.codepath.com/android/Using-an-ArrayAdapter-with-ListView
//
public class BadgesFragment extends Fragment {

    private ArrayList<Badge> badges;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        ListView listview = root.findViewById(R.id.badges_list);
        badges = UnsteppableOpenHelper.getAllBadges(getContext());
        final List<UnsteppableOpenHelper.Badge> badgesList = new ArrayList<>(badges);
        final CustomArrayAdapter adapter = new CustomArrayAdapter
                (this.getActivity(), android.R.layout.simple_list_item_2, badgesList);
        listview.setAdapter(adapter);
        listview.setEmptyView(root.findViewById(android.R.id.empty));
        return root;
    }

    private class CustomArrayAdapter extends ArrayAdapter<Badge>{
        private Context context;
        private List<UnsteppableOpenHelper.Badge> badges;
        public CustomArrayAdapter(@NonNull Context context, int resource, @NonNull List<Badge> objects) {
            super(context, resource, objects);
            this.context = context;
            this.badges = objects;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.view_badge, parent, false);
            };
            TextView title = (TextView) convertView.findViewById(R.id.badge_date);
            TextView subtitle = (TextView) convertView.findViewById(R.id.badge_description);
            title.setText(badges.get(position).getDay());
            subtitle.setText(badges.get(position).getDescription());
            return convertView;
        }
    }
}