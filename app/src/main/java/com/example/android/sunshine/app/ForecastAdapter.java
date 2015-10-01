package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.data.WeatherContract;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastAdapterViewHolder> {

    private static final String LOG_TAG = ForecastAdapter.class.getSimpleName();
    //    public static final int VIEW_TYPE_COUNT = 2;
    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;
    private boolean mUseTodayLayout;

    private Cursor mCursor;
    final private Context mContext;
    final private ForecastAdapterOnClickHandler mClickHandler;
    final private View mEmptyView;
    final private ItemChoiceManager mICM;


    public class ForecastAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ForecastAdapterViewHolder(View view) {
            super(view);
            this.dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            this.iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            this.descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            this.highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            this.lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            mCursor.moveToPosition(position);
            int dateColumnIndex = mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
            mClickHandler.onClick(mCursor.getLong(dateColumnIndex), this);
            mICM.onClick(this);

        }
    }

    public ForecastAdapter(Context context, ForecastAdapterOnClickHandler dh, View emptyView, int choiceMode) {
        mContext = context;
        mClickHandler = dh;
        mEmptyView = emptyView;
        mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(choiceMode);
    }

    public static interface ForecastAdapterOnClickHandler {
        void onClick(Long date, ForecastAdapterViewHolder vh);
    }


    /*
        This takes advantage of the fact that the viewGroup passed to onCreateViewHolder is the
        RecyclerView that will be used to contain the view, so that it can get the current
        ItemSelectionManager from the view.
        One could implement this pattern without modifying RecyclerView by taking advantage
        of the view tag to store the ItemChoiceManager.
     */
    @Override
    public ForecastAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewGroup instanceof RecyclerView) {
            int layoutId = -1;

            if (viewType == VIEW_TYPE_TODAY) {
                layoutId = R.layout.list_item_forecast_today;
            } else if (viewType == VIEW_TYPE_FUTURE_DAY) {
                layoutId = R.layout.list_item_forecast;
            }

            View view = LayoutInflater.from(viewGroup.getContext()).inflate(layoutId, viewGroup, false);
            view.setFocusable(true);
            return new ForecastAdapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }


    @Override
    public void onBindViewHolder(ForecastAdapterViewHolder forecastAdapterViewHolder, int position) {
        mCursor.moveToPosition(position);
        int weatherId = mCursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int defaultImage;

        // colorful for today, gray for other days
        if (getItemViewType(position) == VIEW_TYPE_TODAY) {
            defaultImage = Utility.getArtResourceForWeatherCondition(weatherId);
        } else {
            defaultImage = Utility.getIconResourceForWeatherCondition(weatherId);
        }

        if (Utility.usingLocalGraphics(mContext)) {
            forecastAdapterViewHolder.iconView.setImageResource(defaultImage);
        } else {
            Glide.with(mContext)
                    .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
                    .error(defaultImage)
                    .crossFade()
                    .into(forecastAdapterViewHolder.iconView);
        }


//        // date
        long dateInMillis = mCursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        forecastAdapterViewHolder.dateView.setText(Utility.getFriendlyDayString(mContext, dateInMillis));
//
//        //description
        String description = Utility.getStringForWeatherCondition(mContext, weatherId);
        forecastAdapterViewHolder.descriptionView.setText(description);
        forecastAdapterViewHolder.descriptionView.setContentDescription(mContext.getString(
                R.string.a11y_forecast, description));
//
//        // For accessibility, we don't want a content description for the icon field
//        // because the information is repeated in the description view and the icon
//        // is not individually selectable
//
//        //high temperature
        String high = Utility.formatTemperature(mContext,
                mCursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP));
        forecastAdapterViewHolder.highTempView.setText(high);
        forecastAdapterViewHolder.highTempView.setContentDescription(
                mContext.getString(R.string.a11y_high_temp, high));
//
//        //low temperature
        String low = Utility.formatTemperature(mContext,
                mCursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));
        forecastAdapterViewHolder.lowTempView.setText(low);
        forecastAdapterViewHolder.lowTempView.setContentDescription(
                mContext.getString(R.string.a11y_low_temp, low));

        mICM.onBindViewHolder(forecastAdapterViewHolder, position);
    }


    public void setmUseTodayLayout(boolean mUseTodayLayout) {
        this.mUseTodayLayout = mUseTodayLayout;
    }

    public void onRestoreInstanceState(Bundle savedInstanceState){
        mICM.onRestoreInstanceState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        mICM.onSaveInstanceState(outState);
    }

    public int getSelectedItemPosition() {
        return mICM.getSelectedItemPosition();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void selectView(RecyclerView.ViewHolder viewHolder) {
        if ( viewHolder instanceof ForecastAdapterViewHolder ) {
            ForecastAdapterViewHolder vfh = (ForecastAdapterViewHolder)viewHolder;
            vfh.onClick(vfh.itemView);
            }
        }

    @Override
    public int getItemCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }
}