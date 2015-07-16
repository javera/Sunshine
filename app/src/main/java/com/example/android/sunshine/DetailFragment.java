package com.example.android.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment {
    private static String LOG_TAG = DetailFragment.class.getSimpleName();
    private static String FORECAST_SHARE_EXTRA_TEXT = " #SunshineApp";
    // used to store current forecast details - handy when share action is used
    private String mForecastDetail;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the view
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        // Get the message from the intent
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            mForecastDetail = intent.getStringExtra(Intent.EXTRA_TEXT);
            // Find the text view and change text
            TextView detailTextview = (TextView) rootView.findViewById(R.id.detail_textview);
            detailTextview.setText(mForecastDetail);
        }
        else
        {
            mForecastDetail = "";
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detailfragment, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.action_share);

        // Fetch and store ShareActionProvider
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(createShareIntent());
        } else {
            Log.d(LOG_TAG, "The share action provider is null");
        }
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastDetail + FORECAST_SHARE_EXTRA_TEXT);
        return shareIntent;
    }
}
