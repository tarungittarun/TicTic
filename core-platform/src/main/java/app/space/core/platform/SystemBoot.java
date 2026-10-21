package app.space.core.platform;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

/**
 * Reads the device boot count, so a timer or stopwatch can tell whether the phone
 * restarted while it was running.
 *
 * <p>Moved out of {@code TimerReceiver} in step 1e (SUITE-PLAN.md §6.1). It was
 * package-private inside an {@code :app} broadcast receiver, which made it unreachable
 * from {@code :feature-clock} — and the stopwatch needs it, because {@code SystemClock
 * .elapsedRealtime()} resets to zero on reboot while {@code System.currentTimeMillis()}
 * does not. Comparing a saved elapsed time against the current one across a reboot
 * produces a wildly wrong duration; the boot count is what makes that detectable.
 *
 * <p>Lives here rather than in {@code :core-data} because it is a read of OS state, not
 * of the app's own storage. Permission preflight, time-change rescheduling and
 * power-saver state belong alongside it.
 */
public final class SystemBoot {

    private SystemBoot() {
    }

    /**
     * The current boot count, or {@code -1} when it cannot be read.
     *
     * <p>{@code -1} is a real value and not an error sentinel: callers store it and
     * compare it later, and a boot count that cannot be read must compare equal to itself
     * rather than look like a reboot happened. {@code Settings.Global.BOOT_COUNT} is also
     * not guaranteed present on every device, so the read is guarded and never throws —
     * a stopwatch that crashed on an unusual ROM would be worse than one that could not
     * detect a reboot.
     */
    public static int count(Context context) {
        // Dead at minSdk 26 and deliberately left verbatim: removing dead SDK_INT guards
        // is step 1j, batched so that each removal is not its own unattributable change.
        if (Build.VERSION.SDK_INT < 24) return -1;
        try {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        } catch (RuntimeException ignored) {
            return -1;
        }
    }
}
