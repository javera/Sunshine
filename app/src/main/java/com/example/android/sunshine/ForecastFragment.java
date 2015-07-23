package com.example.android.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A fragment used to display weather forecast in a list fashion.
 */
public class ForecastFragment extends Fragment {

    // a
    private ArrayAdapter<String> mListAdapter;

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

        new FetchWeatherTask().execute(locationPref);
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
        mListAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                forecastList);

        // inflate the fragment's view and set the listview adapter
        View fragmentView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listview = (ListView) fragmentView.findViewById(R.id.list_view_forecast);
        listview.setAdapter(mListAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent detailViewIntent = new Intent(getActivity(), DetailActivity.class);
                detailViewIntent.putExtra(Intent.EXTRA_TEXT, mListAdapter.getItem(position));
                startActivity(detailViewIntent);

            }
        });
        return fragmentView;
    }

    /**
     * Used to fetch the weather forecast from OpenWeatherMap
     */
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        // used for the logger
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /**
         * Background thread method to retrieve the forecast from OpenWeatherMap
         *
         * @param params expects single String that contains place's name/postal code
         * @return array of strings (7 elements, for a weekly forecast, or null if fetching is unsuccessful)
         */
        @Override
        protected String[] doInBackground(String... params) {

            // no name/postal code, return null
            if (params.length == 0) {
                return null;
            }

            String place = params[0];

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr;
            // Will contain parsed forecast (one day per array element)
            String[] weatherForecastsArray = null;

            // parameters used internally (expecting data in JSON format, using metric units and for 7 days)
            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                final String URL_BASE = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String PARAM_QUERY = "q";
                final String PARAM_MODE = "mode";
                final String PARAM_UNITS = "units";
                final String PARAM_DURATION = "cnt";

                Uri.Builder builder = Uri.parse(URL_BASE).buildUpon()
                        .appendQueryParameter(PARAM_QUERY, place)
                        .appendQueryParameter(PARAM_MODE, format)
                        .appendQueryParameter(PARAM_UNITS, units)
                        .appendQueryParameter(PARAM_DURATION, Integer.toString(numDays));

                URL url = new URL(builder.build().toString());

                Log.v(LOG_TAG, url.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                weatherForecastsArray = getWeatherDataFromJson(forecastJsonStr, numDays);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON couldn't be parsed: ", e);
                // If the code didn't successfully parsed the JSON data, there is nothing to return
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }
            return weatherForecastsArray;
        }

        @Override
        protected void onPostExecute(String[] forecasts) {
            if (forecasts != null && forecasts.length > 0) {
                mListAdapter.clear();
                for (String forecast : forecasts) {
                    mListAdapter.add(forecast);
                }
            }
        }

        /**
         * The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {

            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        private boolean isDisplayUnitImperial() {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitsPref = sharedPref.getString(getString(R.string.pref_units_key),
                    getString(R.string.pref_units_default));
            if (unitsPref.contentEquals("Imperial"))
            {
                return true;
            }
            return false;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                    throws JSONException {

                // These are the names of the JSON objects that need to be extracted.
                final String OWM_LIST = "list";
                final String OWM_WEATHER = "weather";
                final String OWM_TEMPERATURE = "temp";
                final String OWM_MAX = "max";
                final String OWM_MIN = "min";
                final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                // since we're getting temp in metric units, check if user wants temp in imperial units, then convert
                if (isDisplayUnitImperial())
                {
                    high = getImperialFromMetric(high);
                    low = getImperialFromMetric(low);
                }

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }
            return resultStrs;

        }

        private double getImperialFromMetric(double metricValue) {
            return  ((metricValue * 9) / 5) + 32;
        }
    }
}
