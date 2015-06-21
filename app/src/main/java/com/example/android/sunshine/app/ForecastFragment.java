package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;
//    public ArrayAdapter<String> mDetailedForecastAdapter;
    private List<String[]> detailedForecast = new ArrayList<>();

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){    //before onCreateView
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        FetchDetailedWeatherTask detailedWeatherTask = new FetchDetailedWeatherTask();
        String location = PreferenceManager.getDefaultSharedPreferences(
            getActivity()).getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        weatherTask.execute(location);
        detailedWeatherTask.execute(location);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mForecastAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                new ArrayList<String>());

        View rootView = inflater.inflate(R.layout.fragment_forecast, container, false);
        ListView listview = (ListView) rootView.findViewById(R.id.listview_forecast);
        listview.setAdapter(mForecastAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent detailIntent;
                if (i<detailedForecast.size()) {   // list of hourly entries

                    // list of string-array: list of days, array of hour-entries
                    String[] detailedDailyForecasts = detailedForecast.get(i);
                    detailIntent = new Intent(getActivity(), DetailListActivity.class)
                                .putExtra(Intent.EXTRA_TEXT, detailedDailyForecasts);

                } else {    // 1 entry

                    String forecast = mForecastAdapter.getItem(i);
                    detailIntent = new Intent(getActivity(), DetailActivity.class)
                            .putExtra(Intent.EXTRA_TEXT, forecast);
                }

                startActivity(detailIntent);
            }
        });


        return rootView;
    }

    public class FetchDetailedWeatherTask extends AsyncTask<String, Void, List<String[]>>{

        private final String LOG_TAG = FetchDetailedWeatherTask.class.getSimpleName();

        @Override
        protected void onPostExecute(List<String[]> result){

            if (result!=null){
                detailedForecast = result;
            }
        }


        @Override
        protected List<String[]> doInBackground(String... params) {

            if (params.length == 0){
                return null;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = prefs.getString(getString(R.string.pref_unit_key),
                    getString(R.string.pref_unit_metric));

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                Uri buildUri = Uri.parse(JsonWeatherExtractor.DETAIL_QUERY_URL).buildUpon()
                        .appendQueryParameter(JsonWeatherExtractor.QUERY_PARAM, params[0])
                        .appendQueryParameter(JsonWeatherExtractor.FORMAT_PARAM, "json")
                        .appendQueryParameter(JsonWeatherExtractor.UNITS_PARAM, "metric")
                        .build();
                URL url = new URL(buildUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
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
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in
                // attempting to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return JsonWeatherExtractor.getDetailedWeatherDataFromJson(forecastJsonStr, unitType);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON-parsing error occurred:" + e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

    }



    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>{

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected void onPostExecute(String[] result){
            if (result!=null){
                mForecastAdapter.clear();
                for (String s : result){
                    mForecastAdapter.add(s);
                }
            }
        }

        @Override
        protected String[] doInBackground(String... params) {

            if (params.length == 0){
                return null;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = prefs.getString(getString(R.string.pref_unit_key),
                    getString(R.string.pref_unit_metric));

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                Uri buildUri = Uri.parse(JsonWeatherExtractor.DAILY_QUERY_URL).buildUpon()
                        .appendQueryParameter(JsonWeatherExtractor.QUERY_PARAM, params[0])
                        .appendQueryParameter(JsonWeatherExtractor.FORMAT_PARAM, "json")
                        .appendQueryParameter(JsonWeatherExtractor.UNITS_PARAM, "metric")
                        .appendQueryParameter(JsonWeatherExtractor.DAYS_PARAM, Integer.toString(numDays))
                        .build();
                URL url = new URL(buildUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
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
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return JsonWeatherExtractor.getWeatherDataFromJson(forecastJsonStr, numDays, unitType);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON-parsing error occurred:" + e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

    }
}
