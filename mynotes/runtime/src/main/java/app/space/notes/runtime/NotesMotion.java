package app.space.notes.runtime;

import android.animation.ValueAnimator;
import android.view.*;

/** Short structural cues; respect system motion scale and haptic settings. Never drive model edits. */
final class NotesMotion {
 private NotesMotion(){}
 static boolean enabled(){return ValueAnimator.areAnimatorsEnabled();}
 static void enter(View view){if(!enabled())return;view.setAlpha(.75f);view.setTranslationY(8*view.getResources().getDisplayMetrics().density);view.animate().alpha(1).translationY(0).setDuration(160).start();}
 static void lift(View view){view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);if(!enabled())return;view.animate().scaleX(1.025f).scaleY(1.025f).alpha(.8f).setDuration(100).start();}
 static void settle(View view){view.animate().cancel();view.setScaleX(1);view.setScaleY(1);view.setAlpha(1);view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);}
}
