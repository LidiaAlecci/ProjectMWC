package com.example.unsteppable.ui.tabs;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Column;
import com.anychart.enums.Anchor;
import com.anychart.enums.HoverMode;
import com.anychart.enums.Position;
import com.anychart.enums.TooltipPositionMode;
import com.example.unsteppable.R;
import com.example.unsteppable.db.UnsteppableOpenHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MonthTabFragment extends Fragment {
    AnyChartView anyChartView;
    Calendar cal = Calendar.getInstance();

    public static MonthTabFragment newInstance() {
        MonthTabFragment fragment = new MonthTabFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_month_tab, container, false);

        //Create the Chart
        anyChartView = root.findViewById(R.id.monthBarChart);
        anyChartView.setProgressBar(root.findViewById(R.id.loadingBar));

        Cartesian cartesian = createColumnChart();
        anyChartView.setBackgroundColor("#00000000");
        anyChartView.setChart(cartesian);
        return root;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Cartesian createColumnChart() {
        Map<String, Integer> graph_map = new TreeMap<>();
        String date, today;
        Integer value;
        boolean todayIsNotPassed = true;
        today = UnsteppableOpenHelper.getDay(cal.getTimeInMillis());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int maxForThisMonth = cal.getActualMaximum(Calendar.DATE);

        for(int i = 0; i < maxForThisMonth; i++){
            date = UnsteppableOpenHelper.getDay(cal.getTimeInMillis());
            Log.d("Current date: ", date);
            if(todayIsNotPassed){
                value = UnsteppableOpenHelper.getStepsFromDashboardByDate(getContext(), date);
            }
            else{
                value = 0;
            }
            if(date.equals(today)){
                todayIsNotPassed = false;
            }
            graph_map.put(date, value);
            cal.add(Calendar.DATE, 1);
        }
        Cartesian cartesian = AnyChart.column();

        List<DataEntry> data = new ArrayList<>();

        for (Map.Entry<String,Integer> entry : graph_map.entrySet()) {
            data.add(new ValueDataEntry(entry.getKey(), entry.getValue()));
        }

        Column column = cartesian.column(data);

        //change chart color based on theme
        TypedValue primaryValue = new TypedValue();
        String prefix="#";
        if (!this.requireContext().getTheme().resolveAttribute(R.attr.colorPrimary, primaryValue, true)) {
            this.requireContext().getTheme().resolveAttribute(R.attr.colorOnBackground, primaryValue, true);
        }
        column.fill(prefix+Integer.toHexString(primaryValue.data).substring(2));
        column.stroke(prefix+Integer.toHexString(primaryValue.data).substring(2));

        column.tooltip()
                .titleFormat("At day: {%X}")
                .format("{%Value}{groupsSeparator: } Steps")
                .anchor(Anchor.RIGHT_TOP);

        column.tooltip()
                .position(Position.RIGHT_TOP)
                .offsetX(0d)
                .offsetY(5);

        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);
        cartesian.interactivity().hoverMode(HoverMode.BY_X);
        cartesian.yScale().minimum(0);


        cartesian.yAxis(0).title("Number of steps");
        cartesian.xAxis(0).title("Day");
        cartesian.background().fill("#00000000");
        cartesian.animation(true);

        return cartesian;
    }
}