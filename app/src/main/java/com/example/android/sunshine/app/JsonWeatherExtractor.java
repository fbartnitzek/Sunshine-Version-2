package com.example.android.sunshine.app;

import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by frank on 18.06.15.
 */
class JsonWeatherExtractor {
    private static final String LOG_TAG = JsonWeatherExtractor.class.getSimpleName();

    public static final String DAILY_QUERY_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
    public static final String DETAIL_QUERY_URL = "http://api.openweathermap.org/data/2.5/forecast";

    public static final String QUERY_PARAM = "q";
    public static final String UNITS_PARAM = "units";
    public static final String DAYS_PARAM = "cnt";
    public static final String FORMAT_PARAM = "mode";

    public static final int NUM_DAYS = 7;

    private static final String UNIT_IMPERIAL = "imperial";
    private static final String UNIT_METRIC = "metric";

    /* The date/time conversion code is going to be moved outside the asynctask later,
 * so for convenience we're breaking it out into its own method now.
 */
    private static String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.

        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private static String formatHighLows(double high, double low, String unitType) {
        // For presentation, assume the user doesn't care about tenths of a degree.

        // reduces network-traffic :-)
        if (unitType.equals(UNIT_IMPERIAL)){
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        } else if (!unitType.equals(UNIT_METRIC)){
            Log.d(LOG_TAG, "Unit type not found: " + unitType);
        }

        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        if (Math.abs(high - low) <= 0.0001){
            return String.valueOf(roundedHigh);
        } else {
            return roundedHigh + "/" + roundedLow;
        }

    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    public static String[] getWeatherDataFromJson(String forecastJsonStr, int numDays, String unitType)
            throws JSONException{

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
        for(int i = 0; i < weatherArray.length(); i++) {
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
            dateTime = dayTime.setJulianDay(julianStartDay+i);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low, unitType);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        return resultStrs;

    }


    private static String getReadableDetailString(String timeStringUtc, long gmtOffset) throws ParseException {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.

        // convert utcString to localeLong
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");//2015-06-21 21:00:00
        Date date = formatter.parse(timeStringUtc);
        long time = date.getTime() + gmtOffset;

        //TODO: modulo +3h to time
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE HH:mm");  //Tue 12:00 (+ 3h)
        return shortenedDateFormat.format(time) + " (+ 3h)";
    }


    public static List<String[]> getDetailedWeatherDataFromJson(String forecastJsonStr, String unitType) throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";

        final String OWM_MAX = "temp_max";
        final String OWM_MIN = "temp_min";
        final String OWM_MAIN = "main";
        final String OWM_DESCRIPTION = "description";
        // 1 time for creation of event: dt - nearly same, on for range dt_txt
        // both in utc
        final String OWM_START_TIME = "dt_txt";  //2015-06-21 21:00:00
        final String OWM_RAIN = "rain";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_PRECIPITATION= "3h";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
        List<String[]> results = new ArrayList<>();

        // OpenWeatherMap uses unix time and UTC/GMT time zone in any API calls
        // including current conditions, forecast and historical data.

        // time-problems:
        //1) openweathermap: unix, utc
        //2) android: locale (within any timezone)
        //3) shown time a) device locale   b) (would be nicer but maybe useless) location-time
        // => easier for now: device locale
        //howto: get gmtOffset, add to every time

        int gmtOffset = TimeZone.getDefault().getRawOffset();   //calc offset between

        int i = -1;
        String oldDate = null;
        List<String> hourlyForecasts = new ArrayList<>();

        do{
            ++i;
            JSONObject detailedForecast = weatherArray.getJSONObject(i);

            String precipitation = null; //OPT rain.3h
            String description; //rain
            String highAndLow; //main.temp_min, temp_max
            String humidity; //main.humidity

            // timeRange
            String startTimeUtc = detailedForecast.getString(OWM_START_TIME);  //2015-06-21 21:00:00
            if (startTimeUtc == null){
                Log.e (LOG_TAG, "no utc-timeString (key: " + OWM_START_TIME + ") found for " + i + ". entry!");
                continue;
            }

            // get timeEntry with gmtOffset (device time)
            String timeEntry;    //Tue 12:00 (+ 3h)
            try {
                timeEntry = getReadableDetailString(startTimeUtc, gmtOffset);
            } catch (ParseException e) {
                Log.e(LOG_TAG, "parsing of utc-timeString crashed (" + startTimeUtc + ")!");
                e.printStackTrace();
                continue;
            }

            // detailed description
            JSONObject weatherObject = detailedForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures and humidity
            JSONObject mainObject = detailedForecast.getJSONObject(OWM_MAIN);
            humidity = mainObject.getString(OWM_HUMIDITY);
            double high = mainObject.getDouble(OWM_MAX);
            double low = mainObject.getDouble(OWM_MIN);
            highAndLow = formatHighLows(high, low, unitType);

            // optional precipitation
            if (detailedForecast.has("rain")){
                JSONObject rainObject = detailedForecast.getJSONObject(OWM_RAIN);
                if (rainObject!=null){
                    double precip = new BigDecimal(
                            rainObject.getDouble(OWM_PRECIPITATION)).setScale(4, RoundingMode.HALF_UP).doubleValue();
                    precipitation = " - PRE: " + precip + "cmm";
                }
            }

            if (precipitation == null){
                precipitation = "";
            }

            // complete detail string
            String detailEntry = timeEntry + ": " + description + precipitation + " - HUM: " + humidity + "% - " + highAndLow;

            // use day of start_time to fill current array or add new list-entry
            String date = timeEntry.substring(0, timeEntry.indexOf(" "));

            if (!date.equals(oldDate) && oldDate != null){ //happens at different days and NOT for first entry
                String[] tmpArray = hourlyForecasts.toArray(new String[hourlyForecasts.size()]);
                results.add(tmpArray);
                hourlyForecasts.clear();
            } else {
                hourlyForecasts.add(detailEntry);
            }
            oldDate = date;

        } while (i < weatherArray.length() - 1);


        if (!hourlyForecasts.isEmpty()){ //add remaining hourlyEntries as lastDay
            String[] tmpArray = hourlyForecasts.toArray(new String[hourlyForecasts.size()]);
            results.add(tmpArray);
        }

        return results; //ListEntry per day with Array of detailedEntries per 3h
    }
}
