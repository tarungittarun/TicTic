package app.space.feature.weather;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import app.space.core.data.AppPrefs;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * A typed city, its current conditions and a three-day outlook from Open-Meteo.
 *
 * <p>No API key and no location permission: the city is typed, which is what lets the
 * Settings page keep promising that no location is ever requested. Network use is opt-in per
 * fetch -- nothing is polled in the background, and a cached reading is shown until the user
 * asks again or it goes stale.
 *
 * <p>The fetch runs on a worker thread and posts back to the main looper. Doing it inline
 * would throw {@code NetworkOnMainThreadException}, and a bare thread touching views would
 * crash only sometimes, which is worse.
 */
public final class WeatherScreen implements Screen {

    static final String KEY_CITY = "weather_city";
    static final String KEY_CELSIUS = "weather_celsius";
    static final String KEY_PAYLOAD = "weather_payload";
    static final String KEY_FETCHED = "weather_fetched_at";
    /** Twenty minutes: recent enough to trust, short enough to not show yesterday's sky. */
    static final long MAX_AGE = 20 * 60 * 1000L;

    private final ScreenHost host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean busy;

    public WeatherScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Weather"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        final boolean celsius = AppPrefs.prefs(context).getBoolean(KEY_CELSIUS, true);
        final String city = AppPrefs.prefs(context).getString(KEY_CITY, "");
        final long fetchedAt = AppPrefs.prefs(context).getLong(KEY_FETCHED, 0L);
        final String payload = AppPrefs.prefs(context).getString(KEY_PAYLOAD, "");
        final boolean stale = WeatherRules.isStale(fetchedAt, System.currentTimeMillis(), MAX_AGE);

        ui.sectionTitle(root, "Weather",
                "Open-Meteo, no account and no location. Nothing is fetched until you ask.");

        final EditText input = new EditText(context);
        input.setHint("City");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(city);
        input.setTextColor(ui.palette().text);
        input.setHintTextColor(ui.palette().muted);
        input.setTextSize(14);
        input.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        input.setBackground(ui.shape(ui.palette().surface, 14));
        LinearLayout.LayoutParams ip = ui.lp(-1, -2);
        ip.topMargin = ui.dp(8);
        root.addView(input, ip);

        LinearLayout actions = ui.hbox();
        LinearLayout.LayoutParams acp = ui.lp(-1, 46);
        acp.topMargin = ui.dp(8);
        root.addView(actions, acp);
        TextView fetch = ui.pillButton(busy ? "Updating\u2026" : "Update", ui.palette().accent,
                android.graphics.Color.WHITE);
        LinearLayout.LayoutParams fp = ui.lp(0, 46);
        fp.weight = 1f;
        actions.addView(fetch, fp);
        fetch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (busy) return;
                String typed = input.getText().toString().trim();
                if (typed.isEmpty()) { host.toast("Type a city first."); return; }
                busy = true;
                host.toast("Fetching " + typed + "\u2026");
                host.invalidate();
                fetch(context, typed);
            }
        });
        TextView units = ui.pillButton(celsius ? "\u00b0C" : "\u00b0F", ui.palette().surface, ui.palette().text);
        LinearLayout.LayoutParams up = ui.lp(56, 46);
        up.leftMargin = ui.dp(6);
        actions.addView(units, up);
        units.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AppPrefs.prefs(context).edit().putBoolean(KEY_CELSIUS, !celsius).apply();
                host.invalidate();
            }
        });

        if (payload.isEmpty()) {
            TextView empty = ui.label("No reading yet.", 13, ui.palette().muted, false);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = ui.lp(-1, 60);
            ep.topMargin = ui.dp(10);
            root.addView(empty, ep);
            return scroll;
        }

        try {
            JSONObject json = new JSONObject(payload);
            JSONObject current = json.optJSONObject("current");
            if (current != null) {
                LinearLayout card = ui.card();
                LinearLayout.LayoutParams kp = ui.lp(-1, -2);
                kp.topMargin = ui.dp(10);
                root.addView(card, kp);
                String place = json.optString("place", city);
                card.addView(ui.label(place, 15, ui.palette().text, true));
                int code = current.optInt("weather_code", -1);
                TextView big = ui.label(
                        WeatherRules.formatTemperature(current.optDouble("temperature_2m"), celsius),
                        38, ui.palette().text, true);
                card.addView(big);
                card.addView(ui.label(WeatherRules.describe(code)
                        + (WeatherRules.isWet(code) ? "  \u00b7  bring an umbrella" : ""),
                        13, ui.palette().muted, false));
                String detail = "Feels " + WeatherRules.formatTemperature(
                                current.optDouble("apparent_temperature"), celsius)
                        + "  \u00b7  humidity " + WeatherRules.formatHumidity(current.optDouble("relative_humidity_2m"))
                        + "  \u00b7  wind " + WeatherRules.formatWind(current.optDouble("wind_speed_10m"));
                TextView detailView = ui.label(detail, 11, ui.palette().muted, false);
                detailView.setPadding(0, ui.dp(4), 0, 0);
                card.addView(detailView);
                card.addView(ui.label(stale
                        ? "Cached \u00b7 older than 20 minutes"
                        : "Fetched " + new java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(new java.util.Date(fetchedAt)),
                        10, stale ? 0xffc2564a : ui.palette().muted, stale));
            }

            JSONObject daily = json.optJSONObject("daily");
            if (daily != null) {
                JSONArray times = daily.optJSONArray("time");
                JSONArray codes = daily.optJSONArray("weather_code");
                JSONArray maxima = daily.optJSONArray("temperature_2m_max");
                JSONArray minima = daily.optJSONArray("temperature_2m_min");
                if (times != null) {
                    ui.sectionTitle(root, "Next days", "");
                    for (int i = 0; i < times.length(); i++) {
                        LinearLayout row = ui.hbox();
                        row.setGravity(Gravity.CENTER_VERTICAL);
                        LinearLayout card = ui.card();
                        LinearLayout.LayoutParams rp = ui.lp(-1, -2);
                        rp.topMargin = ui.dp(6);
                        root.addView(card, rp);
                        card.addView(row, ui.lp(-1, -2));
                        TextView day = ui.label(WeatherRules.dayLabel(times.optString(i)), 14,
                                ui.palette().text, true);
                        LinearLayout.LayoutParams dp = ui.lp(0, -2);
                        dp.weight = 1f;
                        row.addView(day, dp);
                        int code = codes == null ? -1 : codes.optInt(i, -1);
                        TextView what = ui.label(WeatherRules.describe(code), 12, ui.palette().muted, false);
                        LinearLayout.LayoutParams wp = ui.lp(0, -2);
                        wp.weight = 2f;
                        row.addView(what, wp);
                        String range = (minima == null ? "?" : WeatherRules.formatTemperature(minima.optDouble(i), celsius))
                                + " \u2013 "
                                + (maxima == null ? "?" : WeatherRules.formatTemperature(maxima.optDouble(i), celsius));
                        row.addView(ui.label(range, 12, ui.palette().text, true), ui.lp(-2, -2));
                    }
                }
            }
        } catch (Exception e) {
            // A malformed or partial response must not blank the screen: the last good
            // numbers stay up and the user is told the fetch failed.
            root.addView(ui.label("Could not read that response.", 12, ui.palette().muted, false));
        }
        return scroll;
    }

    /** Geocodes the city, then fetches the forecast, then stores the payload. */
    private void fetch(final Context context, final String city) {
        final Context appContext = context.getApplicationContext();
        final boolean celsius = AppPrefs.prefs(appContext).getBoolean(KEY_CELSIUS, true);
        new Thread(new Runnable() {
            @Override public void run() {
                String result = null;
                String failure = "Could not reach Open-Meteo.";
                try {
                    JSONObject geocoded = getJson(WeatherRules.geocodeUrl(city));
                    JSONArray results = geocoded == null ? null : geocoded.optJSONArray("results");
                    if (results == null || results.length() == 0) {
                        failure = "No place matches \u201c" + city + "\u201d.";
                    } else {
                        JSONObject place = results.getJSONObject(0);
                        double lat = place.optDouble("latitude");
                        double lon = place.optDouble("longitude");
                        WeatherRules.Place resolved = new WeatherRules.Place(
                                place.optString("name"), lat, lon);
                        if (!resolved.isValid()) {
                            failure = "That place has no usable coordinates.";
                        } else {
                            JSONObject forecast = getJson(
                                    WeatherRules.forecastUrl(lat, lon, celsius));
                            if (forecast != null) {
                                forecast.put("place", resolved.name);
                                result = forecast.toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    failure = "Network error: " + e.getClass().getSimpleName();
                }
                final String payload = result;
                final String message = result != null ? "Updated " + city + "." : failure;
                main.post(new Runnable() {
                    @Override public void run() {
                        busy = false;
                        if (payload != null) {
                            AppPrefs.prefs(appContext).edit()
                                    .putString(WeatherScreen.KEY_CITY, city)
                                    .putString(WeatherScreen.KEY_PAYLOAD, payload)
                                    .putLong(WeatherScreen.KEY_FETCHED, System.currentTimeMillis())
                                    .apply();
                        }
                        host.toast(message);
                        host.invalidate();
                    }
                });
            }
        }).start();
    }

    private static JSONObject getJson(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) return null;
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) out.append(line);
            reader.close();
            return new JSONObject(out.toString());
        } finally {
            connection.disconnect();
        }
    }

    @Override public void onResume() { }
    @Override public void onPause() { }
}
