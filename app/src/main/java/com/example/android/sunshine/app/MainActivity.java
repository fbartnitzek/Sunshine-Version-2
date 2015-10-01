package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;


public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback{

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private boolean mTwoPane;
    private String mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation = Utility.getPreferredLocation(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (findViewById(R.id.weather_detail_container) != null){
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null){
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailActivityFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
            // removes unnecessary shadow below action bar for smaller screen devices
            // wrong for special today item
            getSupportActionBar().setElevation(0f);
        }

        ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().
                findFragmentById(R.id.fragment_forecast);
        ff.setUseTodayLayout(!mTwoPane);

        SunshineSyncAdapter.initializeSyncAdapter(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //call explicit settings-intent
            startActivity(new Intent(this, SettingsActivity.class));
            return true;

        } else if (id == R.id.action_debug_daily) {
            openUrlInBrowser(false);
            return true;

        } else if (id == R.id.action_debug_detailed) {
            openUrlInBrowser(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openUrlInBrowser(boolean detailed) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String location = prefs.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));


        Uri queryUri;
        if (detailed){
            queryUri = Uri.parse(JsonWeatherExtractor.DETAIL_QUERY_URL).buildUpon()
                    .appendQueryParameter(JsonWeatherExtractor.QUERY_PARAM, location)
                    .appendQueryParameter(JsonWeatherExtractor.FORMAT_PARAM, "xml")
                    .appendQueryParameter(JsonWeatherExtractor.UNITS_PARAM, "metric")
                    .build();
        } else {
            queryUri = Uri.parse(JsonWeatherExtractor.DAILY_QUERY_URL).buildUpon()
                    .appendQueryParameter(JsonWeatherExtractor.QUERY_PARAM, location)
                    .appendQueryParameter(JsonWeatherExtractor.FORMAT_PARAM, "xml")
                    .appendQueryParameter(JsonWeatherExtractor.UNITS_PARAM, "metric")
                    .appendQueryParameter(JsonWeatherExtractor.DAYS_PARAM, Integer.toString(JsonWeatherExtractor.NUM_DAYS))
                    .build();
        }



        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(queryUri);

        if (intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + location + ", no receiving apps installed!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation(this);
        // update the location in out second pane using the fragment manager
        if (location != null && !location.equals(mLocation)){
            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
//            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentByTag(FORECASTFRAGMENT_TAG);
            if (null != ff){
                ff.onLocationChanged();
            }
            DetailActivityFragment df = (DetailActivityFragment) getSupportFragmentManager().
                    findFragmentByTag(DETAILFRAGMENT_TAG);
            if (df != null){
                df.onLocationChanged(location);
            }


            mLocation = location;
        }
    }



    @Override
    public void onItemSelected(Uri contentUri, ForecastAdapter.ForecastAdapterViewHolder vh) {
        if (mTwoPane) {
            // tablet mode - show detail view in same activity by add/replacing detail fragment
            Bundle args = new Bundle();
            args.putParcelable(DetailActivityFragment.DETAIL_URI, contentUri);

            DetailActivityFragment fragment = new DetailActivityFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();

        } else {    // mobile mode - start intent
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);

            // start with animations
            ActivityOptionsCompat activityOptions =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                            new Pair<View, String>(vh.iconView, getString(R.string.detail_icon_transition_name)));
            ActivityCompat.startActivity(this, intent, activityOptions.toBundle());
        }

    }
}
