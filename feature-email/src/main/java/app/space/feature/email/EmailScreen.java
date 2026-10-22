package app.space.feature.email;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.space.core.data.AppPrefs;
import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * Compose, drafts, and a local archive of what this app has sent.
 *
 * <p>Sending hands the message to the mail app already on the device through a
 * {@code mailto:} intent. That is why no credentials are stored and no OAuth is needed --
 * and it is also the honest limit of what ships here: <b>receiving is not implemented.</b>
 * Reading a mailbox needs IMAP, which in turn needs either an app password held by this app
 * or verified OAuth for the provider. Both are owner-side obligations recorded in
 * SUITE-PLAN.md 5.9, and neither can be resolved in code. What is on screen says so rather
 * than implying an inbox that is always empty.
 */
public final class EmailScreen implements Screen {

    static final String KEY = "email_items";
    static final int CAP = 400;

    private final ScreenHost host;
    private String folder = EmailRules.FOLDER_SENT;
    private String query = "";

    public EmailScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Email"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        List<EmailRules.Mail> all = load(context);
        ui.sectionTitle(root, "Email",
                EmailRules.unreadCount(all, EmailRules.FOLDER_DRAFTS) + " drafts  \u00b7  "
                        + EmailRules.inFolder(all, EmailRules.FOLDER_SENT).size() + " sent");

        // ---- what this app can and cannot do, stated plainly ----
        LinearLayout card = ui.card();
        LinearLayout.LayoutParams cp = ui.lp(-1, -2);
        cp.topMargin = ui.dp(8);
        root.addView(card, cp);
        card.addView(ui.label("Sending works through your own mail app.", 13, ui.palette().text, true));
        TextView note = ui.label("No password is stored here and no account is connected. "
                + "Receiving mail is not implemented: reading an inbox needs IMAP, which needs "
                + "either an app password or a verified OAuth sign-in. Both are listed as "
                + "owner-side blockers in the suite plan.", 11, ui.palette().muted, false);
        note.setPadding(0, ui.dp(4), 0, 0);
        card.addView(note);

        TextView compose = ui.pillButton("Compose", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams cmp = ui.lp(-1, 48);
        cmp.topMargin = ui.dp(10);
        root.addView(compose, cmp);
        compose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { composer(context, null); }
        });

        // ---- folder tabs ----
        LinearLayout tabs = ui.hbox();
        LinearLayout.LayoutParams tp = ui.lp(-1, 44);
        tp.topMargin = ui.dp(10);
        root.addView(tabs, tp);
        addTab(tabs, ui, "Drafts", EmailRules.FOLDER_DRAFTS, context);
        addTab(tabs, ui, "Sent", EmailRules.FOLDER_SENT, context);

        final EditText search = field(context, ui, "Search subject, body or sender");
        search.setText(query);
        LinearLayout.LayoutParams sp = ui.lp(-1, -2);
        sp.topMargin = ui.dp(8);
        root.addView(search, sp);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                query = s.toString();
                host.invalidate();
            }
        });

        List<EmailRules.Mail> shown = EmailRules.filter(EmailRules.inFolder(all, folder), query);
        if (shown.isEmpty()) {
            TextView empty = ui.label(folder.equals(EmailRules.FOLDER_DRAFTS)
                    ? "No drafts." : "Nothing sent from here yet.", 13, ui.palette().muted, false);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = ui.lp(-1, 60);
            ep.topMargin = ui.dp(10);
            root.addView(empty, ep);
        }

        for (final EmailRules.Mail mail : shown) {
            LinearLayout row = ui.card();
            LinearLayout.LayoutParams rp = ui.lp(-1, -2);
            rp.topMargin = ui.dp(8);
            root.addView(row, rp);
            row.addView(ui.label(mail.subject, 14, ui.palette().text, true));
            String meta = (mail.to.isEmpty() ? "no recipient" : EmailRules.nameOf(mail.to.get(0))
                    + (mail.to.size() > 1 ? " +" + (mail.to.size() - 1) : ""))
                    + "  \u00b7  " + new java.text.SimpleDateFormat("d MMM HH:mm",
                            java.util.Locale.getDefault()).format(new java.util.Date(mail.timestamp));
            TextView metaView = ui.label(meta, 11, ui.palette().muted, false);
            metaView.setPadding(0, ui.dp(2), 0, ui.dp(4));
            row.addView(metaView);
            if (!mail.body.isEmpty()) {
                row.addView(ui.label(EmailRules.preview(mail.body), 12, ui.palette().muted, false));
            }
            LinearLayout actions = ui.hbox();
            row.addView(actions, ui.lp(-1, 40));
            TextView open = ui.pillButton(mail.folder.equals(EmailRules.FOLDER_DRAFTS) ? "Edit" : "View",
                    ui.palette().surface, ui.palette().text);
            LinearLayout.LayoutParams op = ui.lp(0, 40);
            op.weight = 1f;
            actions.addView(open, op);
            open.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (mail.folder.equals(EmailRules.FOLDER_DRAFTS)) composer(context, mail);
                    else viewer(context, mail);
                }
            });
            TextView send = ui.pillButton("Send", ui.palette().surface, ui.palette().accent);
            LinearLayout.LayoutParams spp = ui.lp(0, 40);
            spp.weight = 1f;
            spp.leftMargin = ui.dp(6);
            actions.addView(send, spp);
            send.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { dispatch(context, mail); }
            });
            TextView delete = ui.pillButton("Delete", ui.palette().surface, ui.palette().muted);
            LinearLayout.LayoutParams dp = ui.lp(0, 40);
            dp.weight = 1f;
            dp.leftMargin = ui.dp(6);
            actions.addView(delete, dp);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    List<EmailRules.Mail> list = new ArrayList<>(load(context));
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).id.equals(mail.id)) { list.remove(i); break; }
                    }
                    save(context, list);
                    host.toast("Deleted.");
                    host.invalidate();
                }
            });
        }
        return scroll;
    }

    private void addTab(LinearLayout parent, UiKit ui, String label, final String target,
                        final Context context) {
        TextView tab = ui.pillButton(label, folder.equals(target) ? ui.palette().accent : ui.palette().surface,
                folder.equals(target) ? android.graphics.Color.WHITE : ui.palette().text);
        LinearLayout.LayoutParams p = ui.lp(0, 40);
        p.weight = 1f;
        p.rightMargin = ui.dp(4);
        parent.addView(tab, p);
        tab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { folder = target; host.invalidate(); }
        });
    }

    /** Hands the message to the device's mail app, then records it as sent. */
    private void dispatch(Context context, EmailRules.Mail mail) {
        String invalid = EmailRules.firstInvalid(mail.to);
        if (invalid != null) {
            host.toast("\u201c" + invalid + "\u201d is not a valid address.");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO,
                    Uri.parse(EmailRules.mailtoUri(mail.to, mail.subject, mail.body)));
            // Resolved first: without a mail app configured, startActivity throws and the
            // message the user just wrote would appear to vanish.
            if (intent.resolveActivity(context.getPackageManager()) == null) {
                host.toast("No mail app is set up on this device.");
                return;
            }
            context.startActivity(intent);
            if (mail.folder.equals(EmailRules.FOLDER_DRAFTS)) {
                List<EmailRules.Mail> list = new ArrayList<>(load(context));
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).id.equals(mail.id)) {
                        list.set(i, new EmailRules.Mail(mail.id, mail.from, mail.to, mail.subject,
                                mail.body, System.currentTimeMillis(), EmailRules.FOLDER_SENT, true));
                        break;
                    }
                }
                save(context, list);
            }
            host.toast("Opened in your mail app.");
            host.invalidate();
        } catch (Exception e) {
            host.toast("Could not open the mail app.");
        }
    }

    private void viewer(Context context, EmailRules.Mail mail) {
        UiKit ui = new UiKit(context, host.palette());
        LinearLayout body = ui.vbox();
        body.setPadding(ui.dp(14), ui.dp(8), ui.dp(14), ui.dp(8));
        body.addView(ui.label("To: " + join(mail.to), 12, ui.palette().muted, false));
        TextView text = ui.label(mail.body.isEmpty() ? "(empty)" : mail.body, 13, ui.palette().text, false);
        text.setPadding(0, ui.dp(8), 0, 0);
        body.addView(text);
        AppAlertBuilder dialog = new AppAlertBuilder(context, ui);
        dialog.setTitle(mail.subject);
        dialog.setView(body);
        dialog.setNegativeButton("Close", null);
        dialog.setPositiveButton("Reply", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                composer(context, new EmailRules.Mail(newId(), "",
                        mail.to.isEmpty() ? java.util.Collections.singletonList(mail.from) : mail.to,
                        EmailRules.replySubject(mail.subject),
                        EmailRules.quoteBody(mail.from, mail.timestamp, mail.body),
                        System.currentTimeMillis(), EmailRules.FOLDER_DRAFTS, true));
            }
        });
        dialog.show();
    }

    private static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) out.append(", ");
            out.append(values.get(i));
        }
        return out.length() == 0 ? "(none)" : out.toString();
    }

    private void composer(final Context context, final EmailRules.Mail existing) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout form = ui.vbox();
        form.setPadding(ui.dp(14), ui.dp(6), ui.dp(14), ui.dp(6));
        final EditText to = field(context, ui, "To");
        final EditText subject = field(context, ui, "Subject");
        final EditText body = field(context, ui, "Message",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        body.setMinLines(6);
        body.setGravity(Gravity.TOP);
        if (existing != null) {
            to.setText(join(existing.to).equals("(none)") ? "" : join(existing.to));
            subject.setText("(no subject)".equals(existing.subject) ? "" : existing.subject);
            body.setText(existing.body);
        }
        form.addView(to, ui.lp(-1, -2));
        for (EditText f : new EditText[]{subject, body}) {
            LinearLayout.LayoutParams p = ui.lp(-1, -2);
            p.topMargin = ui.dp(8);
            form.addView(f, p);
        }

        AppAlertBuilder dialog = new AppAlertBuilder(context, ui);
        dialog.setTitle(existing == null ? "New message" : "Edit draft");
        dialog.setView(form);
        dialog.setNegativeButton("Cancel", null);
        dialog.setNeutralButton("Save draft", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                store(context, existing, to, subject, body, EmailRules.FOLDER_DRAFTS, false);
            }
        });
        dialog.setPositiveButton("Send", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                store(context, existing, to, subject, body, EmailRules.FOLDER_SENT, true);
            }
        });
        dialog.show();
    }

    private void store(Context context, EmailRules.Mail existing, EditText to, EditText subject,
                       EditText body, String targetFolder, boolean dispatch) {
        List<String> recipients = EmailRules.splitRecipients(to.getText().toString());
        String invalid = EmailRules.firstInvalid(recipients);
        if (recipients.isEmpty() || invalid != null) {
            host.toast(invalid == null ? "Add a recipient."
                    : "\u201c" + invalid + "\u201d is not a valid address.");
            return;
        }
        String text = body.getText().toString();
        String title = subject.getText().toString().trim();
        EmailRules.Mail mail = new EmailRules.Mail(
                existing == null ? newId() : existing.id, "", recipients,
                title.isEmpty() ? "(no subject)" : title, text, System.currentTimeMillis(),
                targetFolder, true);
        List<EmailRules.Mail> list = new ArrayList<>(load(context));
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(mail.id)) { list.set(i, mail); replaced = true; break; }
        }
        if (!replaced) list.add(mail);
        save(context, list);
        if (dispatch) {
            // Recorded before handing off, so the archive reflects the attempt even if the
            // mail app is never completed in.
            dispatch(context, mail);
        } else {
            host.toast("Draft saved.");
            host.invalidate();
        }
    }

    private static EditText field(Context context, UiKit ui, String hint) {
        return field(context, ui, hint, InputType.TYPE_CLASS_TEXT);
    }

    private static EditText field(Context context, UiKit ui, String hint, int inputType) {
        EditText e = new EditText(context);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setSingleLine(inputType == InputType.TYPE_CLASS_TEXT);
        e.setTextColor(ui.palette().text);
        e.setHintTextColor(ui.palette().muted);
        e.setTextSize(14);
        e.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        e.setBackground(ui.shape(ui.palette().surface, 14));
        return e;
    }

    static List<EmailRules.Mail> load(Context c) {
        List<EmailRules.Mail> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(AppPrefs.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                List<String> to = new ArrayList<>();
                JSONArray recipients = o.optJSONArray("to");
                if (recipients != null) for (int j = 0; j < recipients.length(); j++) to.add(recipients.optString(j));
                out.add(new EmailRules.Mail(o.optString("id"), o.optString("from"), to,
                        o.optString("subject"), o.optString("body"), o.optLong("timestamp"),
                        o.optString("folder"), o.optBoolean("read", true)));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        List<EmailRules.Mail> sorted = EmailRules.newestFirst(out);
        return sorted.size() > CAP ? new ArrayList<>(sorted.subList(0, CAP)) : sorted;
    }

    static void save(Context c, List<EmailRules.Mail> mail) {
        JSONArray a = new JSONArray();
        try {
            List<EmailRules.Mail> kept = EmailRules.newestFirst(mail);
            if (kept.size() > CAP) kept = new ArrayList<>(kept.subList(0, CAP));
            for (EmailRules.Mail m : kept) {
                JSONArray to = new JSONArray();
                for (String r : m.to) to.put(r);
                a.put(new JSONObject().put("id", m.id).put("from", m.from).put("to", to)
                        .put("subject", m.subject).put("body", m.body)
                        .put("timestamp", m.timestamp).put("folder", m.folder).put("read", m.read));
            }
        } catch (Exception e) {
            return;
        }
        AppPrefs.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    static String newId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString((int) (Math.random() * 1679616), 36);
    }

    @Override public void onResume() { }
    @Override public void onPause() { }
}
