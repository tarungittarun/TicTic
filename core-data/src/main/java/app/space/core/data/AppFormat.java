package app.space.core.data;

import android.content.Context;
import android.text.format.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Rendering of stored time values according to the user's system settings.
 *
 * <p>Fourth slice out of {@code app.tictic.clock.DataStore} (SUITE-PLAN.md §6.1 step
 * 1d-3). These live in the data layer rather than the design system because what they
 * encode is a <em>reading of user settings</em>, not a visual style: {@link #formatTime}
 * switches between 24-hour and 12-hour AM/PM purely from the system
 * {@code is24HourFormat} preference, which every MySpace app must honour identically.
 * A suite app that formatted times its own way would make the device look inconsistent.
 *
 * <p>Logic is unchanged from the original. {@code DataStore} keeps delegating methods,
 * so the 12 existing call sites are untouched.
 *
 * <p>Both use {@link Locale#getDefault()} rather than {@link Locale#ROOT} on purpose:
 * these strings are shown to the user, so month and day names must follow the device
 * locale. Contrast {@code TimeMath} in {@code :core-domain}, which uses
 * {@code Locale.ROOT} because its hundredths readout is numeric and must not vary.
 */
public final class AppFormat {

    private AppFormat() {}

    /** Formats an hour/minute pair using the device's 12- or 24-hour setting. */
    public static String formatTime(Context c, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        return new SimpleDateFormat(DateFormat.is24HourFormat(c) ? "HH:mm" : "h:mm a", Locale.getDefault())
                .format(cal.getTime());
    }

    /** Short weekday-and-date caption, e.g. {@code "Mon, 3 Mar"}, in the device locale. */
    public static String formatDate(long millis) {
        return new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(new Date(millis));
    }
}
