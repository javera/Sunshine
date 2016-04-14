package com.example.android.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_WIND_SPEED = 6;
    static final int COL_WEATHER_WIND_DEGREES = 7;
    static final int COL_WEATHER_PRESSURE = 8;
    static final int COL_WEATHER_ICON_ID = 9;
    private static final String[] DETAIL_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };
    public static final String DETAIL_URI = "DETAIL_URI";
    private static String LOG_TAG = DetailFragment.class.getSimpleName();
    private static String FORECAST_SHARE_EXTRA_TEXT = " #SunshineApp";
    private static int DETAIL_LOADER_ID = 0;
    private ShareActionProvider mShareActionProvider;
    // used to store current forecast details - handy when share action is used
    private String mForecastDetail;
    private ImageView mIconView;
    private TextView mDayView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;
    private Uri mUri;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            mUri = args.getParcelable(DETAIL_URI);
        }

        // inflate the view
        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        mDayView = (TextView) view.findViewById(R.id.detailed_weather_day_textview);
        mDateView = (TextView) view.findViewById(R.id.detailed_weather_date_textview);
        mHighTempView = (TextView) view.findViewById(R.id.detailed_weather_high_textview);
        mLowTempView = (TextView) view.findViewById(R.id.detailed_weather_low_textview);
        mIconView = (ImageView) view.findViewById(R.id.detailed_weather_imageview);
        mDescriptionView = (TextView) view.findViewById(R.id.detailed_weather_forecast_textview);
        mHumidityView = (TextView) view.findViewById(R.id.detailed_weather_humidity_textview);
        mWindView = (TextView) view.findViewById(R.id.detailed_weather_wind_textview);
        mPressureView = (TextView) view.findViewById(R.id.detailed_weather_pressure_textview);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        // Prepare the loader.  Either re-connect with an existing one, or start a new one.
        getLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detailfragment, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.action_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mForecastDetail != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }

    }

    void onLocationChanged( String newLocation ) {
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (null != uri) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastDetail + FORECAST_SHARE_EXTRA_TEXT);
        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mUri != null) {
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    DETAIL_COLUMNS, // return columns specified in the constant array
                    null, // select all
                    null, // no select arguments
                    null // no sort order
            );
        }

        return null;

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        // date
        long dateInMillis = data.getLong(COL_WEATHER_DATE);
        String dayString = Utility.getDayName(getActivity(), dateInMillis);
        mDayView.setText(dayString);

        String dateString = Utility.getFormattedMonthDay(getActivity(), dateInMillis);
        mDateView.setText(dateString);

        // forecast image (art, as we need a coloured resource)
        int weatherArtResId = Utility.getArtResourceForWeatherCondition(data.getInt(COL_WEATHER_ICON_ID));
        mIconView.setImageResource(weatherArtResId);

        // forecast
        String weatherDescription = data.getString(COL_WEATHER_DESC);
        mDescriptionView.setText(weatherDescription);

        // temperature
        boolean isMetric = Utility.isMetric(getActivity());
        String high = Utility.formatTemperature(getActivity(), data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
        mHighTempView.setText(high);
        String low = Utility.formatTemperature(getActivity(), data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
        mLowTempView.setText(low);

        // humidity
        float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
        String humidityString = String.format(getString(R.string.format_humidity), humidity);
        mHumidityView.setText(humidityString);

        // wind
        float windSpeed = data.getFloat(COL_WEATHER_WIND_SPEED);
        float windDegrees = data.getFloat(COL_WEATHER_WIND_DEGREES);
        String windString = Utility.getFormattedWind(getActivity(), windSpeed, windDegrees);
        mWindView.setText(windString);

        // pressure
        float pressure = data.getFloat(COL_WEATHER_PRESSURE);
        String pressureString = String.format(getString(R.string.format_pressure), pressure);
        mPressureView.setText(pressureString);

        mForecastDetail = String.format("%s, %s - %s - %s/%s", dayString, dateString, weatherDescription, high, low);

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
