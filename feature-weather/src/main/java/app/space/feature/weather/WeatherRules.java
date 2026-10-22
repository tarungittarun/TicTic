package app.space.feature.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unit conversion, weather-code wording and Open-Meteo response shaping.
 *
 * <p>Pure Java and takes plain arrays rather than JSON, so it runs as a JVM test. That
 * split is deliberate: {@code org.json} is stubbed "not mocked" in Android unit tests, so
 * parsing and logic in one class would mean neither could be tested here.
 */
public final class WeatherRules {

    private WeatherRules() {}

    /** A city the user typed. Latitude and longitude are resolved by Open-Meteo's geocoder. */
    public static final class Place {
        public final String name;
        public final double latitude;
        public final double longitude;

        public Place(String name, double latitude, double longitude) {
            this.name = name == null ? "" : name.trim();
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public boolean isValid() {
            return !name.isEmpty() && latitude >= -90 && latitude <= 90
                    && longitude >= -180 && longitude <= 180;
        }
    }

    /** Open-Meteo expects degrees, and a typed city must not produce an out-of-range call. */
    public static String geocodeUrl(String city) {
        String encoded = encode(city == null ? "" : city.trim());
        return "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded
                + "&count=1&language=en&format=json";
    }

    /**
     * The forecast call. Daily and hourly fields are requested explicitly, because
     * Open-Meteo returns only what is asked for and an omitted field arrives as an absent
     * array rather than an error.
     */
    public static String forecastUrl(double latitude, double longitude, boolean celsius) {
        return "https://api.open-meteo.com/v1/forecast?latitude=" + trim(latitude)
                + "&longitude=" + trim(longitude)
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m"
                + "&hourly=temperature_2m,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&forecast_days=3"
                + "&temperature_unit=" + (celsius ? "celsius" : "fahrenheit")
                + "&wind_speed_unit=kmh&timezone=auto";
    }

    /** WMO code to a short human phrase. Unknown codes must still say something. */
    public static String describe(int code) {
        switch (code) {
            case 0: return "Clear sky";
            case 1: return "Mainly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Fog";
            case 51: case 53: case 55: return "Drizzle";
            case 56: case 57: return "Freezing drizzle";
            case 61: case 63: case 65: return "Rain";
            case 66: case 67: return "Freezing rain";
            case 71: case 73: case 75: return "Snow";
            case 77: return "Snow grains";
            case 80: case 81: case 82: return "Rain showers";
            case 85: case 86: return "Snow showers";
            case 95: return "Thunderstorm";
            case 96: case 99: return "Thunderstorm with hail";
            default: return "Not reported";
        }
    }

    /** Whether a code means precipitation, which decides the umbrella affordance. */
    public static boolean isWet(int code) {
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 86) || code >= 95;
    }

    public static boolean isSnow(int code) {
        return (code >= 71 && code <= 77) || code == 85 || code == 86;
    }

    /** Celsius to Fahrenheit, for a display that stores one unit and shows either. */
    public static double toFahrenheit(double celsius) {
        return celsius * 9d / 5d + 32d;
    }

    public static double toCelsius(double fahrenheit) {
        return (fahrenheit - 32d) * 5d / 9d;
    }

    /** Rounds for display: a forecast is not precise to a tenth of a degree. */
    public static String formatTemperature(double value, boolean celsius) {
        return String.format(Locale.ROOT, "%.0f\u00b0%s", value, celsius ? "C" : "F");
    }

    public static String formatWind(double kmh) {
        return String.format(Locale.ROOT, "%.0f km/h", kmh);
    }

    public static String formatHumidity(double percent) {
        return String.format(Locale.ROOT, "%.0f%%", percent);
    }

    /** "2026-10-22T14:00" to the hour label shown in the strip. */
    public static String hourLabel(String isoTime) {
        if (isoTime == null || isoTime.length() < 13) return "";
        return isoTime.substring(11, 13) + ":00";
    }

    /** "2026-10-22" to "Wed 22". */
    public static String dayLabel(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return "";
        return isoDate.substring(8, 10);
    }

    /**
     * The next {@code count} hourly readings starting at {@code fromIndex}, clamped to the
     * arrays actually returned. A short response must not throw.
     */
    public static List<Integer> window(double[] values, int fromIndex, int count) {
        List<Integer> out = new ArrayList<>();
        if (values == null) return out;
        for (int i = Math.max(0, fromIndex); i < values.length && out.size() < count; i++) {
            out.add((int) Math.round(values[i]));
        }
        return out;
    }

    /** Whether a cached reading is still fresh enough to show without a network call. */
    public static boolean isStale(long fetchedAt, long now, long maxAgeMillis) {
        return now - fetchedAt > maxAgeMillis || fetchedAt <= 0 || fetchedAt > now;
    }

    /** Four significant digits is what Open-Meteo wants; more is noise in the URL. */
    private static String trim(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    /** Percent-encodes a typed city. A space or an ampersand would otherwise break the query. */
    static String encode(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                out.append(c);
            } else if (c == ' ') {
                out.append("%20");
            } else {
                byte[] bytes = String.valueOf(c).getBytes(java.nio.charset.Charset.forName("UTF-8"));
                for (byte b : bytes) out.append('%').append(String.format(Locale.ROOT, "%02X", b));
            }
        }
        return out.toString();
    }
}
