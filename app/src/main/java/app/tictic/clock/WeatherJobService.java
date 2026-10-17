package app.tictic.clock;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Network work is isolated to the explicitly enabled weather widget. */
public final class WeatherJobService extends JobService {
    private static final int PERIODIC_BASE = 120000;
    private static final int ONE_SHOT_BASE = 150000;

    @Override public boolean onStartJob(JobParameters params) {
        final int id = widgetId(params.getJobId());
        if (id < 0) return false;
        new Thread(() -> {
            try {
                updateWeather(this, id);
            } finally {
                jobFinished(params, false);
            }
        }, "TicTic-weather-" + id).start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) { return true; }

    private int widgetId(int jobId) {
        if (jobId >= PERIODIC_BASE && jobId < PERIODIC_BASE + 0x4000) return jobId - PERIODIC_BASE;
        if (jobId >= ONE_SHOT_BASE && jobId < ONE_SHOT_BASE + 0x4000) return jobId - ONE_SHOT_BASE;
        return -1;
    }

    private static void updateWeather(Context context, int id) {
        String key = "widget_" + id + "_";
        SharedPreferences prefs = DataStore.prefs(context);
        if (!prefs.getBoolean(key + "weather_enabled", false)) return;
        String city = prefs.getString(key + "weather_city", "Bengaluru").trim();
        if (TextUtils.isEmpty(city)) city = "Bengaluru";
        String summary;
        try {
            summary = fetch(city);
            prefs.edit().putString(key + "weather_summary", summary)
                    .putLong(key + "weather_updated", System.currentTimeMillis()).apply();
        } catch (Exception failure) {
            if (TextUtils.isEmpty(prefs.getString(key + "weather_summary", "")))
                prefs.edit().putString(key + "weather_summary", "Weather unavailable").apply();
        }
        WidgetEngine.refreshWidget(context, id);
    }

    private static String fetch(String city) throws Exception {
        String encoded = URLEncoder.encode(city, "UTF-8");
        JSONObject locations = new JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1&language=en&format=json"));
        JSONArray results = locations.optJSONArray("results");
        if (results == null || results.length() == 0) throw new IllegalArgumentException("City not found");
        JSONObject place = results.getJSONObject(0);
        double latitude = place.getDouble("latitude");
        double longitude = place.getDouble("longitude");
        String displayCity = place.optString("name", city);
        String country = place.optString("country_code", "");
        JSONObject response = new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&current=temperature_2m,weather_code&timezone=auto"));
        JSONObject current = response.getJSONObject("current");
        int temperature = (int) Math.round(current.getDouble("temperature_2m"));
        int code = current.getInt("weather_code");
        String condition = condition(code);
        return String.format(Locale.getDefault(), "%s%s · %d° · %s", displayCity,
                country.isEmpty() ? "" : ", " + country, temperature, condition);
    }

    private static String get(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);
        connection.setRequestProperty("User-Agent", "TicTic Android widget (Open-Meteo attribution in-app)");
        try {
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) throw new IllegalStateException("Weather service returned HTTP " + status);
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            if (status < 200 || status >= 300) throw new IllegalStateException("Weather service returned HTTP " + status);
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    private static String condition(int code) {
        if (code == 0) return "Clear";
        if (code == 1) return "Mostly clear";
        if (code == 2) return "Partly cloudy";
        if (code == 3) return "Cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Showers";
        if (code >= 85 && code <= 86) return "Snow showers";
        if (code >= 95) return "Thunderstorm";
        return "Current conditions";
    }
}
