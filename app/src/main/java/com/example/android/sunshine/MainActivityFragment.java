package com.example.android.sunshine;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ArrayAdapter<String> mAdapter;

        // dummy data for the ListView.
        String[] forecasts = {"Mon - Sunny - 32/28",
                "Tue - Partly Cloudy - 32/26",
                "Wed - Rainy - 24/16",
                "Thu - Thunderstorms - 20/14",
                "Fri - Rainy - 22/19",
                "Sat - Foggy - 26/24",
                "Sun - Partly Cloudy - 29/26"};

        List<String> forecastList = new ArrayList<String>(Arrays.asList(forecasts));

        mAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                forecastList);

        View fragmentView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listview = (ListView) fragmentView.findViewById(R.id.list_view_forecast);
        listview.setAdapter(mAdapter);

        return fragmentView;
    }
}
