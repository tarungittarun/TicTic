package app.space.feature.messages;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * Conversations read from the system SMS provider, sent through the framework SmsManager.
 *
 * <p>{@code READ_SMS} and {@code SEND_SMS} are requested only when the user first opens the
 * app's list or first presses send, never at launch, so the Settings page's promise that
 * optional access is asked for only when its feature is used stays true.
 *
 * <p><b>Distribution:</b> Google Play restricts SMS and Call Log permissions to apps whose
 * core function is messaging, and requires a declaration form. This module therefore ships
 * on the direct-APK path; putting it in a Play build needs that form filed first. Recorded
 * in SUITE-PLAN.md 5.9 rather than discovered at review time.
 */
public final class MessagesScreen implements Screen {

    private static final int PERMISSION_REQUEST = 92;
    /** Cap on rows read: an unbounded query on a long-lived phone blocks the render. */
    private static final int ROW_LIMIT = 500;

    private final ScreenHost host;
    private String selectedKey = "";
    private String query = "";

    public MessagesScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Messages"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        boolean canRead = has(context, Manifest.permission.READ_SMS);
        if (!canRead) {
            ui.sectionTitle(root, "Messages", "Not connected");
            LinearLayout card = ui.card();
            LinearLayout.LayoutParams cp = ui.lp(-1, -2);
            cp.topMargin = ui.dp(8);
            root.addView(card, cp);
            card.addView(ui.label("Message access is not granted.", 13, ui.palette().text, true));
            TextView note = ui.label("Nothing is read until you allow it, and the permission is "
                    + "asked for here rather than at launch. Google Play restricts SMS "
                    + "permissions to apps whose core function is messaging, so this app ships "
                    + "on the direct-APK path.", 11, ui.palette().muted, false);
            note.setPadding(0, ui.dp(4), 0, ui.dp(8));
            card.addView(note);
            TextView grant = ui.pillButton("Allow message access", ui.palette().accent,
                    android.graphics.Color.WHITE);
            card.addView(grant, ui.lp(-1, 46));
            grant.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { requestPermissions(context); }
            });
            return scroll;
        }

        final List<MessagesRules.Message> all = read(context);
        List<MessagesRules.Thread> threads = MessagesRules.threads(all);
        int unread = MessagesRules.totalUnread(all);
        ui.sectionTitle(root, "Messages", threads.size()
                + (threads.size() == 1 ? " conversation" : " conversations")
                + (unread == 0 ? "" : "  \u00b7  " + unread + " unread"));

        final EditText search = field(context, ui, "Search messages", InputType.TYPE_CLASS_TEXT);
        search.setText(query);
        LinearLayout.LayoutParams sp = ui.lp(-1, -2);
        sp.topMargin = ui.dp(6);
        root.addView(search, sp);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                query = s.toString();
                selectedKey = "";
                host.invalidate();
            }
        });

        TextView compose = ui.pillButton("New message", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams cmp = ui.lp(-1, 46);
        cmp.topMargin = ui.dp(8);
        root.addView(compose, cmp);
        compose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { composer(context, "", ""); }
        });

        if (!query.trim().isEmpty()) {
            List<MessagesRules.Message> matches = MessagesRules.filter(all, query);
            ui.sectionTitle(root, "Matches", String.valueOf(matches.size()));
            for (MessagesRules.Message m : matches) {
                LinearLayout row = ui.card();
                LinearLayout.LayoutParams rp = ui.lp(-1, -2);
                rp.topMargin = ui.dp(6);
                root.addView(row, rp);
                row.addView(ui.label(MessagesRules.threadLabel(m.address)
                        + (m.incoming ? "  \u00b7  received" : "  \u00b7  sent"),
                        12, ui.palette().muted, false));
                row.addView(ui.label(m.body, 13, ui.palette().text, false));
            }
            return scroll;
        }

        for (final MessagesRules.Thread thread : threads) {
            final boolean open = thread.key.equals(selectedKey);
            LinearLayout card = ui.card();
            LinearLayout.LayoutParams rp = ui.lp(-1, -2);
            rp.topMargin = ui.dp(6);
            root.addView(card, rp);

            LinearLayout head = ui.hbox();
            head.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(head, ui.lp(-1, -2));
            LinearLayout copy = ui.vbox();
            LinearLayout.LayoutParams cpp = ui.lp(0, -2);
            cpp.weight = 1f;
            head.addView(copy, cpp);
            copy.addView(ui.label(thread.label(), 14, ui.palette().text, true));
            copy.addView(ui.label(MessagesRules.singleLine(thread.preview()), 11,
                    ui.palette().muted, false));
            if (thread.unread > 0) {
                TextView badge = ui.pillButton(String.valueOf(thread.unread), ui.palette().accent,
                        android.graphics.Color.WHITE);
                head.addView(badge, ui.lp(30, 26));
            }
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    selectedKey = open ? "" : thread.key;
                    host.invalidate();
                }
            });

            if (!open) continue;
            for (MessagesRules.Message m : thread.messages) {
                TextView bubble = ui.label((m.incoming ? "\u2193 " : "\u2191 ") + m.body
                        + "   " + new java.text.SimpleDateFormat("d MMM HH:mm",
                                java.util.Locale.getDefault()).format(new java.util.Date(m.timestamp)),
                        12, m.incoming ? ui.palette().text : ui.palette().muted, !m.read && m.incoming);
                bubble.setPadding(0, ui.dp(6), 0, 0);
                card.addView(bubble);
            }
            LinearLayout actions = ui.hbox();
            LinearLayout.LayoutParams ap = ui.lp(-1, 40);
            ap.topMargin = ui.dp(8);
            card.addView(actions, ap);
            TextView reply = ui.pillButton("Reply", ui.palette().accent, android.graphics.Color.WHITE);
            LinearLayout.LayoutParams rpp = ui.lp(0, 40);
            rpp.weight = 1f;
            actions.addView(reply, rpp);
            reply.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { composer(context, thread.address, ""); }
            });
        }
        return scroll;
    }

    private static boolean has(Context context, String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions(Context context) {
        if (context instanceof Activity) {
            ((Activity) context).requestPermissions(new String[]{
                    Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS}, PERMISSION_REQUEST);
        } else {
            host.toast("Message access must be granted in system settings.");
        }
    }

    /** Reads the system SMS store, newest first, capped so a long history cannot stall. */
    private List<MessagesRules.Message> read(Context context) {
        List<MessagesRules.Message> out = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI,
                    new String[]{Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
                            Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ},
                    null, null, Telephony.Sms.DATE + " DESC LIMIT " + ROW_LIMIT);
            if (cursor == null) return out;
            int addressColumn = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
            int bodyColumn = cursor.getColumnIndex(Telephony.Sms.BODY);
            int dateColumn = cursor.getColumnIndex(Telephony.Sms.DATE);
            int typeColumn = cursor.getColumnIndex(Telephony.Sms.TYPE);
            int readColumn = cursor.getColumnIndex(Telephony.Sms.READ);
            int idColumn = cursor.getColumnIndex(Telephony.Sms._ID);
            while (cursor.moveToNext()) {
                int type = typeColumn < 0 ? 1 : cursor.getInt(typeColumn);
                out.add(new MessagesRules.Message(
                        idColumn < 0 ? "" : cursor.getString(idColumn),
                        addressColumn < 0 ? "" : cursor.getString(addressColumn),
                        bodyColumn < 0 ? "" : cursor.getString(bodyColumn),
                        dateColumn < 0 ? 0 : cursor.getLong(dateColumn),
                        type == Telephony.Sms.MESSAGE_TYPE_INBOX,
                        readColumn >= 0 && cursor.getInt(readColumn) == 1));
            }
        } catch (Exception e) {
            // A provider that refuses the query, or a device with no telephony, must not
            // blank the screen.
            return out;
        } finally {
            if (cursor != null) cursor.close();
        }
        return out;
    }

    private void composer(final Context context, final String address, final String draft) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout form = ui.vbox();
        form.setPadding(ui.dp(14), ui.dp(6), ui.dp(14), ui.dp(6));
        final EditText to = field(context, ui, "Phone number", InputType.TYPE_CLASS_PHONE);
        to.setText(address);
        final EditText body = field(context, ui, "Message",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        body.setMinLines(3);
        body.setText(draft);
        form.addView(to, ui.lp(-1, -2));
        LinearLayout.LayoutParams bp = ui.lp(-1, -2);
        bp.topMargin = ui.dp(8);
        form.addView(body, bp);
        final TextView counter = ui.label("", 11, ui.palette().muted, false);
        form.addView(counter, ui.lp(-1, -2));
        body.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                int segments = MessagesRules.segmentCount(s.toString());
                // Shown before sending, because the segment count is what the carrier charges.
                counter.setText(segments == 0 ? "" : segments
                        + (segments == 1 ? " SMS segment" : " SMS segments"));
            }
        });

        app.space.core.design.AppAlertBuilder dialog =
                new app.space.core.design.AppAlertBuilder(context, ui);
        dialog.setTitle("New message");
        dialog.setView(form);
        dialog.setNegativeButton("Cancel", null);
        dialog.setPositiveButton("Send", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                String number = to.getText().toString().trim();
                String text = body.getText().toString();
                if (number.isEmpty()) { host.toast("Enter a phone number."); return; }
                if (!MessagesRules.isSendable(text)) {
                    host.toast("Write something first.");
                    return;
                }
                send(context, number, text);
            }
        });
        dialog.show();
    }

    private void send(Context context, String number, String text) {
        if (!has(context, Manifest.permission.SEND_SMS)) {
            requestPermissions(context);
            host.toast("Allow sending to send this message.");
            return;
        }
        try {
            SmsManager manager = SmsManager.getDefault();
            // divideMessage plus sendMultipartTextMessage: a body longer than one segment
            // sent with sendTextMessage is truncated by the platform, silently.
            ArrayList<String> parts = manager.divideMessage(text);
            if (parts.size() > 1) {
                manager.sendMultipartTextMessage(number, null, parts, null, null);
            } else {
                manager.sendTextMessage(number, null, text, null, null);
            }
            host.toast("Sent to " + number + ".");
            host.invalidate();
        } catch (Exception e) {
            host.toast("Could not send: " + e.getClass().getSimpleName());
        }
    }

    private static EditText field(Context context, UiKit ui, String hint, int inputType) {
        EditText e = new EditText(context);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        e.setTextColor(ui.palette().text);
        e.setHintTextColor(ui.palette().muted);
        e.setTextSize(14);
        e.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        e.setBackground(ui.shape(ui.palette().surface, 14));
        return e;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != %s) return;
        // Re-render either way: on grant the screen becomes usable, and on denial it has to
        // stop looking like it is waiting for something that will not arrive.
        host.invalidate();
        boolean granted = grantResults.length > 0;
        for (int result : grantResults) {
            if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) { granted = false; break; }
        }
        host.toast(granted ? "Access granted." : "Access refused. The feature stays unavailable.");
    }

    @Override public void onResume() { }
    @Override public void onPause() { }
}
