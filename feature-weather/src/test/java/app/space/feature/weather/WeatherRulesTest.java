package app.space.feature.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WeatherRulesTest {

    @Test public void everyDocumentedWmoCodeHasWords() {
        for (int code : new int[]{0, 1, 2, 3, 45, 48, 51, 55, 61, 65, 71, 75, 77, 80, 85, 95, 99}) {
            String text = WeatherRules.describe(code);
            assertFalse("code " + code, text.isEmpty());
            assertFalse("code " + code, text.equals("Not reported"));
        }
        // An unknown code must still say something rather than render as blank.
        assertEquals("Not reported", WeatherRules.describe(999));
    }

    @Test public void wetAndSnowClassificationDrivesTheAffordance() {
        assertTrue(WeatherRules.isWet(61));
        assertTrue(WeatherRules.isWet(95));
        assertFalse(WeatherRules.isWet(0));
        assertFalse(WeatherRules.isWet(3));
        assertTrue(WeatherRules.isSnow(71));
        assertTrue(WeatherRules.isSnow(86));
        assertFalse(WeatherRules.isSnow(61));
    }

    @Test public void unitConversionRoundTrips() {
        assertEquals(32d, WeatherRules.toFahrenheit(0d), 1e-9);
        assertEquals(212d, WeatherRules.toFahrenheit(100d), 1e-9);
        assertEquals(100d, WeatherRules.toCelsius(212d), 1e-9);
        assertEquals(-40d, WeatherRules.toCelsius(-40d), 1e-9);
    }

    @Test public void temperatureRoundsForDisplayRatherThanShowingDecimals() {
        assertEquals("21\u00b0C", WeatherRules.formatTemperature(21.4, true));
        assertEquals("22\u00b0C", WeatherRules.formatTemperature(21.6, true));
        assertEquals("70\u00b0F", WeatherRules.formatTemperature(70.2, false));
    }

    @Test public void aTypedCityIsPercentEncodedIntoTheQuery() {
        // A space or an ampersand left raw would truncate or corrupt the query, and the
        // failure looks like "city not found" rather than like a bug.
        assertTrue(WeatherRules.geocodeUrl("Bengaluru").contains("name=Bengaluru"));
        assertTrue(WeatherRules.geocodeUrl("New York").contains("name=New%20York"));
        assertTrue(WeatherRules.encode("a&b").contains("%26"));
        assertTrue(WeatherRules.encode("caf\u00e9").startsWith("caf%"));
        assertEquals("", WeatherRules.encode(""));
    }

    @Test public void theForecastUrlCarriesTheChosenUnitAndExplicitFields() {
        String url = WeatherRules.forecastUrl(12.9716, 77.5946, true);
        assertTrue(url, url.contains("temperature_unit=celsius"));
        assertTrue(url, url.contains("current="));
        assertTrue(url, url.contains("daily="));
        // Coordinates are trimmed: more digits is noise in a URL and can trip strict parsers.
        assertTrue(url, url.contains("latitude=12.9716"));
        assertFalse(WeatherRules.forecastUrl(0, 0, false).contains("celsius"));
    }

    @Test public void aPlaceOutsideValidCoordinatesIsRejected() {
        assertTrue(new WeatherRules.Place("X", 12.9, 77.5).isValid());
        assertFalse(new WeatherRules.Place("X", 91, 0).isValid());
        assertFalse(new WeatherRules.Place("X", 0, 181).isValid());
        assertFalse(new WeatherRules.Place("  ", 0, 0).isValid());
    }

    @Test public void hourAndDayLabelsSurviveAShortOrMissingString() {
        assertEquals("14:00", WeatherRules.hourLabel("2026-10-22T14:00"));
        assertEquals("", WeatherRules.hourLabel(null));
        assertEquals("", WeatherRules.hourLabel("short"));
        assertEquals("22", WeatherRules.dayLabel("2026-10-22"));
        assertEquals("", WeatherRules.dayLabel(null));
    }

    @Test public void theHourlyWindowClampsInsteadOfThrowing() {
        double[] values = {1, 2, 3};
        assertEquals(2, WeatherRules.window(values, 0, 2).size());
        assertEquals(1, WeatherRules.window(values, 2, 5).size());
        assertEquals(0, WeatherRules.window(values, 9, 3).size());
        assertEquals(0, WeatherRules.window(null, 0, 3).size());
        assertEquals(Integer.valueOf(2), WeatherRules.window(values, 0, 3).get(1));
    }

    @Test public void stalenessTreatsAFutureOrAbsentTimestampAsStale() {
        long now = 1_000_000L;
        assertFalse(WeatherRules.isStale(now - 1000, now, 60_000));
        assertTrue(WeatherRules.isStale(now - 120_000, now, 60_000));
        // Never fetched, and a clock that has moved backwards, both count as stale: showing
        // them as fresh would present invented data as current.
        assertTrue(WeatherRules.isStale(0, now, 60_000));
        assertTrue(WeatherRules.isStale(now + 10_000, now, 60_000));
    }
}
