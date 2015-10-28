package com.example.android.sunshine.app;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailListActivityFragment extends Fragment {

    private static final String LOG_TAG = DetailListActivityFragment.class.getName();
    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";

    private ArrayAdapter<String> mDetailedForecastAdapter;

    public DetailListActivityFragment() {
        setHasOptionsMenu(true);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        ShareActionProvider mShareActionProvider =
                (android.support.v7.widget.ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // Attach an intent to this ShareActionProvider.  You can update this at any time,
        // like when the user selects a new piece of data they might like to share.
        if (mShareActionProvider != null){
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        } else {
            Log.d(LOG_TAG, "Share Action Provider is null?");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mDetailedForecastAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_hourly_forecast,
                R.id.list_item_hourly_forecast_textview,
                new ArrayList<String>());

        View rootView = inflater.inflate(R.layout.fragment_hourly_forecasts, container, false);
        Intent intent = getActivity().getIntent();

        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
            // use pre-filtered values
            String [] detailStringArray = intent.getStringArrayExtra(Intent.EXTRA_TEXT);

            mDetailedForecastAdapter.clear();
            for (String s : detailStringArray){
                mDetailedForecastAdapter.add(s);
            }

            ListView listView = (ListView) rootView.findViewById((R.id.listview_hourly_forecasts));
            listView.setAdapter(mDetailedForecastAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String forecast = mDetailedForecastAdapter.getItem(i);
                    Intent detailIntent = new Intent(getActivity(), DetailActivity.class)
                            .putExtra(Intent.EXTRA_TEXT, forecast);
                    startActivity(detailIntent);
                }
            });

        }
        return rootView;
    }

    private Intent createShareForecastIntent(){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        // return to weatherApplication after sharing content in other application
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");

        String hourlyInfo = "";
        for (int i = 0; i<mDetailedForecastAdapter.getCount(); ++i){
            hourlyInfo += mDetailedForecastAdapter.getItem(i) + "; ";
        }

        hourlyInfo = hourlyInfo.substring(0, hourlyInfo.length() -2);

        shareIntent.putExtra(Intent.EXTRA_TEXT, hourlyInfo + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }
}
