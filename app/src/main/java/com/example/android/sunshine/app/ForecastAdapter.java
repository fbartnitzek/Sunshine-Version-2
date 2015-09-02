package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    private static final String LOG_TAG = ForecastAdapter.class.getSimpleName();
    public static final int VIEW_TYPE_COUNT = 2;
    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;
    private boolean mUseTodayLayout;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }


    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;

        if (viewType == VIEW_TYPE_TODAY){
            layoutId = R.layout.list_item_forecast_today;
        } else if (viewType == VIEW_TYPE_FUTURE_DAY){
            layoutId = R.layout.list_item_forecast;
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        //use ViewHolder
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        //icon ID
        int viewType = getItemViewType(cursor.getPosition());
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int fallbackIconId;

        // colorful for today, gray for other days
        if (viewType == VIEW_TYPE_TODAY){
            fallbackIconId = Utility.getArtResourceForWeatherCondition(weatherId);
//            viewHolder.iconView.setImageResource();
        } else {
            fallbackIconId = Utility.getIconResourceForWeatherCondition(weatherId);
//            viewHolder.iconView.setImageResource();
        }

        Glide.with(mContext)
                .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
                .error(fallbackIconId)
                .crossFade()
                .into(viewHolder.iconView);

        // date
        long dateInMillis = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis));

        //description
        String description = Utility.getStringForWeatherCondition(context, weatherId);
        viewHolder.descriptionView.setText(description);
        viewHolder.descriptionView.setContentDescription(context.getString(
                R.string.a11y_forecast, description));

        // For accessibility, we don't want a content description for the icon field
        // because the information is repeated in the description view and the icon
        // is not individually selectable

        //high temperature
        String high = Utility.formatTemperature(context,
                cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP));
        viewHolder.highTempView.setText(high);
        viewHolder.highTempView.setContentDescription(
                context.getString(R.string.a11y_high_temp, high));

        //low temperature
        String low = Utility.formatTemperature(context,
                cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));
        viewHolder.lowTempView.setText(low);
        viewHolder.lowTempView.setContentDescription(
                context.getString(R.string.a11y_low_temp, low));
    }

    public static class ViewHolder{
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ViewHolder(View view) {
            this.dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            this.iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            this.descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            this.highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            this.lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }

    public void setmUseTodayLayout(boolean mUseTodayLayout) {
        this.mUseTodayLayout = mUseTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
}