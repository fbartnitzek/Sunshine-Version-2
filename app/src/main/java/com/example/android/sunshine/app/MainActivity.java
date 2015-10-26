package com.example.android.sunshine.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
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

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;


public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback{

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    /** project number from the GCM API Console */
    static final String PROJECT_NUMBER = "324358525304";
    private GoogleCloudMessaging mGcm;

    private boolean mTwoPane;
    private String mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation = Utility.getPreferredLocation(this);
        Uri contentUri = getIntent() != null ? getIntent().getData() : null;

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
            if (savedInstanceState == null) {
                DetailActivityFragment fragment = new DetailActivityFragment();
                if (contentUri != null) {
                    Bundle args = new Bundle();
                    args.putParcelable(DetailActivityFragment.DETAIL_URI, contentUri);
                    fragment.setArguments(args);
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
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
        if (contentUri != null) {
            ff.setInitialSelectedDate(WeatherContract.WeatherEntry.getDateFromUri(contentUri));
        }

        SunshineSyncAdapter.initializeSyncAdapter(this);

        // If Google Play Services is not available, some features, such as GCM-powered weather
        // alerts, will not be available.
        if (!checkPlayServices()) {
            mGcm = GoogleCloudMessaging.getInstance(this);
            String regId = getRegistrationId(this);

            Log.v(LOG_TAG, "onCreate, " + "after checkPlayServices, regId= [" + regId + "]");
            if (PROJECT_NUMBER.equals("Your Project Number")) {
                new AlertDialog.Builder(this)
                        .setTitle("Needs Project Number")
                        .setMessage("GCM will not function in Sunshine until you set the Project Number to the one from the Google Developers Console.")
                        .setPositiveButton(android.R.string.ok, null)
                        .create().show();
            } else if (regId.isEmpty()) {
                registerInBackground(this);
            }
        } else {
            Log.i(LOG_TAG, "No valid Google Play Services APK. Weather alerts will be disabled.");
            // Store regId as null
            storeRegistrationId(this, null);
        }
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

        if (!checkPlayServices()) {
            // store regID as null
        }

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


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        Log.v(LOG_TAG, "checkPlayServices, " + "resultCode: " + resultCode);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.v(LOG_TAG, "checkPlayServices, " + "error happened...");
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(LOG_TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        Log.v(LOG_TAG, "getRegistrationId, " + "context = [" + context + "]");
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(LOG_TAG, "GCM Registration not found.");
            return "";
        }

        // Check if app was updated; if so, it must clear the registration ID since the
        // existing registration ID is not guaranteed to work with the new app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(LOG_TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     +     * @return Application's {@code SharedPreferences}.
     +     */
    private SharedPreferences getGCMPreferences(Context context) {
        Log.v(LOG_TAG, "getGCMPreferences, " + "context = [" + context + "]");
        // Sunshine persists the registration ID in shared preferences, but how you store
        //  the registration ID in your app is up to you. Just make sure that it is private!
        return getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    /**
    * @return Application's version code from the {@code PackageManager}.
    */
    private static int getAppVersion(Context context) {
        Log.v(LOG_TAG, "getAppVersion, " + "context = [" + context + "]");
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // Should never happen. WHAT DID YOU DO?!?!
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String msg = "";

                Log.v(LOG_TAG, "doInBackground, " + "params = [" + params + "]");
                try {
                    if (mGcm == null) {
                        mGcm = GoogleCloudMessaging.getInstance(context);
                    }
                    String regId = mGcm.register(PROJECT_NUMBER);
                    msg = "Device registered, registration ID = " + regId;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    //sendRegistrationIdToBackend();
                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the registration ID - no need to register again.
                    storeRegistrationId(context, regId);
                } catch (IOException e) {
                    msg = "Error :" + e.getMessage();
                }
                Log.i(LOG_TAG, "registerInBackground " + msg);
                return null;

            }
        }.execute(null, null, null);
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        Log.v(LOG_TAG, "storeRegistrationId, " + "context = [" + context + "], regId = [" + regId + "]");
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(LOG_TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }
}
