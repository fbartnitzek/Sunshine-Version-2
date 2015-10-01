package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.data.WeatherContract;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
    static final String DETAIL_URI = "DETAIL_URI";
    static final String DETAIL_TRANSITION_ANIMATION = "DTA";

//    private ShareActionProvider mShareActionProvider;
    private String mForecast;
    private Uri mUri;
    private boolean mTransitionAnimation;

    private static final int DETAIL_LOADER_ID = 0;

    private static final String[] DETAIL_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_CONDITION_ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_PRESSURE = 6;
    static final int COL_WEATHER_WIND_SPEED = 7;
    static final int COL_WEATHER_DEGREES = 8;
    static final int COL_WEATHER_CONDITION_ID = 9;

    private ImageView mIconView;
//    private TextView mFriendlyDateView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityLabelView;
    private TextView mHumidityView;
    private TextView mWindLabelView;
//    private MyView mWindView;
    private TextView mWindView;
    private TextView mPressureLabelView;
    private TextView mPressureView;

    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_detail_start, container, false);
        // get uri from bundle
        Bundle args = getArguments();
        if (args != null){
            mUri = args.getParcelable(DETAIL_URI);
            mTransitionAnimation = args.getBoolean(DetailActivityFragment.DETAIL_TRANSITION_ANIMATION, false);
        }

        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
//        mFriendlyDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_textview);
        mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_textview);
        mHumidityLabelView = (TextView) rootView.findViewById(R.id.detail_humidity_label_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mWindLabelView = (TextView) rootView.findViewById(R.id.detail_wind_label_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        mPressureLabelView = (TextView) rootView.findViewById(R.id.detail_pressure_label_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);

        return rootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (getActivity() instanceof DetailActivity){
            // Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.detailfragment, menu);
            finishCreatingMenu(menu);
        }

//        // Get the provider and hold onto it to set/change the share intent.
//        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
//
//        if (mForecast != null){
//            mShareActionProvider.setShareIntent(createShareForecastIntent());
//        }

    }

    private void finishCreatingMenu(Menu menu) {
        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);
        menuItem.setIntent(createShareForecastIntent());
    }

    private Intent createShareForecastIntent(){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        // return to weatherApplication after sharing content in other application
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // use CursorLoader that will take care of creating a Cursor for the data being displayed
        if (mUri != null){
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null);
        }

        ViewParent vp = getView().getParent();
        if (vp instanceof CardView) {
            ((View) vp).setVisibility(View.INVISIBLE);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            ViewParent vp = getView().getParent();
            if ( vp instanceof CardView){
                ((View) vp).setVisibility(View.VISIBLE);
            }

            // condition id for pic
            int weatherConditionId = data.getInt(COL_WEATHER_CONDITION_ID);

            if (Utility.usingLocalGraphics(getActivity())) {
                //use weather art image
                mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherConditionId));
            } else {
                Glide.with(this)
                        .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherConditionId))
                        .error(Utility.getArtResourceForWeatherCondition(weatherConditionId))
                        .crossFade()
                        .into(mIconView);
            }

            //date and day
            long date = data.getLong(COL_WEATHER_DATE);
//            String friendlyDateText = Utility.getDayName(getActivity(), date);
//            mFriendlyDateView.setText(friendlyDateText);
//            String dateText = Utility.getFormattedMonthDay(getActivity(), date);
            String dateText = Utility.getFullFriendlyDayString(getActivity(), date);
            mDateView.setText(dateText);

            // description
            String description = Utility.getStringForWeatherCondition(
                    getActivity(), weatherConditionId);
            mDescriptionView.setText(description);
            mDescriptionView.setContentDescription(getString(R.string.a11y_forecast, description));

//            mIconView.setContentDescription(description);
            // For accessibility, add a content description to the icon field. Because the ImageView
            // is independently focusable, it's better to have a description of the image. Using
            // null is appropriate when the image is purely decorative or when the image already
            // has text describing it in the same UI component.
            mIconView.setContentDescription(getString(R.string.a11y_forecast_icon, description));

            // high & low temperature
//            boolean isMetric = Utility.isMetric(getActivity());

            String high = Utility.formatTemperature(getActivity(),
                    data.getDouble(COL_WEATHER_MAX_TEMP));
            mHighTempView.setText(high);
            mHighTempView.setContentDescription(getString(R.string.a11y_high_temp, high));

            String low = Utility.formatTemperature(getActivity(),
                    data.getDouble(COL_WEATHER_MIN_TEMP));
            mLowTempView.setText(low);
            mLowTempView.setContentDescription(getString(R.string.a11y_low_temp, low));

            // humidity
            float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
            mHumidityView.setText(getActivity().getString(R.string.format_humidity, humidity));
            mHumidityView.setContentDescription(getString(R.string.a11y_humidity, mHumidityView.getText()));
            mHumidityLabelView.setContentDescription(mHumidityView.getContentDescription());

            // wind
            float windSpeed = data.getFloat(COL_WEATHER_WIND_SPEED);
            float windDir = data.getFloat(COL_WEATHER_DEGREES);

//            mWindView.update(windDir, Utility.getFormattedWindSpeed(getActivity(), windSpeed));
            mWindView.setText(Utility.getFormattedWind(getActivity(), windSpeed, windDir));
            mWindView.setContentDescription(getString(R.string.a11y_wind, Utility.getFormattedWind(
                    getActivity(), windSpeed, windDir)));
            mWindLabelView.setContentDescription(mWindView.getContentDescription());

            // pressure
            float pressure = data.getFloat(COL_WEATHER_PRESSURE);
            mPressureView.setText(getActivity().getString(R.string.format_pressure, pressure));
            mPressureView.setContentDescription(getString(R.string.a11y_pressure, mPressureView.getText()));
            mPressureLabelView.setContentDescription(mPressureView.getContentDescription());
//            mPressureView.setContentDescription(mPressureView.getText());

            // share intent
            mForecast = String.format("%s - %s - %s/%s", dateText, description, high, low);

//            // update share intent if onCreateOptionsMenu has already happened
//            if (mShareActionProvider != null){
//                mShareActionProvider.setShareIntent(createShareForecastIntent());
//            }

            // custom toolbar...
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            Toolbar toolbarView = (Toolbar) getView().findViewById(R.id.toolbar);

            // We need to start the enter transition after the data has loaded
            if (mTransitionAnimation) {
                activity.supportStartPostponedEnterTransition();

                if (null != toolbarView) {
                    activity.setSupportActionBar(toolbarView);

                    activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                    activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            } else {
                if (null != toolbarView) {
                    Menu menu = toolbarView.getMenu();
                    if (null != menu) menu.clear();
                    toolbarView.inflateMenu(R.menu.detailfragment);
                    finishCreatingMenu(toolbarView.getMenu());
                }
            }
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }

    public void onLocationChanged(String newLocation) {
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (uri != null){
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
        }
    }
}
