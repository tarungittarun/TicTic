package app.tictic.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Refreshes placed widget time precision when Android toggles Battery Saver. */
public final class PowerSaverReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){if(intent!=null&&"android.os.action.POWER_SAVE_MODE_CHANGED".equals(intent.getAction()))WidgetEngine.refreshForPowerSaverChange(context);}
}
