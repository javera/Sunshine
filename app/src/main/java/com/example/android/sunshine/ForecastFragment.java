package com.example.android.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment used to display weather forecast in a list fashion.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    /**
     * Default constructor
     */
    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fragment has menu items
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // inflating the menu
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshWeather();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Runs a fetchWeatherTask on the bg thread, using the location value from app settings
     */
    private void refreshWeather() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        String locationPref = sharedPref.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        new FetchWeatherTask(getActivity(), mForecastAdapter).execute(locationPref);
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        List<String> forecastList = new ArrayList<>();

        // bind the sample data with the adapter
        mForecastAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                forecastList);

        // inflate the fragment's view and set the listview adapter
        View fragmentView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listview = (ListView) fragmentView.findViewById(R.id.list_view_forecast);
        listview.setAdapter(mForecastAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent detailViewIntent = new Intent(getActivity(), DetailActivity.class);
                detailViewIntent.putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(position));
                startActivity(detailViewIntent);

            }
        });
        return fragmentView;
    }

}
